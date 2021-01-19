package com.neognp.dtmsapp_v1.car;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

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


public class RecordDetailActivity extends BasicActivity {

	private boolean onReq;
	private DrawerLayout dLayout;
	private GpsTrackingService mGpsTrackingService;
	private boolean onReqGpsTransmitCheck;
	private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
	private SwipeRefreshLayout swipeRefreshLayout;
	private RecyclerView list;
	private ListAdapter listAdapter;
	private boolean isListPullUp;

	private Bundle args;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.car_record_detail_activity);

		init();
	}

	private void init()
	{
		try
		{
			args = getIntent().getExtras();

			//GoogleHandler Play 서비스 호환 여부 체크
			GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);

			getSwipeRefreshLayout();
			getList();

			selectRecordDetail();
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
				selectRecordDetail();
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

        }
    }

	@SuppressLint("StaticFieldLeak")
	private synchronized void selectRecordDetail()
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
						payloadJson.put("dispatchDt", args.getString("DISPATCH_DT"));
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					return RestRequestor.requestPost(API.URL_CAR_RECORD_DETAIL, false, payloadJson, true, false);
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
				View v = View.inflate(getContext(), R.layout.car_record_detail_item, null);
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

		class ListItemView extends RecordDetailActivity.ListAdapter.ItemViewHolder
		{
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
					String TRANS_AMT = TextUtil.formatCurrency(item.getString("TRANS_AMT", ""));
					String TOT_AMT = TextUtil.formatCurrency(item.getString("TOT_AMT", ""));
					String ITEM_NM_INFO = item.getString("ITEM_NM_INFO", "");
					String DELIVERY_DT_INFO = DELIVERY_DT.substring(0, 4) + "년 " + DELIVERY_DT.substring(4, 6) + "월 " +DELIVERY_DT.substring(6, 8) + "일";

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

					TextView dataTxt9 = ((TextView) itemView.findViewById(R.id.dataTxt9));
					dataTxt9.setText(TRANS_AMT);

					TextView dataTxt10 = ((TextView) findViewById(R.id.txt_tot_amt));
					dataTxt10.setText(TOT_AMT);
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