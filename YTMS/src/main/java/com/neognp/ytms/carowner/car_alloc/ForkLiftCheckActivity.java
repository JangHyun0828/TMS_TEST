package com.neognp.ytms.carowner.car_alloc;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

public class ForkLiftCheckActivity extends BasicActivity {

    private boolean onReq;
    private Bundle args;

    // private TextView
    // private EditText
    // private View

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forklift_check_activity);

        setTitleBar("지게차 하차 여부 체크", 0, 0, R.drawable.selector_button_close);

        //= findViewById(R.id.);

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


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                break;
            case R.id.titleRightBtn1:
                finish();
                break;
            case R.id.bottomBtn0:
                requestForkLiftCheck("Y");
                break;
            case R.id.bottomBtn1:
                requestForkLiftCheck("N");
                break;
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestForkLiftCheck(String checkYn) {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            if (args == null)
                return;

            if (checkYn == null)
                return;

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("carCd", args.getString("CAR_CD"));
                        payloadJson.put("dispatchNo", args.getString("DISPATCH_NO"));
                        payloadJson.put("checkYn", checkYn);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_CAR_FORK_LIFT_CHECK, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            startForkLiftAlarmActivity();
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

    private void startForkLiftAlarmActivity() {
        if (args == null)
            return;

        ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);
        Intent intent = new Intent(this, ForkLiftAlarmActivity.class);
        intent.putExtras((Bundle) args.clone());
        startActivity(intent, options.toBundle());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}