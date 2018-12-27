package com.neognp.ytms.shipper.account;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
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
import com.neognp.ytms.login.LoginActivity;
import com.neognp.ytms.popup.PwdEditDialog;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;
import com.trevor.library.util.Setting;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

public class ShipperAccountActivity extends BasicActivity {

    private boolean onReq;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shipper_account_activity);

        setTitleBar("개인정보");

        if (Key.getUserInfo() != null) {
            ((TextView) findViewById(R.id.idTxt)).setText(TextUtil.formatPhoneNumber(Key.getUserInfo().getString("USER_ID")));

            ((TextView) findViewById(R.id.nameTxt)).setText(Key.getUserInfo().getString("EMP_NM", "") + " 님");

            ((TextView) findViewById(R.id.companyTxt)).setText(Key.getUserInfo().getString("CLIENT_NM"));
        }

        ((TextView) findViewById(R.id.callCenterTxt)).setText("운송전략팀 연결");
        ((TextView) findViewById(R.id.callCenterPhoneNoTxt)).setText(getString(R.string.delivery_call_center_phone_no));

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
            case R.id.pwdBtn:
                showPwdChangeDialog();
                break;
            case R.id.titleRightBtn1:
                requestLoginActivity();
                break;
            case R.id.callCenterBtn:
                AppUtil.runCallApp(getString(R.string.delivery_call_center_phone_no), true);
                break;
        }
    }

    private void showPwdChangeDialog() {
        PwdEditDialog.show(this, new PwdEditDialog.DialogListener() {
            public void onCancel() {
            }

            public void onConfirm(final String userPw) {
                requestPwdChange(userPw);
            }
        });
    }

    private void requestLoginActivity() {
        Setting.putBoolean(Key.allowAutoLogin, false);

        finish();
        Intent intent = new Intent(getContext(), LoginActivity.class);
        // 앱 새로 실행 | 모든 Activity 삭제
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out);
        startActivity(intent, options.toBundle());
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestPwdChange(String userPw) {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            if (userPw == null || userPw.isEmpty())
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
                        payloadJson.put("userPw", userPw);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_SHIPPER_PWD_CHANGE, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            showToast("비밀번호가 변경됐습니다.", true);
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