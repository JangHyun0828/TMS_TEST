package com.neognp.ytms.carowner.account;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioButton;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.login.LoginActivity;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.template.BasicFragment;
import com.trevor.library.util.AppUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AccountEditActivity extends BasicActivity {

    private boolean onReq;

    private String[] tabNames = {"기본정보", "차량정보", "서류첨부"};

    Bundle userInfo;
    ArrayList<Bundle> carTypeList;
    ArrayList<Bundle> carTonList;

    private TabLayout tabLayout;
    private ViewPager tabPager;
    private BasicFragment[] tabFragments = new BasicFragment[3];
    private BasicInfoFragment mBasicInfoFragment = new BasicInfoFragment();
    private CarInfoFragment mCarInfoFragment = new CarInfoFragment();
    private PaperAttachFragment mPaperAttachFragment = new PaperAttachFragment();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_edit_activity);

        setTitleBar("개인정보 수정");

        tabFragments[0] = mBasicInfoFragment;
        tabFragments[1] = mCarInfoFragment;
        tabFragments[2] = mPaperAttachFragment;

        tabPager = findViewById(R.id.tabPager);
        tabPager.setAdapter(mPagerAdapter);
        tabPager.setOffscreenPageLimit(3); // swipe 시 Fragment 새로 생성되지 않게 개수 고정
        tabPager.addOnPageChangeListener(mPageChangeListener);

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(tabPager);

        ((TextView) findViewById(R.id.callCenterTxt)).setText("운송전략팀 연결");
        ((TextView) findViewById(R.id.callCenterPhoneNoTxt)).setText(getString(R.string.delivery_call_center_phone_no));

        init();

        // TEST
        //tabPager.setCurrentItem(2);
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    private void init() {
        try {
            requestAccountInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FragmentPagerAdapter mPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
        public Fragment getItem(int position) {
            return (Fragment) tabFragments[position];
        }

        public int getCount() {
            return tabFragments.length;
        }

        public CharSequence getPageTitle(int position) {
            return tabNames[position];
        }
    };

    private ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
        }

        public void onPageScrollStateChanged(int state) {
        }
    };

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                finish();
                break;
            case R.id.titleRightBtn1:
                requestLoginActivity();
            case R.id.saveBtn:
                finish();
                break;
            case R.id.closeBtn:
                finish();
                break;
            case R.id.callCenterBtn:
                AppUtil.runCallApp(getString(R.string.delivery_call_center_phone_no), true);
                break;
        }
    }

    @SuppressLint ("StaticFieldLeak")
    synchronized void requestAccountInfo() {
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
                    return YTMSRestRequestor.requestPost(API.URL_CAR_USER_INFO, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            userInfo = resBody.getBundle(Key.data);
                            carTypeList = resBody.getParcelableArrayList("typeList");
                            carTonList = resBody.getParcelableArrayList("tonList");
                            sortCarTonList();

                            mBasicInfoFragment.init();
                            mCarInfoFragment.init();
                            mPaperAttachFragment.init();
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

    public void sortCarTonList() {
        if (carTonList == null || carTonList.isEmpty())
            return;

        // CODE 값으로 순서 정렬
        Collections.sort(carTonList, new Comparator<Bundle>() {
            public int compare(Bundle obj1, Bundle obj2) {
                String CODE1 = obj1.getString("CODE", "");
                String CODE2 = obj2.getString("CODE", "");
                return CODE1.compareTo(CODE2);
            }
        });
    }

    private void requestLoginActivity() {
        finish();
        Intent intent = new Intent(getContext(), LoginActivity.class);
        // 앱 새로 실행 | 모든 Activity 삭제
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out);
        startActivity(intent, options.toBundle());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}