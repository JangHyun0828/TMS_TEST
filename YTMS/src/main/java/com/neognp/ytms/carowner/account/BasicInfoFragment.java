package com.neognp.ytms.carowner.account;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.Key;
import com.trevor.library.template.BasicFragment;
import com.trevor.library.util.TextUtil;

public class BasicInfoFragment extends BasicFragment implements View.OnClickListener {

    private View contentView;

    private EditText userPwEdit, userPwConfirmEdit;
    private CheckBox privacyCheck, locationCheck;

    private CarOwnerAccountActivity host;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        host = (CarOwnerAccountActivity) getActivity();

        contentView = inflater.inflate(R.layout.basic_info_fragment, container, false);

        userPwEdit = contentView.findViewById(R.id.userPwEdit);
        userPwConfirmEdit = contentView.findViewById(R.id.userPwConfirmEdit);

        privacyCheck = contentView.findViewById(R.id.privacyCheck);
        locationCheck = contentView.findViewById(R.id.locationCheck);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // CAUTION Fragment 초기화 후 데이타 요청
        ((CarOwnerAccountActivity) getActivity()).requestAccountInfo();
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    void init() {
        try {
            if (Key.getUserInfo() == null || host.accountInfo == null)
                return;

            ((TextView) contentView.findViewById(R.id.car_noEdit)).setText(host.accountInfo.getString("CAR_NO"));

            ((TextView) contentView.findViewById(R.id.mobile_no_edit)).setText(TextUtil.formatPhoneNumber(host.accountInfo.getString("MOBILE_NO")));

            ((TextView) contentView.findViewById(R.id.user_nmEdit)).setText(Key.getUserInfo().getString("USER_NM"));

            if (host.accountInfo.getString("PRIVACY_YN", "N").equalsIgnoreCase("Y"))
                privacyCheck.setChecked(true);
            else
                privacyCheck.setChecked(false);

            if (host.accountInfo.getString("LOCATION_YN", "N").equalsIgnoreCase("Y"))
                locationCheck.setChecked(true);
            else
                locationCheck.setChecked(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isValidUserPwEdit() {
        String userPw = userPwEdit.getText().toString().trim();
        String userPwConfirm = userPwConfirmEdit.getText().toString().trim();

        if (userPw.isEmpty() && userPwConfirm.isEmpty()) {
            return true;
        } else if (!userPw.isEmpty() && userPwConfirm.isEmpty()) {
            showToast("비밀번호 확인을 입력해 주십시요.", true);
            return false;
        } else if (userPw.isEmpty() && !userPwConfirm.isEmpty()) {
            showToast("비밀번호를 입력해 주십시요.", true);
            return false;
        } else if (!userPw.equals(userPwConfirm)) {
            showToast("비밀번호 확인이 일치하지 않습니다.", true);
            return false;
        }

        return true;
    }

    String getUserPw() {
        String userPw = userPwEdit.getText().toString().trim();

        return userPw.isEmpty()? null: userPw;
    }

    String getPrivacyYn() {
        return privacyCheck.isChecked() ? "Y" : "N";
    }

    String getLocationYn() {
        return locationCheck.isChecked() ? "Y" : "N";
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

        switch (v.getId()) {
            //case R.id.:
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}