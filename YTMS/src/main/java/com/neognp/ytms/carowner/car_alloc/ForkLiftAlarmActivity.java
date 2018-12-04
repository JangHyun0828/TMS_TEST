package com.neognp.ytms.carowner.car_alloc;

import android.annotation.SuppressLint;
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
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.notice.NoticeListActivity;
import com.neognp.ytms.notice.PersonalDaySurveyActivity;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class ForkLiftAlarmActivity extends BasicActivity {

    private boolean onReq;
    private Bundle args;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView list;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forklift_alarm_activity);

        setTitleBar("하차 준비 알림", R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.SwipeRefreshLayout_ColorScheme));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                requestProcessStatus();
            }
        });

        list = findViewById(R.id.list);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);
        listAdapter = new ListAdapter();
        list.setAdapter(listAdapter);

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
            args = getIntent().getExtras();
            if (args == null)
                return;

            requestProcessStatus();
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
                requestProcessStatus();
                break;
            case R.id.bottomBtn0:
                finish();
                break;
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestProcessStatus() {
        if (onReq)
            return;

        try {
            if (args == null)
                return;

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
                        payloadJson.put("carCd", args.getString("CAR_CD"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_CAR_FORK_LIFT_ORDER, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    swipeRefreshLayout.setRefreshing(false);

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            Bundle data = resBody.getBundle(Key.data);

                            // 하차구역
                            ((TextView) findViewById(R.id.areaTxt)).setText(data.getString("AREA_NM", " "));

                            // 대기순번
                            String READY_NO = data.getString("READY_NO", "");
                            if (READY_NO.equals("0"))
                                ((TextView) findViewById(R.id.orderTxt)).setText("대기 순번: 입차해 주세요");
                            else
                                ((TextView) findViewById(R.id.orderTxt)).setText("대기 순번: " + READY_NO + "번");

                            // 진행상황
                            ArrayList<Bundle> ready_list = resBody.getParcelableArrayList("ready_list");
                            addListItems(ready_list);
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
                View v = View.inflate(getContext(), R.layout.fork_alarm_list_item, null);
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
            public ListItemView(View itemView) {
                super(itemView);
            }

            public void onBindViewData(Bundle item) {
                try {
                    // 대기순번
                    String READY_NO = item.getString("READY_NO", "");
                    if (READY_NO.equals("0")) {
                        itemView.findViewById(R.id.liftDownTxt).setVisibility(View.VISIBLE);
                        itemView.findViewById(R.id.standbyTxt).setVisibility(View.INVISIBLE);
                    } else {
                        itemView.findViewById(R.id.liftDownTxt).setVisibility(View.INVISIBLE);
                        itemView.findViewById(R.id.standbyTxt).setVisibility(View.VISIBLE);
                    }

                    ((TextView) itemView.findViewById(R.id.dataTxt0)).setText(item.getString("CAR_NO", ""));
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