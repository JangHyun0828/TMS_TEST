package com.neognp.ytms.carowner.car_alloc;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.popup.LocationInfoDialog;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class CarAllocHistoryActivity extends BasicActivity {

    public static final int RESULT_INPUT_PALLETS_COUNT = 100;
    public static final int RESULT_SAVED_RECEIPT = 200;

    private boolean onReq;

    private Calendar fromCal, toCal;

    private boolean isListPullUp;
    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private Button fromDateBtn, toDateBtn;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView list;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_alloc_history_activity);

        toCal = Calendar.getInstance();
        toCal.set(Calendar.HOUR_OF_DAY, 0);
        toCal.set(Calendar.MINUTE, 0);
        toCal.set(Calendar.SECOND, 0);
        toCal.set(Calendar.MILLISECOND, 0);

        fromCal = Calendar.getInstance();
        fromCal.setTimeInMillis(toCal.getTimeInMillis());
        fromCal.add(Calendar.DAY_OF_MONTH, -31); // TEST

        setTitleBar("배차 내역 확인", R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

        fromDateBtn = (Button) findViewById(R.id.fromDateBtn);
        fromDateBtn.setText(Key.SDF_CAL_DEFAULT.format(fromCal.getTime()));

        toDateBtn = (Button) findViewById(R.id.toDateBtn);
        toDateBtn.setText(Key.SDF_CAL_DEFAULT.format(toCal.getTime()));

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.SwipeRefreshLayout_ColorScheme));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                search();
            }
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
                        //requestList(true);
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
            case R.id.fromDateBtn:
                showCalendar(fromCal);
                break;
            case R.id.toDateBtn:
                showCalendar(toCal);
                break;
            case R.id.nextDateBtn:
                setNextDate();
                break;
        }
    }

    private void setPrevDate() {
        fromCal.add(Calendar.DAY_OF_YEAR, -1);
        fromDateBtn.setText(Key.SDF_CAL_DEFAULT.format(fromCal.getTime()));
        search();
    }

    private void setNextDate() {
        toCal.add(Calendar.DAY_OF_YEAR, 1);
        toDateBtn.setText(Key.SDF_CAL_DEFAULT.format(toCal.getTime()));
        search();
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
                    fromDateBtn.setText(Key.SDF_CAL_DEFAULT.format(cal.getTime()));
                else if (cal == toCal)
                    toDateBtn.setText(Key.SDF_CAL_DEFAULT.format(cal.getTime()));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void search() {
        try {
            // 시작일을 종료일 이후 날짜로 설정 금지
            if (fromCal.after(toCal)) {
                showSnackbar(R.drawable.ic_insert_invitation_black_24dp, "조회 시작일을 종료일 이전 날짜로 지정해 주십시요.");
                return;
            }

            // 조회 종료일을 오늘 날짜 이후로 설정 금지
            //Calendar todayCal = Calendar.getInstance();
            //todayCal.set(Calendar.HOUR_OF_DAY, 0);
            //todayCal.set(Calendar.MINUTE, 0);
            //todayCal.set(Calendar.SECOND, 0);
            //todayCal.set(Calendar.MILLISECOND, 0);
            //if (toCal.after(todayCal)) {
            //    showSnackbar(R.drawable.ic_insert_invitation_black_24dp, "조회 종료일은 오늘 날짜 이후로 지정할 수 없습니다.");
            //    return;
            //}

            // 조회 기간 최대 31일로 제한
            //long daysInRange = DateUtil.daysBetween(fromCal, toCal);
            //Log.e(TAG, "+ search(): daysInRange=" + daysInRange);
            //if (daysInRange > 31) {
            //    showSnackbar(R.drawable.ic_insert_invitation_black_24dp, "조회 기간은 최대 31일 이내로 설정해 주십시요.");
            //    return;
            //}

            requestList(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestFreightCharge(Bundle item) {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            if (item == null)
                return;

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, false);

                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("orderNo", item.getString("ORDER_NO"));
                        payloadJson.put("dispatchNo", item.getString("DISPATCH_NO"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_CAR_FREIGHT_CHARGE_REQUEST, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();


                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            search();
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

            final String fromDt = Key.SDF_PAYLOAD.format(fromCal.getTime());
            final String toDt = Key.SDF_PAYLOAD.format(toCal.getTime());

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
                        payloadJson.put("fromDt", fromDt);
                        payloadJson.put("toDt", toDt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_CAR_ALLOC_LIST, false, payloadJson, true, false);
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
                            ArrayList<Bundle> data = resBody.getParcelableArrayList("data");
                            addListItems(data, reqNextPage);
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

    synchronized void addListItems(ArrayList<Bundle> items, boolean reqNextPage) {
        if (items == null)
            return;

        try {
            if (!reqNextPage) {
                listItems.clear();
                listItems.addAll(items);
                listAdapter.notifyDataSetChanged();
            } else {
                listItems.addAll(items);
                listAdapter.notifyItemRangeInserted(items.size(), items.size());
            }
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
                View v = View.inflate(getContext(), R.layout.car_alloc_history_item, null);
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

        class ListItemView extends ListAdapter.ItemViewHolder {

            private ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);

            public ListItemView(View itemView) {
                super(itemView);
            }

            public void onBindViewData(Bundle item) {
                try {
                    // 배차일자
                    String DISPATCH_DT = Key.SDF_CAL_WEEKDAY.format(Key.SDF_PAYLOAD.parse(item.getString("DISPATCH_DT", "")));
                    ((TextView) itemView.findViewById(R.id.dataTxt0)).setText("일자: " + DISPATCH_DT);

                    // 고객사
                    ((TextView) itemView.findViewById(R.id.dataTxt1)).setText("고객사: " + item.getString("CUST_NM", ""));

                    // 상차지
                    TextView dataTxt2 = ((TextView) itemView.findViewById(R.id.dataTxt2));
                    String FROM_CENTER_NM = "상차지: " + item.getString("FROM_CENTER_NM", "");
                    SpannableString content = new SpannableString(FROM_CENTER_NM);
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    dataTxt2.setText(content);
                    dataTxt2.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            LocationInfoDialog.show(CarAllocHistoryActivity.this, "상차지", item.getString("FROM_CENTER_ADDR", ""), item.getString("FROM_CENTER_EMP", ""), item.getString("FROM_CENTER_TEL", ""));
                        }
                    });

                    // 하차지
                    TextView dataTxt3 = ((TextView) itemView.findViewById(R.id.dataTxt3));
                    String TO_CENTER_NM = "하차지: " + item.getString("TO_CENTER_NM", "");
                    content = new SpannableString(TO_CENTER_NM);
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    dataTxt3.setText(content);
                    dataTxt3.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            LocationInfoDialog.show(CarAllocHistoryActivity.this, "하차지", item.getString("TO_CENTER_ADDR", ""), item.getString("TO_CENTER_EMP", ""), item.getString("TO_CENTER_TEL", ""));
                        }
                    });

                    // 비고
                    ((TextView) itemView.findViewById(R.id.dataTxt4)).setText(item.getString("REMARK", ""));

                    // 하차순서 확인
                    TextView dataTxt5 = ((TextView) itemView.findViewById(R.id.dataTxt5));
                    content = new SpannableString(dataTxt5.getText());
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    dataTxt5.setText(content);
                    dataTxt5.setOnClickListener(new View.OnClickListener() {
                        @SuppressLint ("RestrictedApi")
                        public void onClick(View v) {
                            Intent intent = new Intent(CarAllocHistoryActivity.this, ForkLiftCheckActivity.class);
                            intent.putExtras((Bundle) item.clone());
                            startActivityForResult(intent, 0, options.toBundle());
                        }
                    });

                    // 팔레트 입력
                    Button palletsBtn = itemView.findViewById(R.id.palletsBtn);
                    palletsBtn.setOnClickListener(new View.OnClickListener() {
                        @SuppressLint ("RestrictedApi")
                        public void onClick(View v) {
                            Intent intent = new Intent(CarAllocHistoryActivity.this, PalletsInputActivity.class);
                            intent.putExtras(item);
                            startActivityForResult(intent, 0, options.toBundle());
                        }
                    });

                    // 인수증
                    Button cameraBtn = itemView.findViewById(R.id.cameraBtn);
                    cameraBtn.setOnClickListener(new View.OnClickListener() {
                        @SuppressLint ("RestrictedApi")
                        public void onClick(View v) {
                            Intent intent = new Intent(CarAllocHistoryActivity.this, ReceiptPhotoLowVersionActivity.class);
                            intent.putExtras(item);
                            startActivityForResult(intent, 0, options.toBundle());
                        }
                    });

                    // 상차완료 운임받기
                    Button chargeBtn = itemView.findViewById(R.id.chargeBtn);
                    chargeBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            requestFreightCharge(item);
                        }
                    });

                    String RECEIPT_YN = item.getString("RECEIPT_YN", "");
                    String STATUS = item.getString("STATUS", "");

                    // 집하
                    if (item.getString("ORDER_TYPE").equals("100")) {
                        cameraBtn.setVisibility(View.GONE);
                        // P : 팔레트입력 / 인수증전송 완료
                        if (STATUS.equalsIgnoreCase("P")) {
                            palletsBtn.setVisibility(View.GONE);
                            chargeBtn.setVisibility(View.VISIBLE);
                            chargeBtn.setEnabled(true);
                        }
                        // Y : 배송완료(상차완료)
                        else if (STATUS.equalsIgnoreCase("Y")) {
                            palletsBtn.setVisibility(View.GONE);
                            chargeBtn.setVisibility(View.GONE);
                        } else {
                            palletsBtn.setVisibility(View.VISIBLE);
                            chargeBtn.setVisibility(View.GONE);
                            chargeBtn.setEnabled(false);
                        }
                    }
                    // 일반
                    else if (item.getString("ORDER_TYPE").equals("200")) {
                        palletsBtn.setVisibility(View.GONE);
                        cameraBtn.setVisibility(View.VISIBLE);
                        chargeBtn.setVisibility(View.VISIBLE);
                        // P : 팔레트입력 / 인수증전송 완료
                        if (STATUS.equalsIgnoreCase("P")) {
                            chargeBtn.setEnabled(true);
                        }
                        // Y : 배송완료(상차완료)
                        else if (STATUS.equalsIgnoreCase("Y")) {
                            palletsBtn.setVisibility(View.GONE);
                            cameraBtn.setVisibility(View.GONE);
                            chargeBtn.setVisibility(View.GONE);
                        } else {
                            chargeBtn.setEnabled(false);
                        }
                    }
                    // 간선
                    else if (item.getString("ORDER_TYPE").equals("300")) {
                        palletsBtn.setVisibility(View.GONE);
                        cameraBtn.setVisibility(View.VISIBLE);
                        chargeBtn.setVisibility(View.VISIBLE);
                        // P : 팔레트입력 / 인수증전송 완료
                        if (STATUS.equalsIgnoreCase("P")) {
                            chargeBtn.setEnabled(true);
                        }
                        // Y : 배송완료(상차완료)
                        else if (STATUS.equalsIgnoreCase("Y")) {
                            palletsBtn.setVisibility(View.GONE);
                            cameraBtn.setVisibility(View.GONE);
                            chargeBtn.setVisibility(View.GONE);
                        } else {
                            chargeBtn.setEnabled(false);
                        }
                    }
                    // 직송
                    else if (item.getString("ORDER_TYPE").equals("400")) {
                        palletsBtn.setVisibility(View.GONE);
                        cameraBtn.setVisibility(View.VISIBLE);
                        chargeBtn.setVisibility(View.VISIBLE);
                        // P : 팔레트입력 / 인수증전송 완료
                        if (STATUS.equalsIgnoreCase("P")) {
                            chargeBtn.setEnabled(true);
                        }
                        // Y : 배송완료(상차완료)
                        else if (STATUS.equalsIgnoreCase("Y")) {
                            chargeBtn.setVisibility(View.GONE);
                        } else {
                            chargeBtn.setEnabled(false);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            // 팔레트 입력 완료
            if (resultCode == RESULT_INPUT_PALLETS_COUNT) {
                if (data == null)
                    return;

                //Bundle item = data.getExtras();
                //listAdapter.notifyItemChanged(listItems.indexOf(item));
                search();
            }
            // 인수증 저장 완료
            else if (resultCode == RESULT_SAVED_RECEIPT) {
                if (data == null)
                    return;

                //Bundle item = data.getExtras();
                //listAdapter.notifyItemChanged(listItems.indexOf(item));
                search();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}