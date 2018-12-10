package com.neognp.ytms.thirdparty.car_req;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;
import com.neognp.ytms.popup.RemarkInputDialog;
import com.trevor.library.util.AppUtil;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;

public class ThirdPartyCarRequestActivity extends BasicActivity {

    private boolean onReq;

    private Calendar curCal;

    private boolean isListPullUp;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private Button curDateBtn;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView list;

    JSONArray dataList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thirdparty_car_request_activity);

        curCal = Calendar.getInstance();
        curCal.set(Calendar.HOUR_OF_DAY, 0);
        curCal.set(Calendar.MINUTE, 0);
        curCal.set(Calendar.SECOND, 0);
        curCal.set(Calendar.MILLISECOND, 0);

        dataList = new JSONArray();

        setTitleBar("차량 요청", R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

        curDateBtn = (Button) findViewById(R.id.curDateBtn);
        curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.SwipeRefreshLayout_ColorScheme));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() { search(); }
        });

        list = findViewById(R.id.list);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);
        listAdapter = new ListAdapter();
        list.setAdapter(listAdapter);

        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // CAUTION 아이템 높이가 RecycleView 높이보다 큰 경우, 아래 메서드 사용시 index가 모두 -1 로 반환되므로 사용 금지
                //int firstItemIdx = layoutManager.findFirstCompletelyVisibleItemPosition();
                //int lastItemIdx = layoutManager.findLastCompletelyVisibleItemPosition();
                int firstItemIdx = layoutManager.findFirstVisibleItemPosition();
                int lastItemIdx = layoutManager.findLastVisibleItemPosition();

                Log.e(TAG, "+ onScrollStateChanged(): newState=" + newState + " / isListPullUp=" + isListPullUp + " / firstItemIdx=" + firstItemIdx + " / lastItemIdx=" + lastItemIdx);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!isListPullUp && firstItemIdx == 0) {
                        Log.i(TAG, "+ onScrollStateChanged(): request refresh !");
                    } else if (isListPullUp && lastItemIdx == listAdapter.getItemCount() - 1) {
                        Log.i(TAG, "+ onScrollStateChanged(): request next list !");
                        //requestList(false);
                    }
                }

                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    int scrollY = recyclerView.computeVerticalScrollOffset();
                    Log.e(TAG, "+ onScrollStateChanged(): scrollY=" + scrollY);
                }
            }

            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        list.setOnTouchListener(new View.OnTouchListener() {
            float prevY;

            public boolean onTouch(View v, MotionEvent event) {
                //Log.i(TAG, "+ MotionEvent: " + event);
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    // pull down
                    if (prevY < event.getY()) {
                        isListPullUp = false;
                    }
                    // pull up
                    else if (prevY > event.getY()) {
                        isListPullUp = true;
                    }
                    prevY = event.getY();
                }
                return false;
            }
        });

        ((TextView) findViewById(R.id.callCenterTxt)).setText("운송전략팀 연결");
        ((TextView) findViewById(R.id.callCenterPhoneNoTxt)).setText(getString(R.string.delivery_call_center_phone_no));

        init();
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    private void init() {
        try {
            search();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                finish();
                break;
            case R.id.titleRightBtn1:
                search();
                break;
            case R.id.prevDateBtn:
                setPrevDate();
                break;
            case R.id.curDateBtn:
                showCalendar();
                break;
            case R.id.nextDateBtn:
                setNextDate();
                break;
            case R.id.newBtn:
                insertData();
                break;
            case R.id.saveBtn:
                requestSave();
                break;
            case R.id.callCenterBtn:
                AppUtil.runCallApp(getString(R.string.delivery_call_center_phone_no), true);
                break;
        }
    }

    private void setPrevDate() {
        curCal.add(Calendar.DAY_OF_YEAR, -1);
        curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
        search();
    }

    private void setNextDate() {
        curCal.add(Calendar.DAY_OF_YEAR, 1);
        curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
        search();
    }

    private void showCalendar() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

                curCal.set(Calendar.YEAR, year);
                curCal.set(Calendar.MONTH, monthOfYear);
                curCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
            }
        }, curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH), curCal.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void search() {
        try {
            // 오늘 날짜 이후로 설정 금지
            //Calendar todayCal = Calendar.getInstance();
            //todayCal.set(Calendar.HOUR_OF_DAY, 0);
            //todayCal.set(Calendar.MINUTE, 0);
            //todayCal.set(Calendar.SECOND, 0);
            //todayCal.set(Calendar.MILLISECOND, 0);
            //if (curCal.after(todayCal)) {
            //    showSnackbar(R.drawable.ic_insert_invitation_black_24dp, "조회 종료일은 오늘 날짜 이후로 지정할 수 없습니다.");
            //    return;
            //}

            requestList(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void insertData() {
        try {
            int idx = dataList.length();
            JSONObject jObj = new JSONObject();
            jObj.put("SEQ", "");
            jObj.put("REQUEST_DT", "");
            jObj.put("CUST_CD", "");
            jObj.put("CUST_NM", "선택");
            jObj.put("TO_CENTER_CD", "");
            jObj.put("TO_CENTER_NM", "선택");
            jObj.put("PALLET_CNT", "0");
            jObj.put("REMARK", "");
            dataList.put(idx, jObj);

            Bundle bObj = new Bundle();
            bObj.putString("SEQ", "");
            bObj.putString("REQUEST_DT", "");
            bObj.putString("CUST_CD", "");
            bObj.putString("CUST_NM", "선택");
            bObj.putString("TO_CENTER_CD", "");
            bObj.putString("TO_CENTER_NM", "선택");
            bObj.putString("PALLET_CNT", "0");
            bObj.putString("REMARK", "");

            listItems.add(bObj);
            listAdapter.notifyDataSetChanged();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestSave() {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            final String fromDt = Key.SDF_PAYLOAD.format(curCal.getTime());

            for(int i=0; i < dataList.length(); i++)
            {
                JSONObject obj = dataList.getJSONObject(i);
                String custCd = obj.getString("CUST_CD");
                String toCenterCd = obj.getString("TO_CENTER_CD");
                String palletCnt = obj.getString("PALLET_CNT");
                if(custCd.isEmpty() || toCenterCd.isEmpty() || palletCnt.isEmpty())
                {
                    showToast("화주사 또는 하차지 입력이 완료되지 않았습니다.", true);
                    return;
                }
            }

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("requestDt", fromDt);
                        payloadJson.put("dataList", dataList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_THIRDPARTY_CAR_REQUEST_SAVE, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            requestList(false);
                        } else {
                            showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast(e.getMessage(), false);
                    }
                }
            }.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestList(final boolean reqNextPage) {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            final String fromDt = Key.SDF_PAYLOAD.format(curCal.getTime());

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    if (reqNextPage)
                        showLoadingDialog(null, false);
                    else
                        swipeRefreshLayout.setRefreshing(true);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("tplCd", Key.getUserInfo().getString("CLIENT_CD"));
                        payloadJson.put("requestDt", fromDt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_THIRDPARTY_CAR_REQUEST, false, payloadJson, true, true);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    if (reqNextPage)
                        dismissLoadingDialog();
                    else
                        swipeRefreshLayout.setRefreshing(false);

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {

                            JSONObject resJson = new JSONObject(response.getString(Key.resStr));
                            dataList = resJson.optJSONArray(Key.data);

                            ArrayList<Bundle> data = resBody.getParcelableArrayList("data");
                            addListItems(data);
                        } else {
                            showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast(e.getMessage(), false);
                    }
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized void addListItems(ArrayList<Bundle> items) {
        if (items == null)
            return;

        try {
            listItems.clear();
            listItems.addAll(items);
            listAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        static final int TYPE_LIST_ITEM = 0;

        public int getItemViewType(int position) {
            return listItems.get(position).getInt("viewType", TYPE_LIST_ITEM);
        }

        public int getItemCount() {
            return listItems.size();
        }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder holder = null;

            if (viewType == TYPE_LIST_ITEM) {
                View v = View.inflate(getContext(), R.layout.thirdparty_car_request_input_item, null);
                v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                holder = new ListItemView(v);
            }

            return holder;
        }

        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            ((ItemViewHolder) holder).onBindViewData(listItems.get(position));
        }

        abstract class ItemViewHolder extends RecyclerView.ViewHolder {
            public ItemViewHolder(View itemView) {
                super(itemView);
            }

            public abstract void onBindViewData(final Bundle item);
        }

        class ListItemView extends ThirdPartyCarRequestActivity.ListAdapter.ItemViewHolder {
            public ListItemView(View itemView) {
                super(itemView);
            }

            public void onBindViewData(Bundle item) {
                try {
                    int idx = getAdapterPosition();
                    JSONObject obj = dataList.getJSONObject(idx);

                    // 화주사
                    TextView custTxt = (TextView) itemView.findViewById(R.id.custTxt);
                    custTxt.setText("화주사 : " + item.getString("CUST_NM"));
                    custTxt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(ThirdPartyCarRequestActivity.this, ThirdPartySearchListActivity.class);
                            i.putExtra("searchGb", "CST");
                            i.putExtra("searchIdx", idx);
                            i.putExtra("searchKey", Key.getUserInfo().getString("CLIENT_CD"));
                            startActivityForResult(i, 100);
                        }
                    });

                    //하차지
                    TextView centerTxt = (TextView) itemView.findViewById(R.id.centerTxt);
                    centerTxt.setText("하차지 : " + item.getString("TO_CENTER_NM"));
                    centerTxt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String custCd = item.getString("CUST_CD");
                            if(custCd.isEmpty())
                            {
                                showToast("화주사가 선택되지 않았습니다.", true);
                                return;
                            }

                            Intent i = new Intent(ThirdPartyCarRequestActivity.this, ThirdPartySearchListActivity.class);
                            i.putExtra("searchGb", "CNT");
                            i.putExtra("searchIdx", idx);
                            i.putExtra("searchKey", item.getString("CUST_CD"));
                            startActivityForResult(i, 200);
                        }
                    });

                    // 팔레트수
                    EditText itemsCntEdit = itemView.findViewById(R.id.itemsCntEdit);
                    itemsCntEdit.setText(item.getString("PALLET_CNT", "0"));

                    // 비고
                    TextView remarkTxt = (TextView) itemView.findViewById(R.id.remarkTxt);
                    remarkTxt.setText("요청사항 : " + item.getString("REMARK", ""));

                    remarkTxt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RemarkInputDialog.show(ThirdPartyCarRequestActivity.this, new RemarkInputDialog.DialogListener() {
                                public void onCancel() {
                                }

                                public void onConfirm(final String remark) {
                                    remarkTxt.setText("요청사항 : " + remark);
                                    try
                                    {
                                        obj.put("REMARK", remark);
                                        listItems.get(idx).putString("REMARK", remark);
                                    }
                                    catch(Exception e)
                                    {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    });
                    ImageButton minusBtn = (ImageButton) itemView.findViewById(R.id.minusBtn);
                    minusBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                           String cnt = ((EditText)itemView.findViewById(R.id.itemsCntEdit)).getText().toString();
                           if(cnt.isEmpty() || cnt.equals("-"))
                               return;
                            int itemsCnt = Integer.parseInt(cnt);
                            if (itemsCnt - 1 >= 0) {
                                itemsCnt--;
                                itemsCntEdit.setText("" + itemsCnt);
                                try
                                {
                                    obj.put("PALLET_CNT", itemsCnt);
                                    listItems.get(idx).putInt("PALLET_CNT", itemsCnt);
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                    ImageButton plusBtn = (ImageButton) itemView.findViewById(R.id.plusBtn);
                    plusBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String cnt = ((EditText)itemView.findViewById(R.id.itemsCntEdit)).getText().toString();
                            if(cnt.isEmpty() || cnt.equals("-"))
                                return;
                            int itemsCnt = Integer.parseInt(cnt);
                            itemsCnt++;
                            itemsCntEdit.setText("" + itemsCnt);
                            try
                            {
                                obj.put("PALLET_CNT", itemsCnt);
                                listItems.get(idx).putInt("PALLET_CNT", itemsCnt);
                            }
                            catch(Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK)
        {
            int idx = data.getIntExtra("searchIdx", -1);
            String searchCd = data.getStringExtra("searchCd");
            String searchNm = data.getStringExtra("searchNm");
            if(idx >= 0)
            {
                try {

                    JSONObject obj = dataList.getJSONObject(idx);

                    if (requestCode == 100) {
                        TextView custTxt = list.getChildAt(idx).findViewById(R.id.custTxt);
                        custTxt.setText("화주사 : " + searchNm);
                        obj.put("CUST_CD", searchCd);
                        obj.put("CUST_NM", searchNm);
                        listItems.get(idx).putString("CUST_CD", searchCd);
                        listItems.get(idx).putString("CUST_NM", searchNm);
                    }
                    if (requestCode == 200) {
                        TextView centerTxt = list.getChildAt(idx).findViewById(R.id.centerTxt);
                        centerTxt.setText("하차지 : " + searchNm);
                        obj.put("TO_CENTER_CD", searchCd);
                        obj.put("TO_CENTER_NM", searchNm);
                        listItems.get(idx).putString("TO_CENTER_CD", searchCd);
                        listItems.get(idx).putString("TO_CENTER_NM", searchNm);
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

}