package com.neognp.ytms.carowner.account;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.trevor.library.template.BasicFragment;
import com.trevor.library.util.Setting;

public class PaperAttachFragment extends BasicFragment implements View.OnClickListener {

    private boolean onReq;
    private Bundle args;

    private View contentView;

    private ImageButton cameraBtn0, cameraBtn1, cameraBtn2, cameraBtn3, cameraBtn4, cameraBtn5, cameraBtn6, cameraBtn7;

    private AccountEditActivity host;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        host = (AccountEditActivity) getActivity();

        contentView = inflater.inflate(R.layout.paper_attach_fragment, container, false);

        cameraBtn0 = contentView.findViewById(R.id.cameraBtn0);
        cameraBtn0.setOnClickListener(this);
        cameraBtn1 = contentView.findViewById(R.id.cameraBtn1);
        cameraBtn1.setOnClickListener(this);
        cameraBtn2 = contentView.findViewById(R.id.cameraBtn2);
        cameraBtn2.setOnClickListener(this);
        cameraBtn3 = contentView.findViewById(R.id.cameraBtn3);
        cameraBtn3.setOnClickListener(this);
        cameraBtn4 = contentView.findViewById(R.id.cameraBtn4);
        cameraBtn4.setOnClickListener(this);
        cameraBtn5 = contentView.findViewById(R.id.cameraBtn5);
        cameraBtn5.setOnClickListener(this);
        cameraBtn6 = contentView.findViewById(R.id.cameraBtn6);
        cameraBtn6.setOnClickListener(this);
        cameraBtn7 = contentView.findViewById(R.id.cameraBtn7);
        cameraBtn7.setOnClickListener(this);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    void init() {
        try {
            if (host.accountInfo == null)
                return;

            // 사업자등록증
            if (host.accountInfo.getString("L_BUSINESS_YN", "N").equalsIgnoreCase("Y")) {
                cameraBtn0.setImageResource(R.drawable.camera_check);
            } else {
                cameraBtn0.setImageResource(R.drawable.img_camera_up);
            }

            //  차량등록증
            if (host.accountInfo.getString("L_CAR_YN", "N").equalsIgnoreCase("Y")) {
                cameraBtn1.setImageResource(R.drawable.camera_check);
            } else {
                cameraBtn1.setImageResource(R.drawable.img_camera_up);
            }

            //  통장사본
            if (host.accountInfo.getString("L_BANKBOOK_YN", "N").equalsIgnoreCase("Y")) {
                cameraBtn2.setImageResource(R.drawable.camera_check);
            } else {
                cameraBtn2.setImageResource(R.drawable.img_camera_up);
            }

            //  운전면허증
            if (host.accountInfo.getString("L_DRIVER_YN", "N").equalsIgnoreCase("Y")) {
                cameraBtn3.setImageResource(R.drawable.camera_check);
            } else {
                cameraBtn3.setImageResource(R.drawable.img_camera_up);
            }

            //  적재물보험증
            if (host.accountInfo.getString("L_ITEM_YN", "N").equalsIgnoreCase("Y")) {
                cameraBtn4.setImageResource(R.drawable.camera_check);
            } else {
                cameraBtn4.setImageResource(R.drawable.img_camera_up);
            }

            //  운전경력증명서
            if (host.accountInfo.getString("L_CAREER_YN", "N").equalsIgnoreCase("Y")) {
                cameraBtn5.setImageResource(R.drawable.camera_check);
            } else {
                cameraBtn5.setImageResource(R.drawable.img_camera_up);
            }

            //  화물운송증명서
            if (host.accountInfo.getString("L_TRANS_YN", "N").equalsIgnoreCase("Y")) {
                cameraBtn6.setImageResource(R.drawable.camera_check);
            } else {
                cameraBtn6.setImageResource(R.drawable.img_camera_up);
            }

            // 지방세납부현황
            if (host.accountInfo.getString("L_TAX_YN", "N").equalsIgnoreCase("Y")) {
                cameraBtn7.setImageResource(R.drawable.camera_check);
            } else {
                cameraBtn7.setImageResource(R.drawable.img_camera_up);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

        switch (v.getId()) {
            // 사업자등록증
            case R.id.cameraBtn0:
                showAccountPaperCameraActivity("businessFile", "사업자등록증", host.accountInfo.getString("BUSINESS_FILE_PATH", ""));
                break;
            // 차량등록증
            case R.id.cameraBtn1:
                showAccountPaperCameraActivity("carFile", "차량등록증", host.accountInfo.getString("CAR_FILE_PATH", ""));
                break;
            // 통장사본
            case R.id.cameraBtn2:
                showAccountPaperCameraActivity("bankbookFile", "통장사본", host.accountInfo.getString("BANKBOOK_FILE_PATH", ""));
                break;
            // 운전면허증
            case R.id.cameraBtn3:
                showAccountPaperCameraActivity("driverFile", "운전면허증", host.accountInfo.getString("DRIVER_FILE_PATH", ""));
                break;
            // 적재물보험증
            case R.id.cameraBtn4:
                showAccountPaperCameraActivity("itemFile", "적재물보험증", host.accountInfo.getString("ITEM_FILE_PATH", ""));
                break;
            //  운전경력증명서
            case R.id.cameraBtn5:
                showAccountPaperCameraActivity("careerFile", "운전경력증명서", host.accountInfo.getString("CAREER_FILE_PATH", ""));
                break;
            // 화물운송증명서
            case R.id.cameraBtn6:
                showAccountPaperCameraActivity("transFile", "화물운송증명서", host.accountInfo.getString("TRANS_FILE_PATH", ""));
                break;
            // 지방세납부현황
            case R.id.cameraBtn7:
                showAccountPaperCameraActivity("taxFile", "지방세납부현황", host.accountInfo.getString("TAX_FILE_PATH", ""));
                break;
        }
    }

    private void showAccountPaperCameraActivity(String fileParamName, String title, String savedFileUrl) {
        Intent intent = new Intent(getContext(), AccountPaperCameraActivity.class);
        Bundle args = new Bundle();
        args.putString("fileParamName", fileParamName);
        args.putString(Key.title, title);
        if (!savedFileUrl.isEmpty()) {
            savedFileUrl = "http://" + Setting.getString("ip") + ":" + Setting.getInt("port") + savedFileUrl;
            args.putString("savedFileUrl", savedFileUrl);
        }
        intent.putExtras(args);
        ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.fade_in, R.anim.fade_out);
        startActivityForResult(intent, 0, options.toBundle());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}