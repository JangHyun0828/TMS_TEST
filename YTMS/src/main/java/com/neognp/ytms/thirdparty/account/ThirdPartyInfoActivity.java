package com.neognp.ytms.thirdparty.account;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.login.LoginActivity;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

public class ThirdPartyInfoActivity extends BasicActivity {

    private boolean onReq;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thirdparty_info_activity);
        setTitleBar("개인정보", R.drawable.selector_button_back, 0, 0);

        ((TextView) findViewById(R.id.callCenterTxt)).setText("운송전략팀 연결");
        ((TextView) findViewById(R.id.callCenterPhoneNoTxt)).setText(getString(R.string.delivery_call_center_phone_no));

        init();
    }

    protected void onResume() { super.onResume(); }

    protected void onPause() {
        super.onPause();
    }

    private void init()
    {
        try
        {
            requestAccountInfo();
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
            case R.id.titleLeftBtn0:
                finish();
                break;
            case R.id.logoutBtn:
                requestLoginActivity();
                break;
            case R.id.callCenterBtn:
                AppUtil.runCallApp(getString(R.string.delivery_call_center_phone_no), true);
                break;
        }
    }

    private void requestLoginActivity()
    {
        finish();
        Intent intent = new Intent(getContext(), LoginActivity.class);
        // 앱 새로 실행 | 모든 Activity 삭제
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out);
        startActivity(intent, options.toBundle());
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestAccountInfo()
    {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
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
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_THIRDPARTY_USER_INFO, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            Bundle data = resBody.getBundle(Key.data);
                            TextView centerText = (TextView) findViewById(R.id.centerText);
                            SpannableString content = new SpannableString(data.getString("TPL_NM", ""));
                            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                            centerText.setText(content);
                            ((TextView) findViewById(R.id.empNumText)).setText(data.getString("EMP_NO", ""));
                            ((TextView) findViewById(R.id.phoneNoText)).setText(data.getString("MOBILE_NO", ""));
                            ((TextView) findViewById(R.id.idTxt)).setText(data.getString("USER_ID", ""));
                            ((TextView) findViewById(R.id.pwdTxt)).setText("********");
                            ((TextView) findViewById(R.id.nameTxt)).setText(data.getString("USER_NM", ""));

                            centerText.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    startActivityForResult(new Intent(ThirdPartyInfoActivity.this, ThirdPartyTplListActivity.class), 123);
                                }
                            });
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

    public void showActivity(Bundle args)
    {
        if (args == null)
            return;

        // Intent intent = new Intent(this, Activity.class);
        // intent.putExtra(LibKey.args, args);
        // startActivityForResult(intent, REQUEST_);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 123)
        {
            requestAccountInfo();
        }
    }
}