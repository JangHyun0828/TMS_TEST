package com.neognp.ytms.carowner.car_alloc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

public class PalletsInputActivity extends BasicActivity {

    public static final int INPUT_PALLETS_COUNT = 100;

    private boolean onReq;
    private Bundle args;

    private int palletsCnt;

    private TextView palletsCntTxt;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pallets_input_activity);

        setTitleBar("상차 팔레트 수 입력", R.drawable.selector_button_back, 0, 0);

        palletsCntTxt = findViewById(R.id.itemsCntTxt);

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

            palletsCnt = Integer.parseInt(args.getString("PALLET_CNT", "0"));
            palletsCntTxt.setText("" + palletsCnt);
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
            case R.id.minusBtn:
                if (palletsCnt - 1 >= 0) {
                    palletsCnt--;
                    palletsCntTxt.setText("" + palletsCnt);
                }
                break;
            case R.id.plusBtn:
                palletsCnt++;
                palletsCntTxt.setText("" + palletsCnt);
                break;
            case R.id.bottomBtn0:
                requestPalletsInput();
                break;
            case R.id.bottomBtn1:
                requestPalletsInput();
                break;
            case R.id.bottomBtn2:
                finish();
                break;
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestPalletsInput() {
        if (onReq)
            return;

        if (args == null)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            String palletCnt = palletsCntTxt.getText().toString();
            if (palletCnt.equals("0")) {
                showToast("팔레트 수를 1개 이상 입력해 주십시요.", true);
                return;
            }

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("dispatchNo", args.getString("DISPATCH_NO"));
                        payloadJson.put("palletCnt", palletCnt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_CAR_PALLETS_INPUT, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            Intent data = new Intent();
                            args.putString("PALLET_CNT", palletCnt);
                            data.putExtras(args);
                            setResult(INPUT_PALLETS_COUNT, data);
                            finish();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}