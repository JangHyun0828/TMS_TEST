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

    private CarRequestMenuFragment menuFragment = new CarRequestMenuFragment();
    private CarRequestInputFragment inputFragment = new CarRequestInputFragment();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_request_activity);

        getSupportFragmentManager().addOnBackStackChangedListener(fragmentStackListener);

        setTitleBar("차량 요청", R.drawable.selector_button_back, 0, R.drawable.selector_button_close);

        ((TextView) findViewById(R.id.callCenterTxt)).setText("운송전략팀 연결");
        ((TextView) findViewById(R.id.callCenterPhoneNoTxt)).setText(getString(R.string.delivery_call_center_phone_no));

        init();
    }

    private void init() {
        try {
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}