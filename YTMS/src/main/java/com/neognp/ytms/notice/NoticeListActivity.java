package com.neognp.ytms.notice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
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
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class NoticeListActivity extends BasicActivity {

    private boolean onReq;

    private boolean isListPullUp;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView list;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notice_list_activity);

        setTitleBar("공지사항", R.drawable.selector_button_back, 0, 0);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.SwipeRefreshLayout_ColorScheme));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                requestList(false);
            }
        });

        list = (RecyclerView) findViewById(R.id.list);
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
            requestList(false);
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
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestList(final boolean reqNextPage) {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

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
                        payloadJson.put("authCd", Key.getUserInfo().getString("AUTH_CD"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_NOTICE, false, payloadJson, true, false);
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

                    // TEST
                    //ArrayList<Bundle> list = new ArrayList<Bundle>();
                    //for (int i = 0; i < 100; i++) {
                    //    Bundle item = new Bundle();
                    //    list.add(item);
                    //}
                    //addListItems(list, reqNextPage);
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
                View v = View.inflate(getContext(), R.layout.notice_list_item, null);
                //v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
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


        // TODO 더불러오기로 아이템 추가시 새로 추가된 아이템이 EXPAND 되지 않는 문제
        class ListItemView extends ItemViewHolder {
            public ListItemView(View itemView) {
                super(itemView);
            }

            public void onBindViewData(Bundle item) {
                try {
                    View titleBgView = itemView.findViewById(R.id.titleBgView);

                    String NOTI_READ_YN = item.getString("NOTI_READ_YN", "N");
                    TextView newTxt = ((TextView) itemView.findViewById(R.id.newTxt));
                    if (NOTI_READ_YN.equalsIgnoreCase("Y"))
                        newTxt.setVisibility(View.INVISIBLE);
                    else
                        newTxt.setVisibility(View.VISIBLE);

                    TextView titleTxt = ((TextView) itemView.findViewById(R.id.titleTxt));
                    titleTxt.setText(item.getString("NOTI_TITLE", ""));

                    // 팝업 안의 화살표 이미지 회전 에니메이션
                    ImageView toggleImg = (ImageView) itemView.findViewById(R.id.toggleImg);

                    TextView contentTxt = ((TextView) itemView.findViewById(R.id.contentTxt));
                    contentTxt.setText(item.getString("NOTI_CONT", "").replace("\\n", "\n"));

                    //ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) contentTxt.getLayoutParams();
                    //if (item.getBoolean("expand")) {
                    //    params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                    //    contentTxt.setLayoutParams(params);
                    //    contentTxt.setVisibility(View.VISIBLE);
                    //    contentTxt.requestLayout();
                    //} else {
                    //    params.height = 0;
                    //    contentTxt.setLayoutParams(params);
                    //    contentTxt.setVisibility(View.GONE);
                    //    contentTxt.requestLayout();
                    //}

                    if (item.getBoolean("expand")) {
                        Log.e(TAG, "+ onBindViewData()1: " + listItems.indexOf(item));
                        toggleImg.setRotation(180);
                        contentTxt.setVisibility(View.VISIBLE);
                    } else {
                        toggleImg.setRotation(0);
                        contentTxt.setVisibility(View.GONE);
                    }

                    titleBgView.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            item.putBoolean("expand", !item.getBoolean("expand"));
                            listAdapter.notifyItemChanged(listItems.indexOf(item));

                            if (item.getBoolean("expand")) {
                                Log.e(TAG, "+ onBindViewData()2: " + listItems.indexOf(item));
                                toggleImg.startAnimation(AnimationUtils.loadAnimation(getContext(), com.trevor.library.R.anim.rotation_to_right_180));

                                if (NOTI_READ_YN.equalsIgnoreCase("N"))
                                    requestNoticeRead(item);
                            } else {
                                toggleImg.startAnimation(AnimationUtils.loadAnimation(getContext(), com.trevor.library.R.anim.rotation_to_left_180));
                            }
                        }
                    });

                    contentTxt.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            String NOTI_TYPE = item.getString("NOTI_TYPE", "0");
                            // 휴무일 조사
                            if (NOTI_TYPE.equals("1"))
                                startActivityForResult(new Intent(NoticeListActivity.this, PersonalDaySurveyActivity.class), 0);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @SuppressLint ("StaticFieldLeak")
        private synchronized void requestNoticeRead(Bundle item) {
            if (onReq)
                return;

            try {
                if (Key.getUserInfo() == null)
                    return;

                if (item == null)
                    return;

                new AsyncTask<Void, Void, Bundle>() {
                    protected void onPreExecute() {
                    }

                    protected Bundle doInBackground(Void... arg0) {
                        JSONObject payloadJson = null;
                        try {
                            payloadJson = YTMSRestRequestor.buildPayload();
                            payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                            payloadJson.put("authCd", Key.getUserInfo().getString("AUTH_CD"));
                            payloadJson.put("notiCd", item.getString("NOTI_CD"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return YTMSRestRequestor.requestPost(API.URL_NOTICE_READ, false, payloadJson, true, false);
                    }

                    protected void onPostExecute(Bundle response) {
                        try {
                            Bundle resBody = response.getBundle(Key.resBody);
                            String result_code = resBody.getString(Key.result_code);
                            String result_msg = resBody.getString(Key.result_msg);

                            if (result_code.equals("200")) {
                                item.putString("NOTI_READ_YN", "Y");
                                listAdapter.notifyItemChanged(listItems.indexOf(item));
                            }
                        } catch (Exception e) {
                        }
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}