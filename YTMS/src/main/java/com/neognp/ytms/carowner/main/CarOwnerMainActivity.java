package com.neognp.ytms.carowner.main;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.carowner.account.CarOwnerAccountActivity;
import com.neognp.ytms.carowner.car_alloc.CarAllocHistoryActivity;
import com.neognp.ytms.carowner.car_alloc.ForkLiftCheckActivity;
import com.neognp.ytms.carowner.charge.FreightChargeHistoryActivity;
import com.neognp.ytms.fcm.MyFirebaseMessagingService;
import com.neognp.ytms.gps.GpsTrackingService;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.login.LoginActivity;
import com.neognp.ytms.notice.NoticeListActivity;
import com.neognp.ytms.popup.ConfirmCancelDialog;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;
import com.trevor.library.util.DeviceUtil;
import com.trevor.library.util.Setting;

import org.json.JSONObject;

public class CarOwnerMainActivity extends BasicActivity {

    private boolean onNewIntent;

    private ImageView locationImg;

    private TextView carAllocBadgeCntTxt;
    private TextView noticeBadgeCntTxt;

    private GpsTrackingService mGpsTrackingService;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_owner_main_activity);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Key.ACTION_GPS_SERVICE_START);
        filter.addAction(Key.ACTION_GPS_SERVICE_STOP);
        filter.addAction(Key.ACTION_GPS_SERVICE_STATE);
        filter.addAction(Key.ACTION_GPS_SERVICE_LOCATION_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(gpsServiceReceiver, filter);

        //Intent serviceIntent = new Intent(this, GpsTrackingService.class);

        /** 홈 화면>모든 앱 종료시 Service 유지 **/
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        //    startService(serviceIntent);
        //}
        //// Oreo
        //else {
        //    startForegroundService(serviceIntent);
        //}

        /** 홈 화면>모든 앱 종료시 Service 종료 **/
        //bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE);

        setTitleBar(R.string.app_name, 0, 0, 0);

        if (Key.getUserInfo() != null)
            ((TextView) findViewById(R.id.userNameTxt)).setText(Key.getUserInfo().getString("USER_NM", ""));

        locationImg = findViewById(R.id.locationImg);

        carAllocBadgeCntTxt = findViewById(R.id.carAllocBadgeCntTxt);
        noticeBadgeCntTxt = findViewById(R.id.noticeBadgeCntTxt);

        init();
    }

    protected void onResume() {
        super.onResume();

        requestCarAllocCount();
        requestNewNoticeCount();
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsServiceReceiver);

        /* 자동로그인이 아닌 경우, GPS 서비스 종료 */
        if (!Setting.getBoolean(Key.allowAutoLogin)) {
            //Intent serviceIntent = new Intent(CarOwnerMainActivity.this, GpsTrackingService.class);
            //stopService(serviceIntent);

            //unbindService(mServiceConnection);
            //mGpsTrackingService = null;
        }
    }

    /* Service 를 시작시킨 Activity 종료 시, Service 도 함께 종료 */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        String TAG = ServiceConnection.class.getSimpleName();

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mGpsTrackingService = ((GpsTrackingService.LocalBinder) service).getService();

            //if (!mGpsTrackingService.initialize()) {
            //    Log.e(TAG, "+ onServiceConnected: failed");
            //    finish();
            //} else {
            //    Log.e(TAG, "+ onServiceConnected(): succeed");
            //}

            Log.e(TAG, "+ onServiceConnected(): succeed");
        }

        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "+ onServiceDisconnected(): ");
            mGpsTrackingService = null;
        }
    };

    private final BroadcastReceiver gpsServiceReceiver = new BroadcastReceiver() {
        String TAG = "gpsServiceReceiver";

        public void onReceive(Context context, Intent intent) {
            try {
                if (intent == null)
                    return;

                String action = intent.getAction();

                if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_START)) {
                    Intent serviceIntent = new Intent(CarOwnerMainActivity.this, GpsTrackingService.class);
                    startService(serviceIntent);
                } else if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_STOP)) {
                    Intent serviceIntent = new Intent(CarOwnerMainActivity.this, GpsTrackingService.class);
                    stopService(serviceIntent);
                } else if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_STATE)) {

                } else if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_LOCATION_UPDATED)) {
                    Bundle args = intent.getBundleExtra("args");
                    ((TextView) findViewById(R.id.addressTxt0)).setText(args.getString("userAddress0"));
                    ((TextView) findViewById(R.id.addressTxt1)).setText(args.getString("userAddress1"));

                    Animation anim = new AlphaAnimation(0.0f, 1.0f);
                    anim.setDuration(500);
                    locationImg.startAnimation(anim);
                }
            } catch (Exception e)

            {
                e.printStackTrace();
            }
        }
    };

    private void init() {
        try {
            // GoogleHandler Play 서비스 호환 여부 체크
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);

            // 서버 콘솔에 YTMS 토픽이 없을 경우, 이 메서드 호출로 서버 콘솔에 자동으로 토픽 생성(생성하는데 몇 시간 걸림)
            FirebaseMessaging.getInstance().subscribeToTopic(Key.CHANNEL_COMMON);

            // FCM Token 서버에 전달
            MyFirebaseMessagingService.sendRegistrationToServer(this);

            requestGpsTransmitCheck();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onBackPressed() {
        ConfirmCancelDialog.show(this, "앱을 종료하시겠습니까?", "취소", "확인", true, new ConfirmCancelDialog.DialogListener() {
            public void onCancel() {
            }

            public void onConfirm() {
                // TEST
                if (DeviceUtil.getUuid().endsWith("810d")) {
                    // 앱 새로 실행 | 모든 Activity 삭제
                    Intent intent = new Intent(CarOwnerMainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }

                finish();
                //finishApp(); // CAUTION 사용시 Service 도 같이 종료
            }
        });
    }

    private void finishApp() {
        finish();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_from_right, R.anim.fade_out);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                break;
            case R.id.titleRightBtn1:
                break;
            case R.id.userNameTxt:
                startActivity(new Intent(this, CarOwnerAccountActivity.class), options.toBundle());
                break;
            // 배차
            case R.id.menuBtn0:
                startActivity(new Intent(this, CarAllocHistoryActivity.class), options.toBundle());
                break;
            // 하차대기
            case R.id.menuBtn1: {
                if (Key.getUserInfo() == null)
                    return;
                Intent intent = new Intent(this, ForkLiftCheckActivity.class);
                Bundle args = new Bundle();
                args.putString("CAR_CD", Key.getUserInfo().getString("USER_CD"));
                intent.putExtras(args);
                startActivity(intent, options.toBundle());
            }
            break;
            // 정산
            case R.id.menuBtn2:
                startActivity(new Intent(this, FreightChargeHistoryActivity.class), options.toBundle());
                break;
            // 공지사항
            case R.id.menuBtn3:
                startActivity(new Intent(this, NoticeListActivity.class), options.toBundle());
                break;
            case R.id.bottomCenterInfoView:
                AppUtil.runCallApp(getString(R.string.customer_call_center_phone_no), true);
                break;
        }
    }

    // TODO 2018.03 오픈 예정
    // TODO 아이콘 위치에서 Activity 가 커지는 material animation 적용
    //public void onClick(View v) {
    //    InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    //    mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
    //
    //    ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_from_right, R.anim.fade_out);
    //
    //    switch (v.getId()) {
    //        case R.id.titleLeftBtn0:
    //            break;
    //        case R.id.titleRightBtn1:
    //            break;
    //        case R.id.userNameTxt:
    //            startActivity(new Intent(this, CarOwnerAccountActivity.class), options.toBundle());
    //            break;
    //        // 배차
    //        case R.id.menuBtn0:
    //            startActivity(new Intent(this, CarAllocHistoryActivity.class), options.toBundle());
    //            break;
    //        // 인수증
    //        case R.id.menuBtn1:
    //            startActivity(new Intent(this, ReceiptDispatchCheckActivity.class), options.toBundle());
    //            break;
    //        // 정산
    //        case R.id.menuBtn2:
    //            startActivity(new Intent(this, FreightChargeHistoryActivity.class), options.toBundle());
    //            break;
    //        // 하차대기
    //        case R.id.menuBtn3: {
    //            if (Key.getUserInfo() == null)
    //                return;
    //            Intent intent = new Intent(this, ForkLiftCheckActivity.class);
    //            Bundle args = new Bundle();
    //            args.putString("CAR_CD", Key.getUserInfo().getString("USER_CD"));
    //            intent.putExtras(args);
    //            startActivity(intent, options.toBundle());
    //        }
    //        break;
    //        // 공지사항
    //        case R.id.menuBtn4:
    //            startActivity(new Intent(this, NoticeListActivity.class), options.toBundle());
    //            break;
    //        case R.id.bottomCenterInfoView:
    //            AppUtil.runCallApp(getString(R.string.customer_call_center_phone_no), true);
    //            break;
    //    }
    //}

    private boolean onReqCarAllocCnt;

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestCarAllocCount() {
        if (onReqCarAllocCnt)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReqCarAllocCnt = true;
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_CAR_ALLOC_CNT, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReqCarAllocCnt = false;

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            Bundle data = resBody.getBundle(Key.data);
                            ((TextView) findViewById(R.id.menuBtn0CntTxt0)).setText(data.getString("TOTAL_CNT", "0"));
                            String PROGRESS_CNT = data.getString("PROGRESS_CNT", "0");
                            ((TextView) findViewById(R.id.menuBtn0CntTxt1)).setText(data.getString("PROGRESS_CNT", "0"));
                            ((TextView) findViewById(R.id.menuBtn0CntTxt2)).setText(data.getString("FINISH_CNT", "0"));
                            if (PROGRESS_CNT.equals("0")) {
                                carAllocBadgeCntTxt.setVisibility(View.INVISIBLE);
                            } else {
                                carAllocBadgeCntTxt.setVisibility(View.VISIBLE);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean onReqNewNotiCnt;

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestNewNoticeCount() {
        if (onReqNewNotiCnt)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReqNewNotiCnt = true;
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("authCd", Key.getUserInfo().getString("AUTH_CD"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_NOTICE_NEW_COUNT, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReqNewNotiCnt = false;

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            Bundle data = resBody.getBundle(Key.data);
                            String NOTI_READ_N_CNT = data.getString("NOTI_READ_N_CNT", "0");

                            if (NOTI_READ_N_CNT.equals("0")) {
                                noticeBadgeCntTxt.setVisibility(View.INVISIBLE);
                            } else {
                                noticeBadgeCntTxt.setVisibility(View.VISIBLE);
                                noticeBadgeCntTxt.setText(NOTI_READ_N_CNT);
                            }
                        } else {

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean onReqGpsTransmitCheck;

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestGpsTransmitCheck() {
        if (onReqGpsTransmitCheck)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReqGpsTransmitCheck = true;
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_CAR_GPS_TRANSMIT_CHECK, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReqGpsTransmitCheck = false;

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            Bundle data = resBody.getBundle(Key.data);
                            if (data.getString("CHECK_YN").equalsIgnoreCase("Y")) {
                                startGpsTrackingService();
                            } else {
                                stopGpsTrackingService();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showGpsSnackbar() {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setPadding(0, 0, 0, 0);

        TextView snackbarV = (TextView) View.inflate(this, R.layout.notice_snackbar, null);
        snackbarV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.sharp_gps_off_black_36, 0, 0, 0);
        snackbarV.setText(R.string.enable_gps);
        snackbarV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        layout.addView(snackbarV, 0);

        snackbar.show();
    }

    private void startGpsTrackingService() {
        Intent serviceIntent = new Intent(this, GpsTrackingService.class);

        /** 홈 화면>모든 앱 종료시 Service 유지 **/
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(serviceIntent);
        }
        // Oreo
        else {
            startForegroundService(serviceIntent);
        }
    }

    private void stopGpsTrackingService() {
        Intent serviceIntent = new Intent(this, GpsTrackingService.class);
        stopService(serviceIntent);
    }

    private boolean isGpsTrackingServiceRunning = false;

    private boolean checkGpsTrackingServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (GpsTrackingService.class.getName().equals(service.service.getClassName())) {
                Log.e(TAG, "+ checkGpsTrackingServiceRunning(): YEAH~");
                isGpsTrackingServiceRunning = true;
                return true;
            }
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}