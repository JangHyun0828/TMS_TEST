package com.neognp.ytms.carowner.car_alloc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSFileUploadTask;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.ImageFilePath;

import org.json.JSONObject;

public class ReceiptPhotoActivity extends BasicActivity implements YTMSFileUploadTask.FileUploadTaskListener {

    public static final int REQ_PICK_IMAGE = 100;

    private String mCurrentPhotoPath;

    private boolean onReq;
    private Bundle args;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receipt_photo_activity);

        setTitleBar("인수증 촬영", 0, 0, R.drawable.selector_button_close);

        init();
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    private void init() {
        try {
            args = getIntent().getExtras();
            if (args == null)
                return;


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                break;
            case R.id.titleRightBtn1:
                finish();
                break;
            // 촬영
            case R.id.bottomBtn0:
                break;
            // 사진 불러오기
            case R.id.bottomBtn1:
                dispatchPicImageIntent();
                break;
            // 저장
            case R.id.bottomBtn2:
                requestReceiptSave(null);
                break;
        }
    }

    private void dispatchPicImageIntent() {
        try {
            Intent pickPicIntent = new Intent();
            pickPicIntent.setType("image/*");
            pickPicIntent.setAction(Intent.ACTION_GET_CONTENT);
            if (pickPicIntent.resolveActivity(getPackageManager()) != null)
                startActivityForResult(Intent.createChooser(pickPicIntent, "이미지 선택"), REQ_PICK_IMAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private YTMSFileUploadTask mFileUploadTask;

    private synchronized void requestReceiptSave(@NonNull String picPath) {
        if (onReq)
            return;

        if (args == null)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            JSONObject payload = YTMSRestRequestor.buildPayload();
            payload.put("dispatchNo", args.getString("DISPATCH_NO"));
            String receipt_yn = ((CheckBox) findViewById(R.id.noReceiptCheck)).isChecked() ? "n" : "y";
            payload.put("file_yn", receipt_yn);
            //String filePath = reduceUploadFileSize();
            String filePath = picPath;
            // TEST
            //if (DeviceUtil.getUuid().endsWith("810d"))
            //    filePath= "storage/emulated/0/Download/white_tiger.jpg";
            mFileUploadTask = new YTMSFileUploadTask(null, API.URL_CAR_RECEIPT_SAVE, false, payload, "file", filePath, this, true);
            mFileUploadTask.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStartUpload() {
        onReq = true;
        showLoadingDialog(getString(R.string.uploading), false);
    }

    @Override
    public void onProgressUpload(float progress) {
    }

    @Override
    public void onFinishUpload(Bundle response) {
        onReq = false;
        dismissLoadingDialog();

        try {
            Bundle resBody = response.getBundle(Key.resBody);
            String result_code = resBody.getString(Key.result_code);
            String result_msg = resBody.getString(Key.result_msg);

            if (result_code.equals("200")) {
                Intent data = new Intent();
                data.putExtras(args);
                setResult(CarAllocHistoryActivity.RESULT_SAVED_RECEIPT, data);
                finish();
            } else {
                showToast(result_msg + "(result_code:" + result_msg + ")", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast(e.getMessage(), false);
        }
    }

    @Override
    public void onCancelUpload() {
        //finish();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            // 갤러리
            if (requestCode == REQ_PICK_IMAGE) {
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null)
                        return;

                    Uri selectedFileUri = data.getData();
                    mCurrentPhotoPath = ImageFilePath.getPath(this, selectedFileUri);
                    Log.e(TAG, "+ onActivityResult(): mCurrentPhotoPath: " + mCurrentPhotoPath);

                    //setPreviewPhoto(mCurrentPhotoPath);
                } else {
                    finish();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}