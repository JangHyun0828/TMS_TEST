package com.neognp.ytms.thirdparty.car_alloc;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.neognp.ytms.delivery.car_loc.CarLocationActivity;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class ThirdPartyCarAllocInfoActivity extends BasicActivity {

    private boolean onReq;

    private Calendar curCal;

    private boolean isListPullUp;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private Button curDateBtn;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView list;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thirdparty_car_alloc_info_activity);

        curCal = Calendar.getInstance();
        curCal.set(Calendar.HOUR_OF_DAY, 0);
        curCal.set(Calendar.MINUTE, 0);
        curCal.set(Calendar.SECOND, 0);
        curCal.set(Calendar.MILLISECOND, 0);

        setTitleBar("배차 정보", R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

        curDateBtn = (Button) findViewById(R.id.curDateBtn);
        curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));

        swipeRefreshLayout =  findViewById(R.id.swipeRefreshLayout);
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
            case R.id.bottomCenterInfoView:
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
    private synchronized void requestList(final boolean reqNextPage) {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            final String orderDt = Key.SDF_PAYLOAD.format(curCal.getTime());

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
                        payloadJson.put("orderDt", orderDt);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_THIRDPARTY_ALLOC_LIST, false, payloadJson, true, false);
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
                View v = View.inflate(getContext(), R.layout.thirdparty_car_alloc_info_item, null);
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

        class ListItemView extends ThirdPartyCarAllocInfoActivity.ListAdapter.ItemViewHolder {
            public ListItemView(View itemView) {
                super(itemView);
            }

            public void onBindViewData(Bundle item) {
                try {
                    SpannableString content = null;

                    // 상차일자
                    TextView dataTxt0 = ((TextView) itemView.findViewById(R.id.dataTxt0));
                    dataTxt0.setText("상차일자 : " + item.getString("ORDER_DT", ""));

                    // 하차일자
                    TextView dataTxt1 = ((TextView) itemView.findViewById(R.id.dataTxt1));
                    dataTxt1.setText("하차일자 : " + item.getString("ARRIVE_DT", ""));

                    // 하차지
                    ((TextView) itemView.findViewById(R.id.dataTxt2)).setText("하차지 : " + item.getString("CENTER_NM", ""));

                    // 배차상태
                    ((TextView) itemView.findViewById(R.id.dataTxt3)).setText("배차상태 : " + item.getString("STATUS", ""));

                    // 차량번호
                    TextView dataTxt4 = ((TextView) itemView.findViewById(R.id.dataTxt4));
                    String CAR_NO = "차량번호 : " + item.getString("CAR_NO", "");
                    content = new SpannableString(CAR_NO);
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    dataTxt4.setText(content);
                    dataTxt4.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if("".equals(item.getString("CAR_CD", "")))
                            {
                                return;
                            }
                            Intent intent = new Intent(ThirdPartyCarAllocInfoActivity.this, CarLocationActivity.class);
                            intent.putExtras((Bundle) item.clone());
                            startActivity(intent);
                        }
                    });

                    // 도착예정
                    ((TextView) itemView.findViewById(R.id.dataTxt5)).setText(item.getString("EST_DATE", ""));

                    // 차주연결
                    Button rightBtn0 = itemView.findViewById(R.id.palletsBtn);
                    rightBtn0.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            AppUtil.runCallApp(item.getString("DRIVER_HP"), true);
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
    }

}