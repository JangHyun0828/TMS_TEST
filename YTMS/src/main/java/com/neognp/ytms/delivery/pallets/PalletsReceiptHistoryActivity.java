package com.neognp.ytms.delivery.pallets;

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
import android.widget.ImageView;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.popup.CountEditDialog;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class PalletsReceiptHistoryActivity extends BasicActivity {

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
        setContentView(R.layout.pallets_receipt_history_activity);

        toCal = Calendar.getInstance();
        toCal.set(Calendar.HOUR_OF_DAY, 0);
        toCal.set(Calendar.MINUTE, 0);
        toCal.set(Calendar.SECOND, 0);
        toCal.set(Calendar.MILLISECOND, 0);

        fromCal = Calendar.getInstance();
        fromCal.setTimeInMillis(toCal.getTimeInMillis());
        fromCal.add(Calendar.DAY_OF_MONTH, -31); // TEST

        setTitleBar("팔레트 요청 접수내역", R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

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
            // 삭제
            case R.id.bottomBtn0:
                requestPalletsReceiptDelete();
                break;
            // 발송내역
            case R.id.bottomBtn1: {
                Intent intent = new Intent(this, PalletsDispatchHistoryActivity.class);
                startActivity(intent);
            }
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
    private synchronized void requestPalletsReceiptDelete() {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            String selSeq = "";
            for (Bundle item : listItems) {
                if (item.getBoolean("checked"))
                    selSeq += item.getString("SEQ", "") + ",";
            }
            selSeq = selSeq.substring(0, selSeq.length() - 1);
            if (selSeq.isEmpty())
                return;

            final String seq = selSeq;

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
                        payloadJson.put("seq", seq);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_DELIVERY_PALLETS_RECEIPT_DELETE, false, payloadJson, true, false);
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
    private synchronized void requestPalletsReceiptDispatch(Bundle item, int palletCnt) {
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
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("seq", item.getString("SEQ"));
                        payloadJson.put("palletCnt", palletCnt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_DELIVERY_PALLETS_DISPATCH, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            item.putString("STATUS", "Y");
                            listAdapter.notifyItemChanged(listItems.indexOf(item));
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
                    return YTMSRestRequestor.requestPost(API.URL_DELIVERY_PALLETS_RECEIPT_HISTORY, false, payloadJson, true, false);
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

    private synchronized void addListItems(ArrayList<Bundle> items, boolean reqNextPage) {
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
                View v = View.inflate(getContext(), R.layout.pallets_receipt_history_item, null);
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

        class ListItemView extends PalletsReceiptHistoryActivity.ListAdapter.ItemViewHolder {
            public ListItemView(View itemView) {
                super(itemView);
            }

            public void onBindViewData(Bundle item) {
                try {
                    ImageView checkImg = itemView.findViewById(R.id.checkImg);
                    if (item.getBoolean("checked"))
                        checkImg.setImageResource(R.drawable.date_check_box_on);
                    else
                        checkImg.setImageResource(R.drawable.date_check_box_off);

                    // 연번
                    ((TextView) itemView.findViewById(R.id.dataTxt0)).setText(item.getString("NO"));

                    // 화주명
                    ((TextView) itemView.findViewById(R.id.dataTxt1)).setText(item.getString("CUST_NM"));

                    // 요청일자
                    String REQUEST_DT = Key.SDF_CAL_DEFAULT.format(Key.SDF_PAYLOAD.parse(item.getString("REQUEST_DT", "")));
                    ((TextView) itemView.findViewById(R.id.dataTxt2)).setText(REQUEST_DT);

                    //  TODO 수량
                    //((TextView) itemView.findViewById(R.id.dataTxt3)).setText(item.getString(""));

                    // 처리
                    TextView receiptBtn = itemView.findViewById(R.id.receiptBtn);
                    TextView dispatchBtn = itemView.findViewById(R.id.dispatchBtn);
                    TextView completionBtn = itemView.findViewById(R.id.completionBtn);

                    String STATUS = item.getString("STATUS", "N");
                    // TODO 접수
                    if (STATUS.equalsIgnoreCase("Y")) {
                        checkImg.setVisibility(View.VISIBLE);
                        receiptBtn.setVisibility(View.VISIBLE);
                        dispatchBtn.setVisibility(View.INVISIBLE);
                        completionBtn.setVisibility(View.INVISIBLE);
                    }
                    // TODO 발송
                    else {
                        checkImg.setVisibility(View.VISIBLE);
                        receiptBtn.setVisibility(View.INVISIBLE);
                        dispatchBtn.setVisibility(View.VISIBLE);
                        completionBtn.setVisibility(View.INVISIBLE);
                    }
                    // TODO 완료
                    {
                        checkImg.setVisibility(View.VISIBLE);
                        receiptBtn.setVisibility(View.INVISIBLE);
                        dispatchBtn.setVisibility(View.INVISIBLE);
                        completionBtn.setVisibility(View.VISIBLE);
                    }

                    receiptBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                        }
                    });

                    dispatchBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            showPalletsCountEditDialog(item);
                        }
                    });

                    itemView.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (STATUS.equalsIgnoreCase("N")) {
                                item.putBoolean("checked", !item.getBoolean("checked"));
                                //listAdapter.notifyItemChanged(listItems.indexOf(item));
                                listAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void showPalletsCountEditDialog(final Bundle item) {
        if (item == null)
            return;

        CountEditDialog.show(this, "팔레트 수량 입력", 4, new CountEditDialog.DialogListener() {
            public void onCancel() {
            }

            public void onConfirm(String count) {
                try {
                    requestPalletsReceiptDispatch(item, Integer.parseInt(count));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}