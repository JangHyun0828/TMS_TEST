package com.neognp.ytms.carowner.account;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.neognp.ytms.http.YTMSFileUploadTask;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.app.LibApp;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.ImageFilePath;
import com.trevor.library.util.ImageResizeUtil;
import com.trevor.library.util.PhotoRotationUtil;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AccountPaperCameraActivity extends BasicActivity {

    public static final int RESULT_SAVED_PAPER = 200;
    public static final int REQ_PICK_IMAGE = 400;

    private Bundle args;

    private int maxPreviewSizeW = 1280; // G5, G Pro, S2 HD> 1280x720 모두 지원
    private Camera.Size previewSize;
    private Camera mCamera;

    private FrameLayout cameraContainer;
    private CameraPreview mCameraPreview;

    private ImageView previewImg;
    private ProgressBar imgProgress;

    private File uploadFile;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_paper_camera_activity);

        // Create an instance of Camera
        mCamera = getCameraInstance();
        if (mCamera == null) {
            showToast(R.string.unavailable_camera, true);
        }

        setTitleBar("촬영", 0, 0, R.drawable.selector_button_close);

        // Create our Preview view and set it as the content of our activity.
        mCameraPreview = new CameraPreview(this, mCamera);
        cameraContainer = findViewById(R.id.cameraContainer);
        cameraContainer.addView(mCameraPreview, 0);

        previewImg = findViewById(R.id.previewImg);
        imgProgress = findViewById(R.id.imgProgress);

        //setCameraMode(false);

        init();
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    private void init() {
        try {
            args = getIntent().getExtras();
            if (args == null)
                return;

            setTitleBar(args.getString(Key.title, "") + " 촬영");

            String savedFileUrl = args.getString("savedFileUrl");

            if (savedFileUrl != null && !savedFileUrl.isEmpty()) {
                setCameraMode(false);

                ((TextView) findViewById(R.id.guideTxt)).setText("등록된 사진");

                Glide.
                        with(getContext()).
                        load(savedFileUrl).
                        apply(new RequestOptions().
                                diskCacheStrategy(DiskCacheStrategy.NONE). // disk cache off
                                skipMemoryCache(true) //memory cache off
                        ).
                        listener(new RequestListener<Drawable>() {
                            //@Override
                            //public boolean onResourceReady(R resource, Object model, Target<R> target, DataSource dataSource, boolean isFirstResource) {
                            //    return true;
                            //}

                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                imgProgress.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                imgProgress.setVisibility(View.GONE);
                                return false;
                            }
                        }).
                        into(previewImg);
            } else {
                setCameraMode(true);
            }

            Log.i(TAG, "+ init(): download url: " + savedFileUrl);
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
            // 사진 촬영
            case R.id.takePhotoBtn:
                takePhoto();
                break;
            // 사진촬영 모드 변경
            case R.id.showCameraBtn:
                setCameraMode(true);
                break;
            // 갤러리
            case R.id.galleryBtn:
                dispatchPicImageIntent();
                break;
            // 저장
            case R.id.saveBtn:
                setPhotoResult();
                break;
        }
    }

    private void setPhotoResult() {
        if (args == null || uploadFile == null) {
            finish();
            return;
        }

        Intent data = new Intent();
        args.putString("filePath", uploadFile.getPath());
        data.putExtras(args);
        setResult(RESULT_SAVED_PAPER, data);
        finish();
    }

    private boolean isCameraMode = false;

    private void setCameraMode(boolean cameraMode) {
        if (cameraMode) {
            ((TextView) findViewById(R.id.guideTxt)).setText("초점이 맞춰지도록 놓아주세요");
            cameraContainer.setVisibility(View.VISIBLE);
            previewImg.setVisibility(View.INVISIBLE);
            findViewById(R.id.takePhotoBtn).setVisibility(View.VISIBLE);
            findViewById(R.id.showCameraBtn).setVisibility(View.INVISIBLE);

            if (mCamera != null)
                mCamera.startPreview();

            isCameraMode = true;
        } else {
            ((TextView) findViewById(R.id.guideTxt)).setText("초점이 맞춰지도록 놓아주세요");
            cameraContainer.setVisibility(View.INVISIBLE);
            previewImg.setVisibility(View.VISIBLE);
            findViewById(R.id.takePhotoBtn).setVisibility(View.INVISIBLE);
            findViewById(R.id.showCameraBtn).setVisibility(View.VISIBLE);

            if (mCamera != null)
                mCamera.stopPreview();

            isCameraMode = false;
        }
    }

    private void takePhoto() {
        mCamera.takePicture(mShutterCallback, null, mPictureCallback);
    }

    private int curCameraId = 0; // 후면 카메라
    //private int curCameraId = 1; //  전면 카메라

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Camera c = null;

        try {
            // 디바이스의 카메라 기능 보유 여부 체크
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
                return null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(LibApp.get(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                    return null;
            }

            c = Camera.open(curCameraId); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }

        return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    /**
     * A basic Camera preview class
     */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        private Camera mCamera;
        private SurfaceHolder mHolder;

        public CameraPreview(Context context, Camera camera) {
            super(context);

            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);

            // deprecated setting, but required on Android versions prior to 3.0
            //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            Log.e(TAG, "+ surfaceCreated(): ");

            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                //mCamera.startPreview();
            } catch (IOException e) {
                Log.e(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.e(TAG, "+ surfaceDestroyed(): ");

            // empty. Take care of releasing the Camera preview in your activity.
            mCamera.stopPreview();
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            Log.e(TAG, "+ surfaceChanged(): WxH=" + w + "x" + h);

            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            Camera.Parameters params = mCamera.getParameters();

            List<String> focusModes = params.getSupportedFocusModes();

            // 계속 초점 잡기
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

            //if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            //    // Autofocus mode is supported
            //    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //}
            //else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            //    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            //}

            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            //    mCamera.enableShutterSound(true);

            List<Camera.Size> previewSizeList = mCamera.getParameters().getSupportedPreviewSizes();

            for (Camera.Size size : previewSizeList) {
                //Log.i(TAG, "+ getCameraInstance(): supportedPreviewSizes=" + size.width + "x" + size.height);

                // maxPreviewSizeW와 가장 유사한 값이 나올 때까지 looping
                if (size.width >= maxPreviewSizeW) {
                    previewSize = size;
                }
            }

            List<Camera.Size> pictureSizeList = mCamera.getParameters().getSupportedPictureSizes();
            for (Camera.Size size : pictureSizeList) {
                //Log.i(TAG, "+ getCameraInstance(): supportedPictureSizes=" +size.width + "x" + size.height);
            }

            if (previewSize != null) {
                Log.e(TAG, "+ surfaceChanged(): previewSize=" + previewSize.width + "x" + previewSize.height);

                // preview 사이즈 설정
                params.setPreviewSize(previewSize.width, previewSize.height);

                // 촬영 해상도 설정
                params.setPictureSize(previewSize.width, previewSize.height);
            }

            // 카메라 수직으로 촬영
            params.setRotation(90);

            // preview 화면 수직으로 변경
            mCamera.setDisplayOrientation(90);

            mCamera.setParameters(params);

            // start preview with new settings
            try {
                //mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

    }

    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            Log.e(TAG, "+ onAutoFocus(): success=" + success);
            if (success) {
                mCamera.takePicture(mShutterCallback, null, mPictureCallback);
            }
        }
    };

    private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            Log.e(TAG, "+ onShutter(): ");
        }
    };

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            setCameraMode(false);

            try {
                //File dir = getCacheDir();
                // TEST
                File dir = Key.getDebugStorage();

                String fileName = "";
                if (args != null)
                    fileName = dir + File.separator + args.getString("fileParamName", "") + ".jpg";
                else
                    fileName = dir + File.separator + "YTMS_PAPER.jpg";

                uploadFile = new File(fileName);
                if (uploadFile == null) {
                    Log.e(TAG, "+ onPictureTaken(): Error creating uploadFile, check storage permissions: ");
                    return;
                } else {
                    Log.e(TAG, "+ onPictureTaken(): taken pic data=" + TextUtil.formatFileSize(data.length));
                }

                FileOutputStream uploadFos = new FileOutputStream(uploadFile);
                uploadFos.write(data);
                //uploadFos.close(); // 파일 축소 후 닫기
                Log.e(TAG, "+ onPictureTaken(): taken pic size=" + TextUtil.formatFileSize(uploadFile.length()));

                int takenPicDegree = 0;

                // 폰모델에 따라 촬영된 사진 각도가 다름
                // G5:                  ORIENTATION_UNDEFINED(0)
                // 삼성, G pro:      ORIENTATION_ROTATE_90(6)
                ExifInterface exif = new ExifInterface(uploadFile.getPath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Log.e(TAG, "+ onPictureTaken(): picFile orientation=" + orientation);
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
                    takenPicDegree = 90;

                // 촬영 사진 원본 bitmap
                Bitmap takenBmp = BitmapFactory.decodeFile(uploadFile.getPath());

                // preview Bitmap
                Bitmap previewBmp = Bitmap.createBitmap(mCameraPreview.getWidth(), mCameraPreview.getHeight(), takenBmp.getConfig());
                Log.e(TAG, "+ onPictureTaken(): previewBmp size=" + previewBmp.getWidth() + "x" + previewBmp.getHeight());
                Canvas previewCanvas = new Canvas(previewBmp);

                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(curCameraId, info);

                /** 후면 카메라로 촬영된 사진 회전 처리 및 사진 붙이기 **/
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    // G5
                    if (takenPicDegree == 0) {
                        Rect picRect = new Rect(0, 0, mCameraPreview.getWidth(), mCameraPreview.getHeight());
                        previewCanvas.drawBitmap(takenBmp, null, picRect, null); // Rect 미지정시 mCameraPreview 화면에 꽉 차지 않고, 촬영 사진 원본 크기로 draw 됨
                    }
                    //삼성, G pro
                    else {
                        Matrix picMatrix = new Matrix();
                        picMatrix.setRotate(takenPicDegree, (float) takenBmp.getWidth() / 2, (float) takenBmp.getHeight() / 2);
                        takenBmp = Bitmap.createBitmap(takenBmp, 0, 0, takenBmp.getWidth(), takenBmp.getHeight(), picMatrix, true);
                        previewCanvas.drawBitmap(takenBmp, new Matrix(), null);
                    }
                }
                /** 전면 카메라로 촬영된 사진 회전 처리 및 사진 붙이기 **/
                else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    Matrix picMatrix = new Matrix();

                    // 전방 카메라 촬영된 미러링 된 사진을 원래 각도로 변경
                    float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
                    Matrix matrixMirrorY = new Matrix();
                    matrixMirrorY.setValues(mirrorY);

                    picMatrix.postConcat(matrixMirrorY);
                    picMatrix.preRotate(takenPicDegree + 180);
                    takenBmp = Bitmap.createBitmap(takenBmp, 0, 0, takenBmp.getWidth(), takenBmp.getHeight(), picMatrix, true);

                    Rect picRect = new Rect(0, 0, mCameraPreview.getWidth(), mCameraPreview.getHeight());
                    previewCanvas.drawBitmap(takenBmp, null, picRect, null); // Rect 미지정시 mCameraPreview 화면에 꽉 차지 않고, 촬영 사진 원본 크기로 draw 됨
                }

                // preview 사진을 결과 창에 붙이기
                previewImg.setImageBitmap(previewBmp);

                /* 파일로 저장  */

                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(uploadFile.getPath(), bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                // 업로드 사진을 1024x768 사이즈로 제한
                int targetW = 1024;
                int targetH = 768;
                int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                //    bmOptions.inPurgeable = true;

                Bitmap uploadBmp = BitmapFactory.decodeFile(uploadFile.getPath(), bmOptions);
                // CAUTION 100으로 지정 시, 용량이 더 커지므로 100 미만 적용 / 생성된 파일은 폰 디렉토리 APP로 확인 가능.
                uploadBmp.compress(Bitmap.CompressFormat.JPEG, 80, uploadFos);
                uploadFos.close();

                Log.e(TAG, "+ onPictureTaken(): uploadFile=" + uploadFile.getPath() + " / " + TextUtil.formatFileSize(uploadFile.length()));

                notifyNewFile(uploadFile.getPath());
            } catch (Exception e) {
                Log.e(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * Create a file Uri for saving an image or video
     */
    private Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        //File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
        //// This location works best if you want the created images to be shared
        //// between applications and persist after your app has been uninstalled.
        //
        //// Create the storage directory if it does not exist
        //if (!mediaStorageDir.exists()) {
        //    if (!mediaStorageDir.mkdirs()) {
        //        Log.d("MyCameraApp", "failed to create directory");
        //        return null;
        //    }
        //}

        File mediaStorageDir = getCacheDir();
        // TEST
        if (MyApp.onDebug)
            mediaStorageDir = Key.getDebugStorage();

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
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

    private void setPreviewPhoto(final String photoPath) {
        try {
            if (photoPath == null || photoPath.isEmpty())
                return;

            //File dir = getCacheDir();
            // TEST
            File dir = Key.getDebugStorage();

            String fileName = "";
            if (args != null)
                fileName = dir + File.separator + args.getString("fileParamName", "") + ".jpg";
            else
                fileName = dir + File.separator + "YTMS_PAPER.jpg";

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

            setCameraMode(false);

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

    private String[] reduceUploadFileSize(@NonNull String filePath) {
        try {
            File file = new File(filePath);
            long fileSize = file.length();
            Log.e(TAG, "+ reduceUploadFileSize(): fileSize=" + fileSize + "(" + TextUtil.formatFileSize(fileSize) + ")");
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);

            // 업로드 사진 1024x768 사이즈로 제한
            Bitmap bmp = ImageResizeUtil.decodeSampledBitmapFromFile(filePath, 1024, 768);

            //File storageDir = getCacheDir();
            // TEST
            File storageDir = Key.getDebugStorage();

            File resizedFile = new File(storageDir + "/" + "upload_" + fileName);
            FileOutputStream fos = new FileOutputStream(resizedFile);
            // CAUTION 100으로 지정 시, 용량이 더 커지므로 100 미만 적용 / 생성된 파일은 폰 디렉토리 APP로 확인 가능.
            bmp.compress(Bitmap.CompressFormat.PNG, 80, fos);
            fos.flush();
            fos.close();

            filePath = resizedFile.getPath();
            long resizedFileSize = resizedFile.length();
            Log.e(TAG, "+ reduceUploadFileSize(): resized filePath=" + filePath);
            Log.e(TAG, "+ reduceUploadFileSize(): resizedFileSize=" + resizedFileSize + "(" + TextUtil.formatFileSize(resizedFileSize) + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[] {filePath};
    }

    private void notifyNewFile(String photoPath) {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(photoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
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
                    String photoPath = ImageFilePath.getPath(this, selectedFileUri);
                    // CAUTION "raw:/ 로 시작하는 디렉토리 인식 못함 ex) "raw:/storage/emulated/0/Download/Test_Pic/pic3.jpg"
                    Log.e(TAG, "+ onActivityResult(): photoPath: " + photoPath);
                    setPreviewPhoto(photoPath);
                } else {
                    finish();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}