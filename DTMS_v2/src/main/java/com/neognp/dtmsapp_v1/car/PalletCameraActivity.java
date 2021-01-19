package com.neognp.dtmsapp_v1.car;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.http.FileUploadTask;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.ImageFilePath;
import com.trevor.library.util.PhotoRotationUtil;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

public class PalletCameraActivity extends BasicActivity implements FileUploadTask.FileUploadTaskListener, View.OnClickListener
{
    private boolean onReq;
    private PalletCameraFragment palletCameraFragment;
    private static final int REQ_PICK_IMAGE = 400;

    private Bundle args;
    private File uploadFile;
    private ImageView previewImg;

    private String filePath;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pallet_camera_activity);

        palletCameraFragment = (PalletCameraFragment) getSupportFragmentManager().findFragmentById(R.id.palletCameraFragment);

        init();
    }

    private void init()
    {
        try
        {
            args = getIntent().getExtras();

            if(args == null)
                return;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            // 사진 촬영
            case R.id.takePhotoBtn:
                palletCameraFragment.takePicture();
                break;
            // 사진촬영 모드 변경
            case R.id.showCameraBtn:
                break;

            // 저장
            case R.id.saveBtn:
                saveFile();
                break;
        }
    }

    private void dispatchPicImageIntent()
    {
        try
        {
            Intent pickPicIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPicIntent, REQ_PICK_IMAGE);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private HashMap<String, String> fileParams = new HashMap<String, String>();
    private FileUploadTask mFileUploadTask;

    private synchronized void saveFile()
    {
        String fileYn = ((CheckBox) findViewById(R.id.noReceiptCheck)).isChecked() ? "N" : "Y";

        if (onReq)
            return;

        if (args == null)
            return;

        try
        {
            if (Key.getUserInfo() == null)
                return;

            if(args.getString("INOUT").equals("IN"))
            {
                JSONObject payloadJson = RestRequestor.buildPayload();
                payloadJson.put("carCd", Key.getUserInfo().getString("USER_CD"));
                payloadJson.put("deliveryNo", args.getString("PLAN_NO"));
                payloadJson.put("deptCd", args.getString("DEPT_CD"));
                payloadJson.put("outCd", args.getString("OUT_CD"));
                payloadJson.put("moveDt", args.getString("MOVE_DT", "").substring(0, 4) + args.getString("MOVE_DT", "").substring(6,8) + args.getString("MOVE_DT", "").substring(10,12));
                payloadJson.put("moveSeq", args.getString("MOVE_SEQ"));
                payloadJson.put("fileGb", args.getString("FILE"));
                payloadJson.put("fileYn", fileYn);

                if (fileYn.equalsIgnoreCase("Y"))
                {
                    if (uploadFile == null)
                    {
                        showToast("팔레트사진을 촬영해 주십시요. 팔레트사진이 없는 경우, 인수증 없음을 체크해 저장해 주십시요.", false);
                        return;
                    }
                    else
                    {
                        //_filePath = uploadFile.getPath();
                    }
                }
                //final String filePath = _filePath;
                //fileParams.put("receiptFile", filePath);

                mFileUploadTask = new FileUploadTask(null, API.URL_CAR_UPLOAD_IN_PALLET, false, payloadJson, "palletFile", filePath, this, true);
                mFileUploadTask.execute();
            }
            else if(args.getString("INOUT").equals("OUT"))
            {
                JSONObject payloadJson = RestRequestor.buildPayload();
                payloadJson.put("carCd", Key.getUserInfo().getString("USER_CD"));
                payloadJson.put("deliveryNo", args.getString("DELIVERY_NO"));
                payloadJson.put("outCd", args.getString("OUT_CD"));
                payloadJson.put("moveDt", args.getString("MOVE_DT"));
                payloadJson.put("fileGb", args.getString("FILE"));
                payloadJson.put("fileYn", fileYn);

                if (fileYn.equalsIgnoreCase("Y"))
                {
                    if (uploadFile == null)
                    {
                        showToast("팔레트사진을 촬영해 주십시요. 팔레트사진이 없는 경우, 인수증 없음을 체크해 저장해 주십시요.", false);
                        return;
                    }
                    else
                    {
                        //_filePath = uploadFile.getPath();
                        // -filePath = reduceUploadFileSize();
                    }
                }
                //final String filePath = _filePath;
                //fileParams.put("receiptFile", filePath);

                mFileUploadTask = new FileUploadTask(null, API.URL_CAR_UPLOAD_OUT_PALLET, false, payloadJson, "palletFile", filePath, this, true);
                mFileUploadTask.execute();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void setPhotoResult()
    {
        //finish();
    }

    // 사진 촬영 결과
    public void onPictureTaken(String file_path)
    {
        Log.e(TAG, "+ onPictureTaken(): file_path= " + file_path);

        if (file_path == null)
            return;

        filePath = file_path;
        setPreviewPhoto(filePath);
    }

    private void setPreviewPhoto(final String photoPath) {
        try {
            if (photoPath == null || photoPath.isEmpty())
                return;

            //File dir = getCacheDir();
            // TEST
            File dir = Key.getDebugStorage();

            String fileName = "DTMS_PALLET.jpg";

            uploadFile = new File(fileName);
            if (uploadFile == null) {
                Log.i(TAG, "+ setPreviewPhoto(): Error creating uploadFile, check storage permissions: ");
                return;
            } else {
                Log.e(TAG, "+ setPreviewPhoto(): original file data=" + TextUtil.formatFileSize(fileName.length()));
            }

            int targetW = previewImg.getWidth();
            int targetH = previewImg.getHeight();

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(photoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            int scaleFactor = 1;
            if ((targetW > 0) || (targetH > 0)) {
                scaleFactor = Math.min(photoW / targetW, photoH / targetH);
            }

            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inSampleSize = 0;
            //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            //    bmOptions.inPurgeable = true;

            // 액자 크기에 맞춰 이미지 축소
            Bitmap bmp = BitmapFactory.decodeFile(photoPath, bmOptions);

            // 사진 회전
            Bitmap rotatedBmp = PhotoRotationUtil.rotatePhoto(photoPath, bmp);

            // 미리보기 창에 선택된 사진 붙이기
            previewImg.setImageBitmap(rotatedBmp);

            /* 파일로 저장 */

            FileOutputStream uploadFos = new FileOutputStream(uploadFile);

            // 업로드 사진을 1024x768 사이즈로 제한
            targetW = 1024;
            targetH = 768;
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            //    bmOptions.inPurgeable = true;

            Bitmap uploadBmp = BitmapFactory.decodeFile(photoPath, bmOptions);
            // CAUTION 100으로 지정 시, 용량이 더 커지므로 100 미만 적용 / 생성된 파일은 폰 디렉토리 APP로 확인 가능.
            uploadBmp.compress(Bitmap.CompressFormat.JPEG, 80, uploadFos);
            uploadFos.close();

            Log.e(TAG, "+ setPreviewPhoto(): uploadFile=" + uploadFile.getPath() + " / " + TextUtil.formatFileSize(uploadFile.length()));

            notifyNewFile(uploadFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyNewFile(String photoPath) {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(photoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    @Override
    public void onStartUpload()
    {

    }

    @Override
    public void onProgressUpload(float progress)
    {

    }

    @Override
    public void onFinishUpload(Bundle response)
    {
        onReq = false;
        dismissLoadingDialog();

        try {
            Bundle resBody = response.getBundle(Key.resBody);
            String result_code = resBody.getString(Key.result_code);
            String result_msg = resBody.getString(Key.result_msg);

            if (result_code.equals("200"))
            {
                showToast("인수증이 저장되었습니다.", true);
                finish();
            }
            else
            {
                showToast(result_msg + "(result_code:" + result_msg + ")", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("일시적인 네트워크장애가 발생하였거나 서버 응답이 느립니다.\n 잠시 후 이용해주시기 바랍니다.", false);
        }
    }

    @Override
    public void onCancelUpload()
    {

    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_IMAGE)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                if (data == null)
                    return;

                Uri selectedFileUri = data.getData();
                String photoPath = ImageFilePath.getPath(this, selectedFileUri);
                // CAUTION "raw:/ 로 시작하는 디렉토리 인식 못함 ex) "raw:/storage/emulated/0/Download/Test_Pic/pic3.jpg"
                Log.e(TAG, "+ onActivityResult(): photoPath: " + photoPath);
                //photoPath = storage/emulated/0/Download/DTMS/DTMS_PAPER.jpg
                setPreviewPhoto(photoPath);
            }
            else
            {
                //finish();
                showToast("*onActivityResult*", true);
            }
        }
    }

}
