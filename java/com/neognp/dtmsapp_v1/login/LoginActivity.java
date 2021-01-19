package com.neognp.dtmsapp_v1.login;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.neognp.dtmsapp_v1.popup.ConfirmCancelDialog;
import com.neognp.dtmsapp_v1.popup.LocationInfoDialog;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.ArrayList;

public class LoginActivity extends BasicActivity {

    private boolean onReq, autocheck;
    private EditText idEdit, pwdEdit;
    private String userId, userdPw, id, pw, getId;
    private ArrayList<String> phoneNoList;
    private String[] arr;
    //private long lastTimeBackPressed;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int REQUEST_READ_CONTACTS = 1000;

    private CheckBox chkSaveId;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        chkSaveId = (CheckBox) findViewById(R.id.chk_saveId);

        //키패드 완료버튼 클릭시 자동으로 로그인버튼 클릭
        idEdit = findViewById(R.id.edt_id);

        pwdEdit = findViewById(R.id.edt_pwd);

        pwdEdit.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    findViewById(R.id.btn_login).performClick();
                }
                return false;
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS))
            {
                showToast("앱 권한 확인", false);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
            }
        }

        TelephonyManager telManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
//        {
//            return;
//        }
        try
        {
            String userId = telManager.getLine1Number();

            if(!userId.equals("") || userId != null)
            {
                if(userId.startsWith("+82"))
                {
                    userId = userId.replace("+82", "0");
                }
                idEdit.setText(userId);

                //                      idEdit.setEnabled(false);
                idEdit.setEnabled(true);
//                      idEdit.setBackgroundColor(Color.LTGRAY);
//                      idEdit.setBackgroundColor(Color.WHITE);

//                     if(phoneNoList.contains(userId))
//                     {
//                         idEdit.setEnabled(true);
//                         idEdit.setBackgroundColor(Color.WHITE);
//                     }
            }

            else
            {
                idEdit.setEnabled(true);
                idEdit.setBackgroundColor(Color.WHITE);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //해당 번호를 예외적으로 수정가능하게 변경할 경우
        //phoneNoList = new ArrayList<String>(Arrays.asList("07000000000"));

        SharedPreferences sp = getSharedPreferences("DTMS", 0);
        autocheck = sp.getBoolean("chkLogin", false);
        if (autocheck == true) {
            autologin();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                }
                else
                {

                }
                return;
        }
    }

    public void autologin()
    {
        SharedPreferences sp = getSharedPreferences("DTMS", 0);
        id = sp.getString("id", "");
        pw = sp.getString("pw", "");

        if(!id.equalsIgnoreCase("") && !pw.equalsIgnoreCase(""))
        {
            idEdit.setText(id);
            pwdEdit.setText(pw);
            chkSaveId.setChecked(true);
            findViewById(R.id.btn_login).performClick();
        }
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
            case R.id.btn_login:
                requestLogin();
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private synchronized void requestLogin()
    {
        if (onReq)
            return;

        try {
            final String userId = ((TextView) findViewById(R.id.edt_id)).getText().toString().replaceAll("-", "").trim();
            if (userId.isEmpty())
            {
                showToast("ID를 입력하세요.", true);
                return;
            }

            final String userPw = ((TextView) findViewById(R.id.edt_pwd)).getText().toString().trim();
            if (userPw.isEmpty())
            {
                showToast("패스워드를 입력하세요.", true);
                return;
            }

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
                        payloadJson.put("userId", userId);
                        payloadJson.put("userPw", userPw);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return RestRequestor.requestPost(API.URL_LOGIN, false, payloadJson, true, true);
                }

                protected void onPostExecute(Bundle response)
                {
                    onReq = false;
                    dismissLoadingDialog();

                    try
                    {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);
                        SharedPreferences.Editor spe = getSharedPreferences("DTMS", 0).edit();
                        SharedPreferences sp = getSharedPreferences("DTMS", 0);

                        if (result_code.equals("200"))
                        {
                            JSONObject resJson = new JSONObject(response.getString(Key.resStr));
                            JSONObject data = resJson.optJSONObject(Key.data);


//                            String app_version = resBody.getString("app_version", "");
//                            String local_version = DeviceUtil.getAppVersionName();


//                            Log.d("app", app_version);
//                            Log.d("local", local_version);

//                            if(app_version.isEmpty() || !app_version.equalsIgnoreCase(local_version))
//                            {
//                                showToast("최신 버전 앱을 새로 설치 후 실행해 주십시요.", true);
//                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.neognp.com/daiso/update_car.html"));
//                                startActivity(intent);
//
//                                finish();
//                                return;
//                            }

                            if (data != null)
                            {
                                data.remove("USER_PW");
                                Key.saveUserInfo(data);

                                if(chkSaveId.isChecked())
                                {
                                    spe.putString("id", idEdit.getText().toString());
                                    spe.putString("pw", pwdEdit.getText().toString());
                                    spe.putBoolean("chkLogin", true);
                                }
                                else
                                {
                                    spe.putString("id", "");
                                    spe.putString("pw", "");
                                    spe.putBoolean("chkLogin", false);
                                }
                                spe.commit();

                                if(sp.getString("privacy", "N").equals("Y"))
                                {
                                    loginSuccess();
                                }
                                else
                                {
                                    LocationInfoDialog.show(LoginActivity.this, "개인정보 활용약관", true, new LocationInfoDialog.DialogListener()
                                    {
                                        public void onCancel()
                                        {
                                            spe.putString("privacy", "N");
                                            spe.commit();
                                        }
                                        public void onConfirm()
                                        {
                                            spe.putString("privacy", "Y");
                                            spe.commit();

                                            loginSuccess();
                                        }
                                    });
                                }

                            }
                        }
                        else
                        {
                            showToast("ID 또는 비밀번호를 다시 확인하세요.\n등록되지 않은 ID이거나, ID 또는 비밀번호를 잘못 입력하였습니다.", true);
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

//    public boolean checkLocationServicesStatus()
//    {
//        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//
//        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//    }

    public void loginSuccess()
    {
        String USER_GB = Key.getUserInfo().getString("USER_GB", "");

        //위치 접근 권한 체크를 위한 변수
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if(USER_GB.equals("CAR"))
        {
            if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED)
            {
                //startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GPS_ENABLE_REQUEST_CODE);
                showMainActivity();
            }
            else
            {
                ConfirmCancelDialog.show(this, "이 앱은 위치정보를 사용합니다. 정보제공에 동의하시겠습니까?", "동의안함", "동의함", false, new ConfirmCancelDialog.DialogListener() {
                    public void onCancel()
                    {
                        finish();
                    }

                    public void onConfirm()
                    {
//                        if (!checkLocationServicesStatus())
//                        {
//                            showDialogForLocationServiceSetting();
//                        }
//                        else
//                        {
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GPS_ENABLE_REQUEST_CODE);
                            showMainActivity();
//                        }
                    }
                });
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            }
        }
        else if(USER_GB.equals("CUST"))
        {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            showMainActivity();
        }

        else if(USER_GB.equals("ADMIN"))
        {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            showMainActivity();
        }

        else if(USER_GB.equals("YCAR"))
        {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            showMainActivity();
        }
    }

    private void showDialogForLocationServiceSetting()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" + "위치 설정을 수정하시겠습니까?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                finish();
            }
        });
        builder.create().show();
    }


    public void showMainActivity()
    {
        String USER_GB = Key.getUserInfo().getString("USER_GB", "");
        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_from_right, R.anim.fade_out);
        Intent intent = null;

        if(USER_GB.equals(""))
        {
             return;
         }

        //기사
        if(USER_GB.equals("CAR"))
        {
            intent = new Intent(getContext(), com.neognp.dtmsapp_v1.car.MainActivity.class);
        }

        //고객
        else if(USER_GB.equals("CUST"))
        {
            intent = new Intent(getContext(), com.neognp.dtmsapp_v1.cust.MainActivity.class);
        }

        //관리자
        else if(USER_GB.equals("ADMIN"))
        {
            intent = new Intent(getContext(), com.neognp.dtmsapp_v1.admin.MainActivity.class);
        }

        //관리자
        else if(USER_GB.equals("YCAR"))
        {
            intent = new Intent(getContext(), com.neognp.dtmsapp_v1.car.YDispatchListActivity.class);
        }

        // 앱 새로 실행 | 모든 Activity 삭제
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent, options.toBundle());
        finish();
    }

    public void onBackPressed() {
        ConfirmCancelDialog.show(this, "앱을 종료하시겠습니까?", "취소", "확인", true, new ConfirmCancelDialog.DialogListener()
        {
            public void onCancel()
            {
                return;
            }

            public void onConfirm()
            {
                finish();
            }
        });
//        if (System.currentTimeMillis() - lastTimeBackPressed < 1500)
//        {
//            finish();
//            return;
//        }
//        showToast("\"뒤로가기\"버튼을 한번 더 누르시면\n앱이 종료됩니다.", false);
//        lastTimeBackPressed = System.currentTimeMillis();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }
}