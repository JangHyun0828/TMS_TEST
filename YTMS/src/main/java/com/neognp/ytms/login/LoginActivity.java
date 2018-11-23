package com.neognp.ytms.login;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.neognp.ytms.carowner.car_alloc.CarAllocHistoryActivity;
import com.neognp.ytms.carowner.main.CarOwnerMainActivity;
import com.neognp.ytms.delivery.main.DeliveryMainActivity;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.popup.LocationInfoDialog;
import com.neognp.ytms.shipper.main.ShipperMainActivity;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;
import com.trevor.library.util.DeviceUtil;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

public class LoginActivity extends BasicActivity {

    private boolean onReq;

    private String targetClassName;

    private EditText idEdit, pwdEdit;
    private CheckBox idSaveCheck, autoLoginCheck;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        idEdit = findViewById(R.id.idEdit);
        idEdit.setText(DeviceUtil.getPhoneNumber());

        pwdEdit = findViewById(R.id.pwdEdit);
        // 키패드 '완료' 버튼 클릭 시, ' 로그인' 버튼 자동 클릭
        pwdEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    findViewById(R.id.loginBtn).performClick();
                }
                return false;
            }
        });

        idSaveCheck = findViewById(R.id.idSaveCheck);
        autoLoginCheck = findViewById(R.id.autoLoginCheck);

        ((TextView) findViewById(R.id.callCenterTxt)).setText("운송전략팀 연결");
        ((TextView) findViewById(R.id.callCenterPhoneNoTxt)).setText(getString(R.string.delivery_call_center_phone_no));

        init();

        if (MyApp.onTest) {
            findViewById(R.id.testAccountGroup).setVisibility(View.VISIBLE);

            TextView testAccountTxt0 = ((TextView) findViewById(R.id.testAccountTxt0));
            testAccountTxt0.setVisibility(View.VISIBLE);
            SpannableString content = new SpannableString(testAccountTxt0.getText());
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            testAccountTxt0.setText(content);
            testAccountTxt0.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    idEdit.setText("01011111111");
                    pwdEdit.setText("1");
                }
            });

            TextView testAccountTxt1 = ((TextView) findViewById(R.id.testAccountTxt1));
            testAccountTxt1.setVisibility(View.VISIBLE);
            content = new SpannableString(testAccountTxt1.getText());
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            testAccountTxt1.setText(content);
            testAccountTxt1.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    idEdit.setText("01012345678");
                    pwdEdit.setText("1");
                }
            });

            TextView testAccountTxt2 = ((TextView) findViewById(R.id.testAccountTxt2));
            testAccountTxt2.setVisibility(View.VISIBLE);
            content = new SpannableString(testAccountTxt2.getText());
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            testAccountTxt2.setText(content);
            testAccountTxt2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    idEdit.setText("01099999999");
                    pwdEdit.setText("1");
                }
            });

            TextView testAccountTxt3 = ((TextView) findViewById(R.id.testAccountTxt3));
            testAccountTxt3.setVisibility(View.VISIBLE);
            content = new SpannableString(testAccountTxt3.getText());
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            testAccountTxt3.setText(content);
            testAccountTxt3.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    idEdit.setText("01088888888");
                    pwdEdit.setText("1");
                }
            });
        } else {
            findViewById(R.id.testAccountGroup).setVisibility(View.GONE);
        }
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    private void init() {
        try {
            //targetClassName = getIntent().getStringExtra(Key.className);
            //Log.i(TAG, "+ init(): targetClassName=" + targetClassName);
            //
            //if (targetClassName.equals(ShipperMainActivity.class.getSimpleName())) {
            //    idEdit.setEnabled(true);
            //    idSaveCheck.setVisibility(View.VISIBLE);
            //    autoLoginCheck.setVisibility(View.VISIBLE);
            //} else if (targetClassName.equals(CarOwnerMainActivity.class.getSimpleName())) {
            //    idEdit.setEnabled(true);
            //    // TODO 푸시 수신>로그인 바로 이동>permission 얻기 전 DeviceUtil.getUuid() 호출시 SecurityException 발생
            //    idEdit.setText(TextUtil.formatPhoneNumber(DeviceUtil.getPhoneNumber()));
            //    idEdit.setEnabled(false);
            //    idSaveCheck.setVisibility(View.INVISIBLE);
            //    autoLoginCheck.setVisibility(View.INVISIBLE);
            //}else if (targetClassName.equals(DeliveryMainActivity.class.getSimpleName())) {
            //    idEdit.setEnabled(true);
            //    idSaveCheck.setVisibility(View.VISIBLE);
            //    autoLoginCheck.setVisibility(View.VISIBLE);
            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.loginBtn:
                requestLogin();
                break;
            case R.id.bottomCenterInfoView:
                AppUtil.runCallApp(getString(R.string.delivery_call_center_phone_no), true);
                break;
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestLogin() {
        if (onReq)
            return;

        try {
            final String userId = ((TextView) findViewById(R.id.idEdit)).getText().toString().trim();
            if (userId.isEmpty()) {
                showToast("ID를 입력하세요.", true);
                return;
            }

            final String userPw = ((TextView) findViewById(R.id.pwdEdit)).getText().toString().trim();
            if (userPw.isEmpty()) {
                showToast("비밀번호를 입력하세요.", true);
                return;
            }

            // TEST
            final String userDataInit = Boolean.toString(((CheckBox) findViewById(R.id.userDatInitCheck)).isChecked());

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userId", userId);
                        payloadJson.put("userPw", userPw);
                        if (MyApp.onTest)
                            payloadJson.put("userDataInit", userDataInit);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_LOGIN, false, payloadJson, true, true);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            JSONObject resJson = new JSONObject(response.getString(Key.resStr));
                            JSONObject data = resJson.optJSONObject(Key.data);
                            if (data != null) {
                                // 비밀번호 길이 파악을 위해 별표로 변경 저장
                                StringBuilder builder = new StringBuilder(data.optString("USER_PW", ""));
                                builder.replace(0, builder.length(), "*");
                                data.put("USER_PW_HIDDEN", builder.toString());

                                data.remove("USER_PW"); // 비밀번호 삭제

                                Key.saveUserInfo(data);

                                showCategoryMainActivity(data.getString("AUTH_CD"));
                            }
                        } else {
                            //showToast(result_msg + "(result_code:" + result_code + ")", true);
                            showToast("ID 또는 비밀번호를 다시 확인하세요.\n등록되지 않은 ID이거나, ID 또는 비밀번호를 잘못 입력하였습니다.", true);
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

    public void showCategoryMainActivity(String AUTH_CD) {
        if (AUTH_CD == null)
            return;

        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_from_right, R.anim.fade_out);
        Intent intent = null;
        if (AUTH_CD.equals("CST")) {
            intent = new Intent(getContext(), ShipperMainActivity.class);
        } else if (AUTH_CD.equals("CAR")) {
            intent = new Intent(getContext(), CarOwnerMainActivity.class);
        } else if (AUTH_CD.equals("DC")) {
            intent = new Intent(getContext(), DeliveryMainActivity.class);
        } else if (AUTH_CD.equals("TPL")) {
            //intent = new Intent(getContext(), .class);
            showToast("준비중...", true);
            return;
        }

        // 앱 새로 실행 | 모든 Activity 삭제
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent, options.toBundle());

        finish();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}