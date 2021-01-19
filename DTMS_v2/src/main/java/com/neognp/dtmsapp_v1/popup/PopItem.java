package com.neognp.dtmsapp_v1.popup;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.common.GoogleApiAvailability;
import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.cust.OrderNewActivity;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class PopItem extends BasicActivity {

    private boolean onReq;
    private Bundle args;

    private SwipeRefreshLayout swipeRefreshLayout;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private RecyclerView list;
    private ListAdapter listAdapter;
    private boolean isListPullUp;
    private EditText itemNm;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_item);

        itemNm = (EditText) findViewById(R.id.edt_item_nm);

        init();
    }

    private void init()
    {
        try
        {
            args = getIntent().getExtras();
            if(args == null)
            {
                showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
                return;
            }

            //GoogleHandler Play 서비스 호환 여부 체크
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);

            getSwipeRefreshLayout();
            getList();
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //Recycler View를 swipe시 Refresh
    public SwipeRefreshLayout getSwipeRefreshLayout()
    {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.SwipeRefreshLayout_ColorScheme));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                selectItem();
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
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // CAUTION 아이템 높이가 RecycleView 높이보다 큰 경우, 아래 메서드 사용시 index가 모두 -1 로 반환되므로 사용 금지
                int firstItemIdx = layoutManager.findFirstVisibleItemPosition();
                int lastItemIdx = layoutManager.findLastVisibleItemPosition();

                Log.e(TAG, "+ onScrollStateChanged(): newState=" + newState + " / isListPullUp=" + isListPullUp + " / firstItemIdx=" + firstItemIdx + " / lastItemIdx=" + lastItemIdx);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!isListPullUp && firstItemIdx == 0) {
                        Log.i(TAG, "+ onScrollStateChanged(): request refresh !");
                    } else if (isListPullUp && lastItemIdx == listAdapter.getItemCount() - 1) {
                        Log.i(TAG, "+ onScrollStateChanged(): request next list !");
                    }
                }

                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
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
        super.onResume();
    }

    protected void onPause()
    {
        super.onPause();
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.btn_search:
                if(itemNm.getText().toString() == null || itemNm.getText().toString().equals(""))
                {
                    showToast("제품명을 입력해주세요.", false);
                    return;
                }
                else
                {
                    selectItem();
                }
                break;

            case R.id.btn_cancel:
                args.putString("ITEM_NM", "");
                args.putString("ITEM_CD", "");

                Intent intent = new Intent(PopItem.this, OrderNewActivity.class);
                intent.putExtras((Bundle) args.clone());
                startActivity(intent);
                finish();
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private synchronized void selectItem()
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
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0)
                {
                    JSONObject payloadJson = null;
                    try
                    {
                        payloadJson = RestRequestor.buildPayload();
                        payloadJson.put("deptCd", args.getString("DEPT_CD"));
                        payloadJson.put("custCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("itemNm", itemNm.getText().toString());
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    return RestRequestor.requestPost(API.URL_POP_ITEM, false, payloadJson, true, false);
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
                View v = View.inflate(getContext(), R.layout.pop_item, null);
                v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                holder = new ListAdapter.ListItemView(v);
            }

            return holder;
        }

        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position)
        {
            ((ListAdapter.ItemViewHolder) holder).onBindViewData(listItems.get(position));
        }

        abstract class ItemViewHolder extends RecyclerView.ViewHolder
        {
            public ItemViewHolder(View itemView)
            {
                super(itemView);
            }

            public abstract void onBindViewData(final Bundle item);
        }

        class ListItemView extends PopItem.ListAdapter.ItemViewHolder
        {
            private ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);

            public ListItemView(View itemView)
            {
                super(itemView);
            }

            public void onBindViewData(Bundle item)
            {
                try
                {
                    String ITEM_NM = item.getString("ITEM_NM", "");
                    String ITEM_CD = item.getString("ITEM_CD", "");
                    String ORDER_NO = item.getString("ORDER_NO", "");
                    String ITEM_NM_INFO = item.getString("ITEM_NM_INFO", "");

                    TextView dataTxt0 = ((TextView) itemView.findViewById(R.id.txt_data0));
                    dataTxt0.setText(ORDER_NO);

                    TextView dataTxt1 = ((TextView) itemView.findViewById(R.id.txt_data1));
                    dataTxt1.setText(ITEM_NM_INFO);

                    ConstraintLayout itemLayout = (ConstraintLayout) itemView.findViewById(R.id.layout_item);
                    itemLayout.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            args.putString("ITEM_NM", ITEM_NM);
                            args.putString("ITEM_CD", ITEM_CD);
                            args.putString("ORDER_NO", ORDER_NO);
                            args.putString("ITEM_NM_INFO", ITEM_NM_INFO);

                            Intent intent = new Intent(PopItem.this, OrderNewActivity.class);
                            intent.putExtras((Bundle) args.clone());
                            startActivity(intent);
                            finish();
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

    @Override
    public void onBackPressed()
    {
        args.putString("ITEM_NM", "");
        args.putString("ITEM_CD", "");

        Intent intent = new Intent(PopItem.this, OrderNewActivity.class);
        intent.putExtras((Bundle) args.clone());
        startActivity(intent);
        finish();
        super.onBackPressed();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        try
        {

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}