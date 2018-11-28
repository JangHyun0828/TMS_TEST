package com.neognp.ytms.template;


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
import com.trevor.library.template.BasicFragment;

import org.json.JSONObject;

public class YTMSTemplateFragment extends BasicFragment implements View.OnClickListener {

    private boolean onReq;
    private Bundle args;

    private View contentView;

    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public void onDetach() {
        super.onDetach();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    //public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    //    // contentView 최초 생성시
    //    if (contentView == null) {
    //        contentView = inflater.inflate(R.layout.ytms_template_fragment, container, false);
    //
    //        //=contentView.findViewById(R.id.);
    //        //=contentView.findViewById(R.id.);
    //
    //        init();
    //    }
    //    // contentView가 이미 생성되어 있는 경우
    //    else {
    //        ((ViewGroup) contentView.getParent()).removeView(contentView);
    //        //Log.i(TAG, "+ onCreateView(): reused contentView");
    //    }
    //
    //    return contentView;
    //}

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.template_fragment, container, false);

        //=contentView.findViewById(R.id.);
        //=contentView.findViewById(R.id.);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    private void init() {
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
                        payloadJson = YTMSRestRequestor.buildPayload();
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