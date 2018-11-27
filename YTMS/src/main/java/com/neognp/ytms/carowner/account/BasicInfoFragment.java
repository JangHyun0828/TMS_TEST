package com.neognp.ytms.carowner.account;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.Key;
import com.trevor.library.template.BasicFragment;
import com.trevor.library.util.DeviceUtil;
import com.trevor.library.util.TextUtil;

public class BasicInfoFragment extends BasicFragment implements View.OnClickListener {

    private View contentView;

    private AccountEditActivity host;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        host = (AccountEditActivity) getActivity();

        contentView = inflater.inflate(R.layout.basic_info_fragment, container, false);

        //=(EditText) contentView.findViewById(R.id.);
        //=(TextView) contentView.findViewById(R.id.);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // CAUTION Fragment 초기화 후 데이타 요청
        ((AccountEditActivity) getActivity()).requestAccountInfo();
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    void init() {
        try {
            if (Key.getUserInfo() == null || host.userInfo == null)
                return;

            ((TextView) contentView.findViewById(R.id.car_noEdit)).setText(host.userInfo.getString("CAR_NO"));
            ((TextView) contentView.findViewById(R.id.mobile_no_edit)).setText(TextUtil.formatPhoneNumber(host.userInfo.getString("MOBILE_NO")));
            ((TextView) contentView.findViewById(R.id.user_nmEdit)).setText(Key.getUserInfo().getString("USER_NM"));
        } catch (Exception e) {
            e.printStackTrace();
        }
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