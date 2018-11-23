package com.neognp.ytms.notice;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class PersonalDaySurveyActivity extends BasicActivity {

    private boolean onReq;

    private Calendar curCal;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private Button dateBtn;

    private TextView sumTxt;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView list;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.personal_day_survey_activity);

        curCal = Calendar.getInstance();
        curCal.set(Calendar.HOUR_OF_DAY, 0);
        curCal.set(Calendar.MINUTE, 0);
        curCal.set(Calendar.SECOND, 0);
        curCal.set(Calendar.MILLISECOND, 0);

        setTitleBar("휴무일 조사", R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

        dateBtn = findViewById(R.id.dateBtn);

        ((TextView) findViewById(R.id.sumTitle)).setText("휴무일수");
        sumTxt = findViewById(R.id.sumTxt);
        ((TextView) findViewById(R.id.sumUnitTxt)).setText("일");

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.SwipeRefreshLayout_ColorScheme));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                requestList();
            }
        });

        list = findViewById(R.id.list);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);
        listAdapter = new ListAdapter();
        list.setAdapter(listAdapter);

        //((TextView) findViewById(R.id.callCenterTxt)).setText("고객 센타");
        //((TextView) findViewById(R.id.callCenterPhoneNoTxt)).setText(getString(R.string.customer_call_center_phone_no));

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
            requestList();
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
                requestList();
                break;
            case R.id.dateBtn:
                showCalendar();
                break;
            // 삭제
            case R.id.bottomBtn0:
                requestPersonalDayDelete();
                break;
            // 닫기
            case R.id.bottomBtn1:
                finish();
                break;
            //case R.id.callCenterBtn:
            //    AppUtil.runCallApp(getString(R.string.customer_call_center_phone_no), true);
            //    break;
        }
    }

    private void showCalendar() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

                curCal.set(Calendar.YEAR, year);
                curCal.set(Calendar.MONTH, monthOfYear);
                curCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                requestPersonalDayAdd(Key.SDF_PAYLOAD.format(curCal.getTime()));
            }
        }, curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH), curCal.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestPersonalDayAdd(String selectDt) {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            if (selectDt == null)
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
                        payloadJson.put("clientCd", Key.getUserInfo().getString("CLIENT_CD"));
                        payloadJson.put("selectDt", selectDt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_PERSONAL_DAY_ADD, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            requestList();
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
    private synchronized void requestPersonalDayDelete() {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            String dates = "";
            for (Bundle item : listItems) {
                if (item.getBoolean("checked"))
                    dates += item.getString("SELECT_DT", "") + ",";
            }
            dates = dates.substring(0, dates.length() - 1);
            if (dates.isEmpty())
                return;

            final String selectDt = dates;

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
                        payloadJson.put("clientCd", Key.getUserInfo().getString("CLIENT_CD"));
                        payloadJson.put("selectDt", selectDt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_PERSONAL_DAY_DELETE, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            requestList();
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
    private synchronized void requestList() {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    swipeRefreshLayout.setRefreshing(true);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("clientCd", Key.getUserInfo().getString("CLIENT_CD"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_PERSONAL_DAY_LIST, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    swipeRefreshLayout.setRefreshing(false);

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            sumTxt.setText("" + resBody.getInt("list_cnt"));

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
                View v = View.inflate(getContext(), R.layout.personal_day_survey_item, null);
                v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                holder = new ListAdapter.ListItemView(v);
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
            public ListItemView(View itemView) {
                super(itemView);
            }

            public void onBindViewData(Bundle item) {
                try {
                    TextView dateTxt = itemView.findViewById(R.id.dateTxt);
                    String SELECT_DT = Key.SDF_CAL_WEEKDAY.format(Key.SDF_PAYLOAD.parse(item.getString("SELECT_DT", "")));
                    dateTxt.setText(SELECT_DT);

                    if (item.getBoolean("checked"))
                        dateTxt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.date_check_box_on, 0, 0, 0);
                    else
                        dateTxt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.date_check_box_off, 0, 0, 0);

                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            item.putBoolean("checked", !item.getBoolean("checked"));
                            listAdapter.notifyItemChanged(listItems.indexOf(item));
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