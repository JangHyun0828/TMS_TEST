package com.neognp.dtmsapp_v1.cust;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class NoticeListActivity extends BasicActivity
{

    private boolean onReq;

    private boolean isListPullUp;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView list;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notice_list_activity);

        init();
    }

    protected void onResume()
    {
        super.onResume();
    }

    protected void onPause()
    {
        super.onPause();
    }

    private void init()
    {
        try
        {
            getList();
            getSwipeRefreshLayout();
            selectNotice(false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void onClick(View v)
    {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId())
        {
//            case R.id.btnHome:
//                finish();
//                break;
        }
    }

    public SwipeRefreshLayout getSwipeRefreshLayout()
    {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.SwipeRefreshLayout_ColorScheme));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            public void onRefresh() {
                selectNotice(false);
            }
        });
        return swipeRefreshLayout;
    }

    public void getList()
    {
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
                //int firstItemIdx = layoutManager.findFirstCompletelyVisibleItemPosition();
                //int lastItemIdx = layoutManager.findLastCompletelyVisibleItemPosition();
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
                        //requestList(true);
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

        list.setOnTouchListener(new View.OnTouchListener()
        {
            float prevY;

            public boolean onTouch(View v, MotionEvent event)
            {
                //Log.i(TAG, "+ MotionEvent: " + event);
                if (event.getAction() == MotionEvent.ACTION_MOVE)
                {
                    // pull down
                    if (prevY < event.getY())
                    {
                        isListPullUp = false;
                    }
                    // pull up
                    else if (prevY > event.getY())
                    {
                        isListPullUp = true;
                    }
                    prevY = event.getY();
                }
                return false;
            }
        });
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void selectNotice(final boolean reqNextPage)
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
                    if (reqNextPage)
                        showLoadingDialog(null, false);
                    else
                        swipeRefreshLayout.setRefreshing(true);
                }

                protected Bundle doInBackground(Void... arg0)
                {
                    JSONObject payloadJson = null;
                    try
                    {
                        payloadJson = RestRequestor.buildPayload();
                        payloadJson.put("deptCd", Key.getUserInfo().getString("DEPT_CD"));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return RestRequestor.requestPost(API.URL_NOTICE_LIST, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response)
                {
                    onReq = false;
                    if (reqNextPage)
                        dismissLoadingDialog();
                    else
                        swipeRefreshLayout.setRefreshing(false);

                    try
                    {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200"))
                        {
                            ArrayList<Bundle> data = resBody.getParcelableArrayList("data");
                            addListItems(data, reqNextPage);
                        }
                        else
                        {
                            showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        showToast("일시적인 네트워크장애가 발생하였거나 서버 응답이 느립니다.\n 잠시 후 이용해주시기 바랍니다.", false);
                    }
                }
            }.execute();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    synchronized void addListItems(ArrayList<Bundle> items, boolean reqNextPage)
    {
        if (items == null)
            return;

        try
        {
            if (!reqNextPage)
            {
                listItems.clear();
                listItems.addAll(items);
                listAdapter.notifyDataSetChanged();
            }
            else
            {
                listItems.addAll(items);
                listAdapter.notifyItemRangeInserted(items.size(), items.size());
            }
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
            return listItems.get(position).getInt("viewType", TYPE_LIST_ITEM);
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
                View v = View.inflate(getContext(), R.layout.notice_list_item, null);
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

        class ListItemView extends ItemViewHolder
        {
            public ListItemView(View itemView)
            {
                super(itemView);
            }

            public void onBindViewData(Bundle item)
            {
                try
                {
                    TextView titleTxt = (TextView) itemView.findViewById(R.id.txt_title);
                    titleTxt.setText(item.getString("NOTICE_TITLE", ""));

                    itemView.setOnClickListener(new View.OnClickListener()
                    {
                        public void onClick(View v)
                        {
                            Intent intent = new Intent(NoticeListActivity.this, NoticeDetailActivity.class);
                            intent.putExtras(item);
                            startActivity(intent);
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

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

}