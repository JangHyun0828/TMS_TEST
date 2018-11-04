package com.neognp.ytms.delivery.main;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.delivery.car_alloc.DeliveryCarAllocInfoActivity;
import com.neognp.ytms.delivery.direct.DirectDeliveryActivity;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.notice.NoticeListActivity;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;

import org.json.JSONObject;

public class DeliveryMainActivity extends BasicActivity {

    private boolean onReq;
    private Bundle args;

    private boolean onNewIntent;

    private TextView noticeBadgeCntTxt;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.delivery_main_activity);

        IntentFilter workActionFilter = new IntentFilter();
        workActionFilter.addAction(Key.ACTION_LOCATION_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(gpsActionReceiver, workActionFilter);

        setTitleBar(R.string.app_name, R.drawable.selector_button_back, 0, R.drawable.selector_button_setting);

        if (Key.getUserInfo() != null)
            ((TextView) findViewById(R.id.userNameTxt)).setText(Key.getUserInfo().getString("USER_NM", ""));

        findViewById(R.id.locationImg).setVisibility(View.GONE);
        findViewById(R.id.userLocAddrTxt0).setVisibility(View.GONE);
        findViewById(R.id.userLocAddrTxt1).setVisibility(View.GONE);

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

        LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsActionReceiver);
    }

    private final BroadcastReceiver gpsActionReceiver = new BroadcastReceiver() {
        String TAG = "gpsActionReceiver";

        public void onReceive(Context context, Intent intent) {
            try {

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

    // TODO 아이콘 위치에서 Activity 가 커지는 material animation 적용
    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_from_right, R.anim.fade_out);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                finish();
                break;
            case R.id.titleRightBtn1:
                break;
            case R.id.userNameTxt:
                //startActivity(new Intent(this, AccountEditActivity.class), options.toBundle());
                break;
            // 수송
            case R.id.menuBtn0:
                //startActivity(new Intent(this, .class), options.toBundle());
                break;
            // 상태
            case R.id.menuBtn1:
                startActivity(new Intent(this, DeliveryCarAllocInfoActivity.class), options.toBundle());
                break;
            // 반품
            case R.id.menuBtn2:
                //startActivity(new Intent(this, .class), options.toBundle());
                break;
            // 직송조회
            case R.id.menuBtn3:
                startActivity(new Intent(this, DirectDeliveryActivity.class), options.toBundle());
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
                            ((TextView) findViewById(R.id.menuBtn1CntTxt0)).setText(data.getString("TOTAL_CNT", ""));
                            ((TextView) findViewById(R.id.menuBtn1CntTxt1)).setText(data.getString("PROGRESS_CNT", ""));
                            ((TextView) findViewById(R.id.menuBtn1CntTxt2)).setText(data.getString("FINISH_CNT", ""));
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

    public void showActivity(Bundle args) {
        if (args == null)
            return;

        // Intent intent = new Intent(this, Activity.class);
        // intent.putExtra(LibKey.args, args);
        // startActivityForResult(intent, REQUEST_);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}