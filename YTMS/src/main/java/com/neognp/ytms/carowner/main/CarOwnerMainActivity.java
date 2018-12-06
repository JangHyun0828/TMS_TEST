package com.neognp.ytms.carowner.main;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
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

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.carowner.account.CarOwnerAccountActivity;
import com.neognp.ytms.carowner.car_alloc.CarAllocHistoryActivity;
import com.neognp.ytms.carowner.car_alloc.ForkLiftCheckActivity;
import com.neognp.ytms.carowner.charge.FreightChargeHistoryActivity;
import com.neognp.ytms.carowner.receipt.ReceiptDispatchCheckActivity;
import com.neognp.ytms.gps.GpsTrackingService;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.notice.NoticeListActivity;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;

import org.json.JSONObject;

public class CarOwnerMainActivity extends BasicActivity {

    private boolean onReq;
    private Bundle args;

    private boolean onNewIntent;

    private ImageView locationImg;

    private TextView carAllocBadgeCntTxt;
    private TextView noticeBadgeCntTxt;

    private GpsTrackingService mGpsTrackingService;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_owner_main_activity);

        IntentFilter workActionFilter = new IntentFilter();
        workActionFilter.addAction(Key.ACTION_LOCATION_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(gpsUpdateReceiver, workActionFilter);

        Intent serviceIntent = new Intent(this, GpsTrackingService.class);

        /** 홈 화면>모든 앱 종료시 Service 유지 **/
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        //    startService(serviceIntent);
        //}
        //// Oreo
        //else {
        //    startForegroundService(serviceIntent);
        //}

        /** 홈 화면>모든 앱 종료시 Service 종료 **/
        bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE);

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

        LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsUpdateReceiver);

        //Intent serviceIntent = new Intent(this, GpsTrackingService.class);
        //stopService(serviceIntent);

        unbindService(mServiceConnection);
        mGpsTrackingService = null;
    }

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

    private final BroadcastReceiver gpsUpdateReceiver = new BroadcastReceiver() {
        String TAG = "gpsUpdateReceiver";

        public void onReceive(Context context, Intent intent) {
            try {
                if (intent == null)
                    return;

                Bundle args = intent.getBundleExtra("args");

                ((TextView) findViewById(R.id.addressTxt0)).setText(args.getString("userAddress0"));
                ((TextView) findViewById(R.id.addressTxt1)).setText(args.getString("userAddress1"));

                Animation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(500);
                locationImg.startAnimation(anim);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void init() {
        try {
            args = getIntent().getExtras();
            if (args == null)
                return;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO 아이콘 위치에서 Activity가 커지는 material animation 적용
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
            // 인수증
            case R.id.menuBtn1:
                startActivity(new Intent(this, ReceiptDispatchCheckActivity.class), options.toBundle());
                break;
            // 정산
            case R.id.menuBtn2:
                startActivity(new Intent(this, FreightChargeHistoryActivity.class), options.toBundle());
                break;
            // 하차대기
            case R.id.menuBtn3: {
                if (Key.getUserInfo() == null)
                    return;
                Intent intent = new Intent(this, ForkLiftCheckActivity.class);
                Bundle args = new Bundle();
                args.putString("CAR_CD", Key.getUserInfo().getString("USER_CD"));
                intent.putExtras(args);
                startActivity(intent, options.toBundle());
            }
            break;
            // 공지사항
            case R.id.menuBtn4:
                startActivity(new Intent(this, NoticeListActivity.class), options.toBundle());
                break;
            case R.id.bottomCenterInfoView:
                AppUtil.runCallApp(getString(R.string.customer_call_center_phone_no), true);
                break;
        }
    }

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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}