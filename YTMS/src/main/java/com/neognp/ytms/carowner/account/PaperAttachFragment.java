package com.neognp.ytms.carowner.account;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.http.RestRequestor;
import com.trevor.library.template.BasicFragment;

import org.json.JSONObject;

public class PaperAttachFragment extends BasicFragment implements View.OnClickListener {

    private boolean onReq;
    private Bundle args;

    private View contentView;

    private AccountEditActivity host;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        host = (AccountEditActivity) getActivity();

        contentView = inflater.inflate(R.layout.paper_attach_fragment, container, false);

        //=(EditText) contentView.findViewById(R.id.);
        //=(TextView) contentView.findViewById(R.id.);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    void init() {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

        switch (v.getId()) {
            //case R.id.:
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void request() {
        if (onReq)
            return;

        //if(args == null)
        //    return;

        try {
            if (Key.getUserInfo() == null)
                return;

            //final String  = ((TextView) findViewById(R.id.)).getText().toString().trim();
            //if (.isEmpty()) {
            //    showToast("입력하세요.", true);
            //    return;
            //}

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = RestRequestor.buildPayload();
                        //payloadJson.put("", );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {

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

    public void showActivity(Bundle item) {
        if (item == null)
            return;

        // Intent intent = new Intent(getActivity(), Activity.class);
        // intent.putExtra(LibKey.Item, item);
        // startActivityForResult(intent, REQUEST_CODE_);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // if (requestCode == REQUEST_CODE_) {
        // if (resultCode == Activity.RESULT_CANCELED) {
        //
        // }
        // }
    }

}