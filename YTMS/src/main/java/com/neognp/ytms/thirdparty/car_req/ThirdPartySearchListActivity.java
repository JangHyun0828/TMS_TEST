package com.neognp.ytms.thirdparty.car_req;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.util.Log;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class ThirdPartySearchListActivity extends BasicActivity {

    private boolean onReq;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private String searchCd, searchNm, searchGb, searchKey;

    private int searchIdx;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView list;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thirdparty_search_list_activity);

        Intent i = getIntent();
        searchGb = i.getStringExtra("searchGb");
        searchIdx = i.getIntExtra("searchIdx", -1);
        searchKey = i.getStringExtra("searchKey");
        String titleTxt = "하차지 선택";
        if("CST".equals(searchGb))
        {
            titleTxt = "화주사 선택";
        }
        setTitleBar(titleTxt, R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

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
            // 저장
            case R.id.bottomBtn0:
                select();
                break;
            // 닫기
            case R.id.bottomBtn1:
                finish();
                break;
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void select() {
        Intent intent = new Intent();
        intent.putExtra("searchCd", searchCd);
        intent.putExtra("searchNm", searchNm);
        intent.putExtra("searchIdx", searchIdx);
        setResult(RESULT_OK, intent);
        finish();
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestList() {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            searchCd = "";
            searchNm = "";
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
                        payloadJson.put("tplCd", searchKey);
                        payloadJson.put("custCd", searchKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String URL = API.URL_THIRDPARTY_CAR_REQUEST_CENTER;
                    if("CST".equals(searchGb))
                    {
                        URL = API.URL_THIRDPARTY_CAR_REQUEST_CUST;
                    }

                    return YTMSRestRequestor.requestPost(URL, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    swipeRefreshLayout.setRefreshing(false);

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
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
                View v = View.inflate(getContext(), R.layout.thirdparty_search_list_item, null);
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

        class ListItemView extends ThirdPartySearchListActivity.ListAdapter.ItemViewHolder {
            public ListItemView(View itemView) {
                super(itemView);
            }

            public void onBindViewData(Bundle item) {
                try {
                    TextView searchTxt = itemView.findViewById(R.id.searchTxt);

                    searchTxt.setText(item.getString("SEARCH_CDNM", ""));

                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for(int i=0; i < list.getAdapter().getItemCount(); i++)
                            {
                                TextView ct = list.getChildAt(i).findViewById(R.id.searchTxt);
                                ct.setSelected(false);
                            }
                            searchCd = item.getString("SEARCH_CD");
                            searchNm = item.getString("SEARCH_NM");
                            searchTxt.setSelected(true);
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