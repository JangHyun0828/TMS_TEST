package com.neognp.dtmsapp_v1.car;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.common.GoogleApiAvailability;
import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.gps.GpsTrackingService;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;


public class RecordListActivity extends BasicActivity {

	private boolean onReq;
	private Calendar fromCal, toCal;
	private Button fromDateBtn, toDateBtn;
	private DrawerLayout dLayout;
	private GpsTrackingService mGpsTrackingService;
	private boolean onReqGpsTransmitCheck;
	private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
	private SwipeRefreshLayout swipeRefreshLayout;
	private RecyclerView list;
	private ListAdapter listAdapter;
	private boolean isListPullUp;

	private TextView txt_tot_amt;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.car_recordlist_activity);

		//Calendar 설정
		fromCal = Calendar.getInstance();
		fromCal.set(Calendar.DAY_OF_MONTH, fromCal.getActualMinimum(Calendar.DAY_OF_MONTH));
		fromCal.set(Calendar.HOUR_OF_DAY, 0);
		fromCal.set(Calendar.MINUTE, 0);
		fromCal.set(Calendar.SECOND, 0);
		fromCal.set(Calendar.MILLISECOND, 0);

		toCal = Calendar.getInstance();
		toCal.set(Calendar.HOUR_OF_DAY, 0);
		toCal.set(Calendar.MINUTE, 0);
		toCal.set(Calendar.SECOND, 0);
		toCal.set(Calendar.MILLISECOND, 0);

		//날짜 클릭시
		fromDateBtn = (Button) findViewById(R.id.fromDateBtn);
		fromDateBtn.setText(Key.SDF_CAL_DEFAULT.format(fromCal.getTime()));

		toDateBtn = (Button) findViewById(R.id.toDateBtn);
		toDateBtn.setText(Key.SDF_CAL_DEFAULT.format(toCal.getTime()));

		txt_tot_amt = findViewById(R.id.txt_tot_amt);

		init();
	}

	private void init()
	{
		try
		{
			//GoogleHandler Play 서비스 호환 여부 체크
			GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);

			getSwipeRefreshLayout();
			getList();

			selectRecord();
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
				selectRecord();
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
		selectRecord();
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
		super.onDestroy();
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

			case R.id.fromDateBtn:
				showCalendar(fromCal);
				break;
			case R.id.toDateBtn:
				showCalendar(toCal);
				break;
        }
    }

	private void setPrevDate()
	{
		fromCal.add(Calendar.DAY_OF_YEAR, -1);
		fromDateBtn.setText(Key.SDF_CAL_DEFAULT.format(fromCal.getTime()));
		selectRecord();
	}

	private void setNextDate()
	{
		toCal.add(Calendar.DAY_OF_YEAR, 1);
		toDateBtn.setText(Key.SDF_CAL_DEFAULT.format(toCal.getTime()));
		selectRecord();
	}

	private void showCalendar(@NonNull final Calendar cal) {
		if (cal == null)
			return;

		DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, monthOfYear);
				cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

				if (cal == fromCal)
				{
					fromDateBtn.setText(Key.SDF_CAL_DEFAULT.format(cal.getTime()));
					selectRecord();
				}
				else if (cal == toCal)
				{
					toDateBtn.setText(Key.SDF_CAL_DEFAULT.format(cal.getTime()));
					selectRecord();
				}
			}
		}, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

		datePickerDialog.show();
	}

	@SuppressLint("StaticFieldLeak")
	private synchronized void selectRecord()
	{
		if (onReq)
			return;

		try
		{
			if (Key.getUserInfo() == null)
				return;

			final String fromDt = Key.SDF_PAYLOAD.format(fromCal.getTime());
			final String toDt = Key.SDF_PAYLOAD.format(toCal.getTime());

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
						payloadJson.put("fromDt", fromDt);
						payloadJson.put("toDt", toDt);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					return RestRequestor.requestPost(API.URL_CAR_RECORD, false, payloadJson, true, false);
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
							txt_tot_amt.setText(TextUtil.formatCurrency(resBody.getInt("tot_amt")));
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
				View v = View.inflate(getContext(), R.layout.car_record_item, null);
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

		class ListItemView extends RecordListActivity.ListAdapter.ItemViewHolder
		{

			private ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);

			public ListItemView(View itemView)
			{
				super(itemView);
			}

			public void onBindViewData(Bundle item)
			{
				try
				{
					String DISPATCH_DT = item.getString("DISPATCH_DT", "");
					String DISPATCH_DT_INFO = DISPATCH_DT.substring(0, 4) + "년 " + DISPATCH_DT.substring(4, 6) + "월 " +DISPATCH_DT.substring(6, 8) + "일";
					String TRANS_AMT = TextUtil.formatCurrency(item.getString("TRANS_AMT", ""));

					TextView dataTxt0 = ((TextView) itemView.findViewById(R.id.txt_data0));
					dataTxt0.setText(DISPATCH_DT_INFO);

					TextView dataTxt1 = ((TextView) itemView.findViewById(R.id.txt_data1));
					dataTxt1.setText(TRANS_AMT);

					ConstraintLayout recordLayout = (ConstraintLayout) itemView.findViewById(R.id.layout_item);
					recordLayout.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							Intent intent = new Intent(RecordListActivity.this, RecordDetailActivity.class);
							intent.putExtras((Bundle) item.clone());
							startActivity(intent);
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
		setResult(RESULT_OK);
		finish();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}
}