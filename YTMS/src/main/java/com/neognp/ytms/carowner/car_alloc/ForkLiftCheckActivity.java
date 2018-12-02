package com.neognp.ytms.carowner.car_alloc;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.app.LibApp;
import com.trevor.library.app.LibKey;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.DeviceUtil;
import com.trevor.library.util.Setting;
import com.trevor.library.web.MyWebChromeClient;
import com.trevor.library.web.MyWebViewClient;

import org.json.JSONObject;

public class ForkLiftCheckActivity extends BasicActivity {

    private boolean onReq;
    private Bundle args;

    private WebView contentWeb;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forklift_check_activity);

        setTitleBar("지게차 하차 여부 체크", 0, 0, R.drawable.selector_button_close);

        contentWeb = (WebView) findViewById(R.id.contentWeb);
        contentWeb.setWebViewClient(new MyWebViewClient(this)); // 앱에서 url 직접 처리
        contentWeb.setWebChromeClient(new MyWebChromeClient(this));
        //contentWeb.clearCache(true); // cache 사용 금지
        contentWeb.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // cache 사용
        contentWeb.getSettings().setSupportZoom(true);
        contentWeb.getSettings().setBuiltInZoomControls(true);
        contentWeb.getSettings().setDisplayZoomControls(false);
        contentWeb.getSettings().setUseWideViewPort(true);
        contentWeb.getSettings().setLoadWithOverviewMode(true);
        contentWeb.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        contentWeb.setScrollbarFadingEnabled(false);
        contentWeb.getSettings().setSupportMultipleWindows(true);
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

            String url = "http://" + Setting.getString("ip") + ":" + Setting.getInt("port") + "/" + API.URL_WEB_FORK_LIFT_CHECK;
            // TEST
            //String url = "file:///android_asset/html/fork_lift_check.html";

            String postData = "";
            String uuid = DeviceUtil.getUuid();
            if (uuid.endsWith("810d"))
                uuid = "ffffffff-0000-0000-0000-0000aaaaaaaa";
            postData += "uuid=" + uuid;
            postData += "&" + "phone_no=" + DeviceUtil.getPhoneNumber();
            postData += "&" + "manufacturer=" + Build.MANUFACTURER;
            postData += "&" + "network_operator=" + DeviceUtil.getOperatorName();
            postData += "&" + "model=" + Build.MODEL;
            postData += "&" + "os_type=Android";
            postData += "&" + "os_version=" + Build.VERSION.RELEASE;
            postData += "&" + "app_version=" + DeviceUtil.getAppVersionName();
            contentWeb.postUrl(url, postData.getBytes());

            if (MyApp.onDebug)
                Log.i(TAG, "+ init():\n" + url + "?" + postData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (contentWeb.canGoBack()) {
                        contentWeb.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }

        return super.onKeyDown(keyCode, event);
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