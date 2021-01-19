package com.neognp.ytms.popup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.delivery.pallets.PalletsReceiptHistoryActivity;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.template.BasicDialog;

import org.json.JSONObject;

import java.util.ArrayList;

@SuppressLint ("ValidFragment")
public class ShipperSearchDialog extends BasicDialog implements View.OnClickListener, DialogInterface.OnCancelListener {

    private boolean onReq;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private View contentView;
    private EditText shipperNameEdit;
    private RecyclerView list;

    private DialogListener listener;

    public ShipperSearchDialog(DialogListener listener) {
        setCancelable(true);
        this.listener = listener;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.shipper_search_dialog, null, false);

        // 팝업 및 팝업 밖 터치 시 닫기
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        shipperNameEdit = contentView.findViewById(R.id.shipperNameEdit);

        contentView.findViewById(R.id.keywordClearBtn).setOnClickListener(this);
        contentView.findViewById(R.id.searchBtn).setOnClickListener(this);

        list = contentView.findViewById(R.id.list);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);
        listAdapter = new ListAdapter();
        list.setAdapter(listAdapter);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.keywordClearBtn:
                shipperNameEdit.setText(null);
                break;
            case R.id.searchBtn:
                requestList();
                break;
        }
    }

    public static ShipperSearchDialog show(Activity activity, DialogListener listener) {
        ShipperSearchDialog dialog = new ShipperSearchDialog(listener);
        return (ShipperSearchDialog) dialog.show((AppCompatActivity) activity);
    }

    public interface DialogListener {

        void onSelect(Bundle shipper);

    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestList() {
        if (!isAdded())
            return;

        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            String custNm = shipperNameEdit.getText().toString().trim();
            if (custNm.isEmpty()) {
                ((BasicActivity) getActivity()).showToast("거래처명을 입력해 주십시요.", true);
                return;
            }

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    ((BasicActivity) getActivity()).showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("custNm", custNm);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_SHIPPER_LIST, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    ((BasicActivity) getActivity()).dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            ArrayList<Bundle> data = resBody.getParcelableArrayList("data");
                            addListItems(data);
                        } else {
                            ((BasicActivity) getActivity()).showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ((BasicActivity) getActivity()).showToast(e.getMessage(), false);
                    }
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void addListItems(ArrayList<Bundle> items) {
        try {
            listItems.clear();
            listItems.addAll(items);
            listAdapter.notifyItemRangeInserted(items.size(), items.size());
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
                View v = View.inflate(getContext(), R.layout.shipper_search_dialog_list_item, null);
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
                    String CUST_CD = item.getString("CUST_CD");

                    // 고객사 코드
                    ((TextView) itemView.findViewById(R.id.dataTxt0)).setText(CUST_CD);

                    // 고객사 명
                    ((TextView) itemView.findViewById(R.id.dataTxt1)).setText(item.getString("CUST_NM"));

                    itemView.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (listener != null)
                                listener.onSelect((Bundle) item.clone());
                            hide();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
