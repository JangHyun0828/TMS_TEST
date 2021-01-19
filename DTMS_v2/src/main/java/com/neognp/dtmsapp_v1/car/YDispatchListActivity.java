package com.neognp.dtmsapp_v1.car;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.common.GoogleApiAvailability;
import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.gps.GpsTrackingService;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.neognp.dtmsapp_v1.popup.ConfirmCancelDialog;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;


public class YDispatchListActivity extends BasicActivity {

	private boolean onReq;
	private Calendar fromCal, toCal, curCal;
	private Button btnDate, btnPrevDate, btnNextDate;
	private DrawerLayout dLayout;
	private GpsTrackingService mGpsTrackingService;
	private boolean onReqGpsTransmitCheck;
	private final ArrayList<Bundle> listItems = new ArrayList<Bundle>();
	private SwipeRefreshLayout swipeRefreshLayout;
	private RecyclerView list;
	private ListAdapter listAdapter;
	private boolean isListPullUp;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.car_dispatchlist_activity);

		//GPS
		IntentFilter filter = new IntentFilter();
		filter.addAction(Key.ACTION_GPS_SERVICE_START);
		filter.addAction(Key.ACTION_GPS_SERVICE_STOP);
		filter.addAction(Key.ACTION_GPS_SERVICE_STATE);
		filter.addAction(Key.ACTION_GPS_SERVICE_LOCATION_UPDATED);
		LocalBroadcastManager.getInstance(this).registerReceiver(gpsServiceReceiver, filter);

		//Calendar 설정
		toCal = Calendar.getInstance();
		toCal.set(Calendar.HOUR_OF_DAY, 0);
		toCal.set(Calendar.MINUTE, 0);
		toCal.set(Calendar.SECOND, 0);
		toCal.set(Calendar.MILLISECOND, 0);

		fromCal = Calendar.getInstance();
		fromCal.setTimeInMillis(toCal.getTimeInMillis());

		curCal = Calendar.getInstance();
		curCal.set(Calendar.HOUR_OF_DAY, 0);
		curCal.set(Calendar.MINUTE, 0);
		curCal.set(Calendar.SECOND, 0);
		curCal.set(Calendar.MILLISECOND, 0);

		//날짜 클릭시
		btnDate = (Button) findViewById(R.id.btnDate);
		btnDate.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
		btnDate.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				showCalendar();
			}
		});

		init();
	}

	private void init()
	{
		try
		{
			//GoogleHandler Play 서비스 호환 여부 체크
			GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);

			requestGpsTransmitCheck();

			getSwipeRefreshLayout();
			getList();

			selectDispatch();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	//Recycler View를 swipe시 Refresh
	public SwipeRefreshLayout getSwipeRefreshLayout() {
		swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
		swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.SwipeRefreshLayout_ColorScheme));
		swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
		{
			@Override
			public void onRefresh()
			{
				selectDispatch();
			}
		});
		return swipeRefreshLayout;
	}

	//list
	public RecyclerView getList() {
		list = findViewById(R.id.list);
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		list.setLayoutManager(layoutManager);
		listAdapter = new ListAdapter();
		list.setAdapter(listAdapter);
		list.addOnScrollListener(new RecyclerView.OnScrollListener()
		{
			public void onScrollStateChanged(RecyclerView recyclerView, int newState)
			{
				super.onScrollStateChanged(recyclerView, newState);

				// CAUTION 아이템 높이가 RecycleView 높이보다 큰 경우, 아래 메서드 사용시 index가 모두 -1 로 반환되므로 사용 금지
				int firstItemIdx = layoutManager.findFirstVisibleItemPosition();
				int lastItemIdx = layoutManager.findLastVisibleItemPosition();

				Log.e(TAG, "+ onScrollStateChanged(): newState=" + newState + " / isListPullUp=" + isListPullUp + " / firstItemIdx=" + firstItemIdx + " / lastItemIdx=" + lastItemIdx);

				if (newState == RecyclerView.SCROLL_STATE_IDLE)
				{
					if (!isListPullUp && firstItemIdx == 0)
					{
						Log.i(TAG, "+ onScrollStateChanged(): request refresh !");
					}
					else if (isListPullUp && lastItemIdx == listAdapter.getItemCount() - 1)
					{
						Log.i(TAG, "+ onScrollStateChanged(): request next list !");
					}
				}

				if (newState == RecyclerView.SCROLL_STATE_SETTLING)
				{
					int scrollY = recyclerView.computeVerticalScrollOffset();
					Log.e(TAG, "+ onScrollStateChanged(): scrollY=" + scrollY);
				}
			}

			public void onScrolled(RecyclerView recyclerView, int dx, int dy)
			{
				super.onScrolled(recyclerView, dx, dy);
			}
		});
		return list;
	}

	protected void onResume()
	{
		selectDispatch();
		super.onResume();
	}

	protected void onPause()
	{
		super.onPause();
	}

	protected void onRestart()
	{
		super.onRestart();
	}

	protected void onDestroy()
	{
		LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsServiceReceiver);
		super.onDestroy();
	}

	/* Service 를 시작시킨 Activity 종료 시, Service 도 함께 종료 */
	private final ServiceConnection mServiceConnection = new ServiceConnection()
	{
		final String TAG = ServiceConnection.class.getSimpleName();

		public void onServiceConnected(ComponentName componentName, IBinder service)
		{
			mGpsTrackingService = ((GpsTrackingService.LocalBinder) service).getService();
			Log.e(TAG, "+ onServiceConnected(): succeed");
		}

		public void onServiceDisconnected(ComponentName componentName)
		{
			Log.e(TAG, "+ onServiceDisconnected(): ");
			mGpsTrackingService = null;
		}
	};

	private final BroadcastReceiver gpsServiceReceiver = new BroadcastReceiver()
	{
		final String TAG = "gpsServiceReceiver";

		public void onReceive(Context context, Intent intent)
		{
			try
			{
				if (intent == null)
					return;

				String action = intent.getAction();

				if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_START))
				{
					Intent serviceIntent = new Intent(YDispatchListActivity.this, GpsTrackingService.class);
					startService(serviceIntent);
//					startGpsTrackingService();
				}

				else if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_STOP))
				{
					Intent serviceIntent = new Intent(YDispatchListActivity.this, GpsTrackingService.class);
					stopService(serviceIntent);
//					stopGpsTrackingService();
				}

				else if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_STATE))
				{

				}

				else if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_LOCATION_UPDATED))
				{

				}

			} catch (Exception e)

			{
				e.printStackTrace();
			}
		}
	};

	private void stopGpsTrackingService()
	{
		Intent serviceIntent = new Intent(this, GpsTrackingService.class);
		stopService(serviceIntent);
	}

	private synchronized void requestGpsServiceStop()
	{
		Intent intent = new Intent(Key.ACTION_GPS_SERVICE_STOP);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressLint ("StaticFieldLeak")
	private synchronized void requestGpsTransmitCheck()
	{
		if (onReqGpsTransmitCheck)
			return;

		if (Key.getUserInfo() == null)
			return;

		startGpsTrackingService();
	}

	private void startGpsTrackingService()
	{
		Intent serviceIntent = new Intent(this, GpsTrackingService.class);

		/** 홈 화면>모든 앱 종료시 Service 유지 **/
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
		{
			startService(serviceIntent);
		}
		// Oreo
		else {
			startForegroundService(serviceIntent);
		}
	}

    public void onClick(View v)
	{
    	InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId())
		{
			case R.id.prevDateBtn:
				setPrevDate();
				break;

			case R.id.nextDateBtn:
				setNextDate();
				break;

			case R.id.menu_close:
				dLayout.closeDrawer(Gravity.RIGHT);
				break;
        }
    }

	private void setPrevDate()
	{
		curCal.add(Calendar.DAY_OF_YEAR, -1);
		btnDate.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
		selectDispatch();
	}

	private void setNextDate()
	{
		curCal.add(Calendar.DAY_OF_YEAR, 1);
		btnDate.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
		selectDispatch();
	}

	private void showCalendar()
	{
		DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener ()
		{
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
			{
				Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

				curCal.set(Calendar.YEAR, year);
				curCal.set(Calendar.MONTH, monthOfYear);
				curCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				btnDate.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
				selectDispatch();
			}

		}, curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH), curCal.get(Calendar.DAY_OF_MONTH));
		datePickerDialog.show();
	}

	@SuppressLint("StaticFieldLeak")
	private synchronized void selectDispatch()
	{
		if (onReq)
			return;

		try
		{
			if (Key.getUserInfo() == null)
				return;

			new AsyncTask<Void, Void, Bundle>()
			{
				protected void onPreExecute()
				{
					onReq = true;
					showLoadingDialog(null, false);
				}

				protected Bundle doInBackground(Void... arg0)
				{
					JSONObject payloadJson = null;
					try
					{
						payloadJson = RestRequestor.buildPayload();
						payloadJson.put("carCd", Key.getUserInfo().getString("USER_CD"));
						payloadJson.put("dispatchDt", Key.SDF_PAYLOAD.format(curCal.getTime()));
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					return RestRequestor.requestPost(API.URL_CAR_DISPATCH, false, payloadJson, true, false);
				}

				protected void onPostExecute(Bundle response)
				{
					onReq = false;
					swipeRefreshLayout.setRefreshing(false);
					dismissLoadingDialog();

					try
					{
						Bundle resBody = response.getBundle(Key.resBody);
						String result_code = resBody.getString(Key.result_code);
						String result_msg = resBody.getString(Key.result_msg);

						if (result_code.equals("200"))
						{
							ArrayList<Bundle> data = resBody.getParcelableArrayList("data");
							addListItems(data);
						}
						else
						{
							showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						showToast(e.getMessage(), false);
					}
				}
			}.execute();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	synchronized void addListItems(ArrayList<Bundle> items)
	{
		if (items == null)
			return;

		try
		{
			listItems.clear();
			listItems.addAll(items);
			listAdapter.notifyDataSetChanged();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private class ListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
	{
		static final int TYPE_LIST_ITEM = 0;

		public int getItemViewType(int position)
		{
			return TYPE_LIST_ITEM;
		}

		public int getItemCount()
		{
			return listItems.size();
		}

		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
		{
			RecyclerView.ViewHolder holder = null;

			if (viewType == TYPE_LIST_ITEM)
			{
				View v = View.inflate(getContext(), R.layout.car_dispatch_item, null);
				v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
				holder = new ListItemView(v);
			}

			return holder;
		}

		public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position)
		{
			((ItemViewHolder) holder).onBindViewData(listItems.get(position));
		}

		abstract class ItemViewHolder extends RecyclerView.ViewHolder
		{
			public ItemViewHolder(View itemView)
			{
				super(itemView);
			}

			public abstract void onBindViewData(final Bundle item);
		}

		class ListItemView extends YDispatchListActivity.ListAdapter.ItemViewHolder
		{

			private final ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);

			public ListItemView(View itemView)
			{
				super(itemView);
			}

			public void onBindViewData(Bundle item)
			{
				try
				{
					String DELIVERY_DT = item.getString("DELIVERY_DT", "");
					String CUST_NM = item.getString("CUST_NM", "");
					String OUT_NM = item.getString("OUT_NM", "");
					String ITEM_SIZE = item.getString("ITEM_SIZE", "");
					String PB_MIX = item.getString("PB_MIX", "");
					String ORDER_QTY = item.getString("ORDER_QTY", "");
					String REMARK1 = item.getString("REMARK1", "");
					String REMARK2 = item.getString("REMARK2", "");
					String DELIVERY_DT_INFO = DELIVERY_DT.substring(0, 4) + "년 " + DELIVERY_DT.substring(4, 6) + "월 " +DELIVERY_DT.substring(6, 8) + "일";
					String ITEM_NM_INFO = item.getString("ITEM_NM_INFO", "");
					String DISPATCH_DT = item.getString("DISPATCH_DT", "");
					String DISPATCH_DT_ORG = item.getString("DISPATCH_DT_ORG", "");
					String DISPATCH_DT_INFO = DISPATCH_DT.substring(0, 4) + "년 " + DISPATCH_DT.substring(4, 6) + "월 " +DISPATCH_DT.substring(6, 8) + "일";

					TextView dataTxt0 = ((TextView) itemView.findViewById(R.id.dataTxt0));
					dataTxt0.setText(DELIVERY_DT_INFO);

					TextView dataTxt1 = ((TextView) itemView.findViewById(R.id.dataTxt1));
					dataTxt1.setText(CUST_NM);

					TextView dataTxt2 = ((TextView) itemView.findViewById(R.id.dataTxt2));
					dataTxt2.setText(OUT_NM);

					TextView dataTxt3 = ((TextView) itemView.findViewById(R.id.dataTxt3));
					dataTxt3.setText(ITEM_NM_INFO);

					TextView dataTxt4 = ((TextView) itemView.findViewById(R.id.dataTxt4));
					dataTxt4.setText(ITEM_SIZE);

					TextView dataTxt5 = ((TextView) itemView.findViewById(R.id.dataTxt5));
					dataTxt5.setText(PB_MIX);

					TextView dataTxt6 = ((TextView) itemView.findViewById(R.id.dataTxt6));
					dataTxt6.setText(ORDER_QTY);

					TextView dataTxt7 = ((TextView) itemView.findViewById(R.id.dataTxt7));
					dataTxt7.setText(REMARK1);

					TextView dataTxt8 = ((TextView) itemView.findViewById(R.id.dataTxt8));
					dataTxt8.setText(REMARK2);

					Button btnCall = (Button) itemView.findViewById(R.id.btn_call);
					btnCall.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							ConfirmCancelDialog.show(YDispatchListActivity.this, "[" + OUT_NM + "]" + "에 전화 연결하시겠습니까?", "취소", "확인", true, new ConfirmCancelDialog.DialogListener()
							{
								public void onCancel()
								{

								}

								public void onConfirm()
								{
									AppUtil.runCallApp(item.getString("OUT_TEL_NO"), true);
								}
							});
						}
					});

					Button btnLocation = (Button) itemView.findViewById(R.id.btn_location);
					btnLocation.setOnClickListener(new View.OnClickListener()
					{
						public void onClick(View v)
						{
							Intent intent = new Intent(YDispatchListActivity.this, LocationActivity.class);
							intent.putExtras((Bundle) item.clone());
							startActivity(intent);
						}
					});

					Button btnPallet = (Button) itemView.findViewById(R.id.btn_pallet);
					btnPallet.setOnClickListener(new View.OnClickListener()
					{
						public void onClick(View v)
						{
							ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);

							Intent intent = new Intent(YDispatchListActivity.this, PalletOutActivity.class);
							intent.putExtras((Bundle) item.clone());
							intent.putExtra("MOVE_DT_INFO", DISPATCH_DT_INFO);
							intent.putExtra("MOVE_DT", DISPATCH_DT);
							intent.putExtra("MOVE_DT_ORG", DISPATCH_DT_ORG);
							startActivityForResult(intent, 0, options.toBundle());
						}
					});

					Button btnReceipt = (Button) itemView.findViewById(R.id.btn_receipt);
					btnReceipt.setOnClickListener(new View.OnClickListener()
					{
						public void onClick(View v)
						{
							ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);

							Intent intent = new Intent(YDispatchListActivity.this, ReceiptCameraActivity.class);
							intent.putExtras((Bundle) item.clone());
							startActivityForResult(intent, 0, options.toBundle());
						}
					});


				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public void onBackPressed()
	{
		ConfirmCancelDialog.show(this, "앱을 종료하시겠습니까? \n관제도 종료됩니다.", "취소", "확인", false, new ConfirmCancelDialog.DialogListener() {
			public void onCancel()
			{

			}

			public void onConfirm()
			{
				/*GPS 서비스 종료*/
				requestGpsServiceStop();
				finish();
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}
}