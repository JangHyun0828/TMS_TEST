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

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.common.GoogleApiAvailability;
import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class PalletActivity extends BasicActivity {

    public static final int RESULT_INPUT_PALLETS_COUNT = 100;

    private boolean onReq;
    private Bundle args;

    private Calendar fromCal, toCal, curCal;
    private Button btnDate, btnPrevDate, btnNextDate, btnNewPallet;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private RecyclerView list;
    private ListAdapter listAdapter;
    private boolean isListPullUp;

    private Bundle data;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_pallets_activity);

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
        args = getIntent().getExtras();
        if (args == null)
        {
            showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
            return;
        }

        //GoogleHandler Play 서비스 호환 여부 체크
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);

        getSwipeRefreshLayout();
        getList();

        selectInPallet();
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
                selectInPallet();
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
        selectInPallet();
        super.onResume();
    }

    protected void onPause()
    {
        super.onPause();
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
                selectInPallet();
            }

        }, curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH), curCal.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void setPrevDate()
    {
        curCal.add(Calendar.DAY_OF_YEAR, -1);
        btnDate.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
        selectInPallet();
    }

    private void setNextDate()
    {
        curCal.add(Calendar.DAY_OF_YEAR, 1);
        btnDate.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
        selectInPallet();
    }

    @SuppressLint("NonConstantResourceId")
    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.prevDateBtn:
                setPrevDate();
                break;

            case R.id.nextDateBtn:
                setNextDate();
                break;

            case R.id.btn_new_pallet:
                ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);
                Intent intent = new Intent(PalletActivity.this, PalletInActivity.class);
                intent.putExtra("PAGE_GB", "NEW");
                intent.putExtra("DEPT_CD", Key.getUserInfo().getString("DEPT_CD"));
                intent.putExtra("DEPT_NM", Key.getUserInfo().getString("USER_DEPT_NM"));
                intent.putExtra("CAR_CD", Key.getUserInfo().getString("USER_CD"));
                intent.putExtra("CAR_NO", Key.getUserInfo().getString("PRF_NM"));
                intent.putExtra("MOVE_DT", Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
                startActivityForResult(intent, 100, options.toBundle());
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private synchronized void selectInPallet()
    {
        if (onReq)
            return;

        try
        {
            if (Key.getUserInfo() == null)
                return;

            //noinspection deprecation
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
                        payloadJson.put("moveDt", Key.SDF_PAYLOAD.format(curCal.getTime()));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    return RestRequestor.requestPost(API.URL_IN_PALLET_LIST, false, payloadJson, true, false);
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
                View v = View.inflate(getContext(), R.layout.car_pallets_item, null);
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

        class ListItemView extends ListAdapter.ItemViewHolder
        {
            public ListItemView(View itemView)
            {
                super(itemView);
            }

            public void onBindViewData(Bundle item)
            {
                try
                {
                    String CODE_INFO = item.getString("CODE_INFO", "");
                    String OUT_NM = item.getString("OUT_NM", "");
                    String MOVE_DT = item.getString("MOVE_DT", "");
                    String PALLET_NM = item.getString("PALLET_NM", "");
                    String MOVE_TYPE_NM = item.getString("MOVE_TYPE_NM", "");
                    String MOVE_QTY = item.getString("MOVE_QTY", "");
                    String REMARK = item.getString("REMARK", "");

                    TextView dataTxt0 = ((TextView) itemView.findViewById(R.id.dataTxt0));
                    dataTxt0.setText(CODE_INFO);

                    TextView dataTxt1 = ((TextView) itemView.findViewById(R.id.dataTxt1));
                    dataTxt1.setText(OUT_NM);

                    TextView dataTxt2 = ((TextView) itemView.findViewById(R.id.dataTxt2));
                    dataTxt2.setText(MOVE_TYPE_NM);

                    TextView dataTxt3 = ((TextView) itemView.findViewById(R.id.dataTxt3));
                    dataTxt3.setText(PALLET_NM);

                    TextView dataTxt4 = ((TextView) itemView.findViewById(R.id.dataTxt4));
                    dataTxt4.setText(MOVE_QTY);

                    TextView dataTxt5 = ((TextView) itemView.findViewById(R.id.dataTxt5));
                    dataTxt5.setText(REMARK);

                    ConstraintLayout palletLayout = (ConstraintLayout) itemView.findViewById(R.id.item_pallet);
                    palletLayout.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);
                            Intent intent = new Intent(PalletActivity.this, PalletInActivity.class);
                            intent.putExtras((Bundle) item.clone());
                            intent.putExtra("MOVE_DT", Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
                            intent.putExtra("PAGE_GB", "EDIT");
                            startActivityForResult(intent, 100, options.toBundle());
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try
        {
            if(resultCode == 100)
            {
                selectInPallet();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}