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
import com.neognp.dtmsapp_v1.car.PalletInActivity;
import com.neognp.dtmsapp_v1.car.PalletOutActivity;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class PopPallet extends BasicActivity {

    public static final int RESULT_INPUT_PALLETS_COUNT = 100;

    private boolean onReq;
    private Bundle args;

    private SwipeRefreshLayout swipeRefreshLayout;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private RecyclerView list;
    private ListAdapter listAdapter;
    private boolean isListPullUp;
    private EditText clientNm;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_pallet);

        init();
    }

    private void init()
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

        selectPallet();
    }

    //Recycler View를 swipe시 Refresh
    public SwipeRefreshLayout getSwipeRefreshLayout() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.SwipeRefreshLayout_ColorScheme));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                selectPallet();
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
            public void onScrollStateChanged(RecyclerView recyclerView, int newState)
            {
                super.onScrollStateChanged(recyclerView, newState);

                // CAUTION 아이템 높이가 RecycleView 높이보다 큰 경우, 아래 메서드 사용시 index가 모두 -1 로 반환되므로 사용 금지
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

    public void onClick(View v)
    {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId())
        {
            case R.id.btn_cancel:
                args.putString("PALLET_CD", "");
                args.putString("PALLET_NM", "");
                args.putString("ITEM_TYPE", "");

                if(args.getString("ACTIVITY_GB").equals("PALLET"))
                {
                    Intent intent = new Intent(PopPallet.this, PalletInActivity.class);
                    intent.putExtras((Bundle) args.clone());
                    startActivity(intent);
                    finish();
                }
                else if(args.getString("ACTIVITY_GB").equals("PALLET2"))
                {
                    Intent intent = new Intent(PopPallet.this, PalletOutActivity.class);
                    intent.putExtras((Bundle) args.clone());
                    startActivity(intent);
                    finish();
                }
                break;

        }
    }

    @SuppressLint("StaticFieldLeak")
    private synchronized void selectPallet()
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
                        payloadJson.put("deptCd", Key.getUserInfo().getString("DEPT_CD"));
                        payloadJson.put("itemType", args.getString("ITEM_TYPE", ""));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    return RestRequestor.requestPost(API.URL_POP_PALLET, false, payloadJson, true, false);
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
                View v = View.inflate(getContext(), R.layout.pop_pallet_item, null);
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

        class ListItemView extends PopPallet.ListAdapter.ItemViewHolder
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
                    String PALLET_CD = item.getString("PALLET_CD", "");
                    String PALLET_NM = item.getString("PALLET_NM", "");
                    String ITEM_TYPE = item.getString("ITEM_TYPE", "");
                    String ITEM_TYPE_NM = item.getString("ITEM_TYPE_NM", "");

                    TextView dataTxt0 = ((TextView) itemView.findViewById(R.id.txt_data0));
                    dataTxt0.setText(PALLET_CD);

                    TextView dataTxt1 = ((TextView) itemView.findViewById(R.id.txt_data1));
                    dataTxt1.setText(PALLET_NM);

                    TextView dataTxt2 = ((TextView) itemView.findViewById(R.id.txt_data2));
                    dataTxt2.setText(ITEM_TYPE_NM);

                    ConstraintLayout palletLayout = (ConstraintLayout) itemView.findViewById(R.id.layout_pallet);
                    palletLayout.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            args.putString("PALLET_CD", PALLET_CD);
                            args.putString("PALLET_NM", PALLET_NM);
                            args.putString("ITEM_TYPE", ITEM_TYPE);

                            if(args.getString("ACTIVITY_GB").equals("PALLET"))
                            {
                                Intent intent = new Intent(PopPallet.this, PalletInActivity.class);
                                intent.putExtras((Bundle) args.clone());
                                startActivity(intent);
                                finish();
                            }
                            else if(args.getString("ACTIVITY_GB").equals("PALLET2"))
                            {
                                Intent intent = new Intent(PopPallet.this, PalletOutActivity.class);
                                intent.putExtras((Bundle) args.clone());
                                startActivity(intent);
                                finish();
                            }
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
        args.putString("PALLET_CD", "");
        args.putString("PALLET_NM", "");
        args.putString("ITEM_TYPE", "");

        if(args.getString("ACTIVITY_GB").equals("PALLET"))
        {
            Intent intent = new Intent(PopPallet.this, PalletInActivity.class);
            intent.putExtras((Bundle) args.clone());
            startActivity(intent);
            finish();
            super.onBackPressed();
        }
        else if(args.getString("ACTIVITY_GB").equals("PALLET2"))
        {
            Intent intent = new Intent(PopPallet.this, PalletOutActivity.class);
            intent.putExtras((Bundle) args.clone());
            startActivity(intent);
            finish();
            super.onBackPressed();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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