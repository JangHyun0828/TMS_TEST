package com.neognp.ytms.shipper.car_req;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

public class CarRequestActivity extends BasicActivity {

    private boolean onReq;
    private Bundle args;

    //private Calendar curCal;
    //
    //private Button curDateBtn;

    private CarRequestMenuFragment menuFragment = new CarRequestMenuFragment();
    private CarRequestInputFragment inputFragment = new CarRequestInputFragment();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_request_activity);

        getSupportFragmentManager().addOnBackStackChangedListener(fragmentStackListener);

        //curCal = Calendar.getInstance();
        //curCal.set(Calendar.HOUR_OF_DAY, 0);
        //curCal.set(Calendar.MINUTE, 0);
        //curCal.set(Calendar.SECOND, 0);
        //curCal.set(Calendar.MILLISECOND, 0);

        setTitleBar("차량 요청", R.drawable.selector_button_back, 0, R.drawable.selector_button_close);

        //curDateBtn = (Button) findViewById(R.id.curDateBtn);
        //curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));

        ((TextView) findViewById(R.id.callCenterTxt)).setText("운송전략팀 연결");
        ((TextView) findViewById(R.id.callCenterPhoneNoTxt)).setText(getString(R.string.delivery_call_center_phone_no));

        init();
    }

    private void init() {
        try {
            //args = getIntent().getExtras();
            //if (args == null)
            //    return;

            addMenuFragment();

            // TEST
            //addInputFragment();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    FragmentManager.OnBackStackChangedListener fragmentStackListener = new FragmentManager.OnBackStackChangedListener() {
        String TAG = "fragmentStackListener";

        public void onBackStackChanged() {
            FragmentManager fm = getSupportFragmentManager();
            List<Fragment> fragments = fm.getFragments();
            Log.e(TAG, "+ onBackStackChanged(): list=" + fragments);

            if (fragments != null && fragments.size() != 0) {
                Fragment lastFragment = null;

                for (int i = fragments.size() - 1; i >= 0; i--) {
                    // CAUTION fragments의 맨 마지막은 null일 수 있으므로 null이 아닌 요소를 마지막 Fragment로 지정
                    if (fragments.get(i) != null) {
                        lastFragment = fragments.get(i);
                        break;
                    }
                }

                Log.i(TAG, "+ onBackStackChanged()  lastFragment=" + lastFragment.getClass().getSimpleName());
            }
        }
    };

    public void onBackPressed() {
        removeCurrentFragment();
    }

    void removeCurrentFragment() {
        FragmentManager fm = getSupportFragmentManager();

        List<Fragment> fragments = fm.getFragments();
        Log.i(TAG, "+ removeCurrentFragment(): fragments=" + fragments);

        if (fm.getBackStackEntryCount() > 1)
            fm.popBackStack();
        else
            finish();
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                removeCurrentFragment();
                break;
            case R.id.titleRightBtn1:
                finish();
                break;
            //case R.id.prevDateBtn:
            //    break;
            //case R.id.curDateBtn:
            //    showCalendar();
            //    break;
            //case R.id.nextDateBtn:
            //    break;
            case R.id.callCenterBtn:
                AppUtil.runCallApp(getString(R.string.delivery_call_center_phone_no), true);
                break;
        }
    }

    void addMenuFragment() {
        getSupportFragmentManager().
                beginTransaction().
                add(R.id.fragmentContainer, menuFragment, CarRequestMenuFragment.class.getSimpleName()).
                addToBackStack(null).
                commitAllowingStateLoss();
    }

    void addInputFragment() {
        getSupportFragmentManager().
                beginTransaction().
                add(R.id.fragmentContainer, inputFragment, CarRequestInputFragment.class.getSimpleName()).
                addToBackStack(null).
                commitAllowingStateLoss();
    }

    //private void showCalendar() {
    //    DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
    //        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
    //            Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
    //
    //            curCal.set(Calendar.YEAR, year);
    //            curCal.set(Calendar.MONTH, monthOfYear);
    //            curCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    //
    //            curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
    //        }
    //    }, curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH), curCal.get(Calendar.DAY_OF_MONTH));
    //
    //    datePickerDialog.show();
    //}

    private void search() {
        try {
            // 오늘 날짜 이후로 설정 금지
            //Calendar todayCal = Calendar.getInstance();
            //todayCal.set(Calendar.HOUR_OF_DAY, 0);
            //todayCal.set(Calendar.MINUTE, 0);
            //todayCal.set(Calendar.SECOND, 0);
            //todayCal.set(Calendar.MILLISECOND, 0);
            //if (curCal.after(todayCal)) {
            //    showSnackbar(R.drawable.ic_insert_invitation_black_24dp, "조회 종료일은 오늘 날짜 이후로 지정할 수 없습니다.");
            //    return;
            //}

            requestList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestList() {
        if (onReq)
            return;

        //if(args == null)
        //    return;

        try {
            if (Key.getUserInfo() == null)
                return;

            //final String  = ((TextView) findViewById(R.id.)).getText().toString().trim();
            //if (.isEmpty()) {
            //    showToast("입력하세요.", true);
            //    return;
            //}

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        //payloadJson.put("", );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {

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