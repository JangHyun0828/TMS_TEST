package com.neognp.dtmsapp_v1.car;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.Key;
import com.trevor.library.app.LibApp;
import com.trevor.library.template.BasicFragment;
import com.trevor.library.util.TextUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// Google tutorial> https://github.com/googlesamples/android-Camera2Basic/#readme
// https://myandroidarchive.tistory.com/6
public class ReceiptCameraFragment extends BasicFragment implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private final String fileName = "DTMS_RECEIPT.jpg";

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = ReceiptCameraFragment.class.getSimpleName();

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            //configureTransform(width, height);  // CAUTION AutoFitTextureView.onMeasure()안에서 1:1 square preview 세팅하기 때문에 호출 금지
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView textureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.

            /*
            CAUTION
             mCameraOpenCloseLock.release()를 createCameraPreviewSession() 보다 먼저 호출할 경우
             java.lang.IllegalStateException: CameraDevice was already closed  에러 발생
            ->
            createCameraPreviewSession() 안에서 맨 마지막에 호출하도록 변경
            this Semaphore should be released after the camera createCaptureSession action to prevent resource conflicts between multiple threads.
            The source code releases the semaphore too early just after camera opened. Pausing the app will close the camera which causes the error.
            To test it, call finish() before mCameraOpenCloseLock.release(), you will not see the error
            https://stackoverflow.com/questions/42749056/camera2-api-and-java-lang-illegalstateexception
             */
            //mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));

            // CAUTION 에러 발생 대비 try-catch 처리: java.lang.IllegalStateException: maxImages (2) has already been acquired, call #close before acquiring more.
            try {
                Image image = reader.acquireNextImage();
                File file = saveToFile(image);

                if (file != null)
                    ((ReceiptCameraActivity) getActivity()).onPictureTaken(file.getAbsolutePath());
                unlockFocus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPreCaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE:
                    {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED)
                    {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE:
                    {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE)
                    {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult)
        {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result)
        {
            process(result);
        }

    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text)
    {
        final Activity activity = getActivity();
        if (activity != null)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio)
    {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight && option.getHeight() == option.getWidth() * h / w)
            {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight)
                {
                    bigEnough.add(option);
                }
                else
                {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else if (notBigEnough.size() > 0)
        {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        }
        else
        {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Fragment newInstance() {
        return new ReceiptCameraFragment();
    }

    private ImageButton takePictureBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera2_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        // contentView = view;

        textureView = view.findViewById(R.id.textureView);

        // view.findViewById(R.id.galleryBtn).setOnClickListener(this);

        // takePictureBtn = view.findViewById(R.id.takePictureBtn);
        // takePictureBtn.setOnClickListener(this);

        // view.findViewById(R.id.lensFacingBtn).setOnClickListener(this);

        //view.findViewById(R.id.info).setOnClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFile = new File(getActivity().getExternalFilesDir(null), "pic.jpg");
        // TEST
        // mFile = new File(Key.getAppStorage(), Key.FILE_FEED_SRC);
    }

    //@Override
    //public void onResume() {
    //    super.onResume();
    //
    //    startBackgroundThread();
    //
    //    // When the screen is turned off and turned back on, the SurfaceTexture is already
    //    // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
    //    // a camera and start preview from here (otherwise, we wait until the surface is ready in
    //    // the SurfaceTextureListener).
    //    if (textureView.isAvailable()) {
    //        openCamera(textureView.getWidth(), textureView.getHeight());
    //    } else {
    //        textureView.setSurfaceTextureListener(mSurfaceTextureListener);
    //    }
    //}

    @Override
    public void onResume() {
        super.onResume();

        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        // mOnImageAvailableListener.onImageAvailable()에서 unlockFocus 호출 이후에 촬영 버튼 활성화
        if (mState == STATE_PREVIEW)
            onTakingPicture = false;
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    // CAUTION
    //  Fragment visible/hidden 호출시 아래 처리를 하지 않으면 back key로 CameraFragment 복귀 후 촬영 시, 사진이 2번 찍히는 현상 발생
    //@Override
    //public void onHiddenChanged(boolean hidden) {
    //    // Fragment 화면에 표시된 직후 호출
    //    if (!hidden) {
    //        startBackgroundThread();
    //
    //        if (textureView.isAvailable()) {
    //            openCamera(textureView.getWidth(), textureView.getHeight());
    //        }
    //    } else {
    //        closeCamera();
    //        stopBackgroundThread();
    //    }
    //}

    //private void requestCameraPermission() {
    //    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
    //        new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
    //    } else {
    //        requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    //    }
    //}
    //
    //@Override
    //public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    //    if (requestCode == REQUEST_CAMERA_PERMISSION) {
    //        if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
    //            ErrorDialog.newInstance(getString(R.string.request_permission)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
    //        }
    //    } else {
    //        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    //    }
    //}

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings ("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Log.e(TAG, "+ setUpCameraOutputs(): width x height= " + width + "x" + height);
        Log.e(TAG, "+ setUpCameraOutputs(): textureView size= " + textureView.getWidth() + "x" + textureView.getHeight());

        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                if (facing != null && facing != mCameraLensFacingDirection) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                Log.e(TAG, "+ setUpCameraOutputs(): largest= " + largest.getWidth() + "x" + largest.getHeight());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
                //mImageReader = ImageReader.newInstance(Key.SIZE_FEED_IMAGE, Key.SIZE_FEED_IMAGE, ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                Log.e(TAG, "+ setUpCameraOutputs(): screen size= " + displaySize.x + "x" + displaySize.y);

                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }
                // 1:1 square size로 고정
                //maxPreviewWidth = width;
                //maxPreviewHeight = width;

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest);
                Log.e(TAG, "+ setUpCameraOutputs(): mPreviewSize=" + mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                //if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //    textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                //} else {
                //    textureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                //}
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight(), true);
                } else {
                    textureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth(), true);
                }

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                Log.e(TAG, "+ setUpCameraOutputs(): mCameraId=" + mCameraId);

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the camera specified by {@link ReceiptCameraFragment#mCameraId}.
     */
    @SuppressLint ("MissingPermission")
    private void openCamera(int width, int height) {
        //if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        //    requestCameraPermission();
        //    return;
        //}

        //if (host == null || !host.hasPermissions()) {
        //    return;
        //}

        setUpCameraOutputs(width, height);
        //configureTransform(width, height); // CAUTION AutoFitTextureView.onMeasure()안에서 1:1 square preview 세팅하기 때문에 호출 금지
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try
        {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    int mCameraLensFacingDirection = 1; // 후면 카메라를 기본 활성화

    private void switchCamera() {
        if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_BACK) {
            mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_FRONT;
            closeCamera();
            reopenCamera();

        } else if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
            mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_BACK;
            closeCamera();
            reopenCamera();
        }
    }

    //public static final String CAMERA_FRONT = "1";
    //public static final String CAMERA_BACK = "0";
    //
    ///*
    //카메라 후면/전면 촬영 변경
    // */
    //public void switchCamera() {
    //    if (mCameraId.equals(CAMERA_FRONT)) {
    //        mCameraId = CAMERA_BACK;
    //        closeCamera();
    //        reopenCamera();
    //    } else if (mCameraId.equals(CAMERA_BACK)) {
    //        mCameraId = CAMERA_FRONT;
    //        closeCamera();
    //        reopenCamera();
    //    }
    //}

    private void reopenCamera() {
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }

                    // When the session is ready, we start displaying the preview.
                    mCaptureSession = cameraCaptureSession;
                    try {
                        // Auto focus should be continuous for camera preview.
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // Flash is automatically enabled when necessary.
                        setAutoFlash(mPreviewRequestBuilder);

                        // Finally, we start displaying the camera preview.
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);

                        mCameraOpenCloseLock.release();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    showToast("Failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == textureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(), (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    boolean onTakingPicture = false;

    @Override
    public void onClick(View view) {
        // switch (view.getId()) {
        //     case R.id.galleryBtn:
        //         ((IoniqRunCameraHost) getActivity()).onGalleryRequested();
        //         break;
        //     case R.id.takePictureBtn:
        //         takePicture();
        //         break;
        //     case R.id.lensFacingBtn:
        //         switchCamera();
        //         break;
        // }
    }

    /**
     * Initiate a still image capture.
     */
    public synchronized void takePicture() {
        if (onTakingPicture)
            return;

        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // 촬영 버튼 누른 이후 연타 방지
            onTakingPicture = true;

            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    //private void unlockFocus() {
    //    try {
    //        // Reset the auto-focus trigger
    //        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
    //        setAutoFlash(mPreviewRequestBuilder);
    //        mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
    //        // After this, the camera will go back to the normal state of preview.
    //        mState = STATE_PREVIEW;
    //        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
    //    } catch (CameraAccessException e) {
    //        e.printStackTrace();
    //    }
    //}
    private void unlockFocus()
    {
        try
        {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            //mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            //mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Run the preCapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPreCaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Log.d(TAG, mFile.toString());
                    //unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);

            // 사진 촬영음 재생
            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.SHUTTER_CLICK);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        // 플래시 기능 disable
        //if (mFlashSupported) {
        //    requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        //}
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;

        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            Log.e(TAG, "+ ImageSaver.run(): ");

            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            //mImage.close();

            //FileOutputStream output = null;
            try {
                //output = new FileOutputStream(mFile);
                //output.write(bytes);
                saveToFile(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mImage.close();

                //if (null != output) {
                //    try {
                //        output.close();
                //    } catch (IOException e) {
                //        e.printStackTrace();
                //    }
                //}
                //
                //FileUtil.notifyNewFile(mFile.getAbsolutePath(), "image/jpeg");
            }
        }
    }

    private synchronized void saveToFile(byte[] data) {
        try {
            File srcFile = new File(Key.getDebugStorage(), fileName);
            FileOutputStream srcFos = new FileOutputStream(srcFile);
            srcFos.write(data);
            srcFos.close();
            Log.e(TAG, "+ saveToFile(): srcFile length=" + TextUtil.formatFileSize(srcFile.length()));

            int srcPicDegree = 0;

            // 폰모델에 따라 촬영된 사진 각도가 다름
            // G5:                  ORIENTATION_UNDEFINED(0)
            // 삼성, G pro:      ORIENTATION_ROTATE_90(6)
            ExifInterface exif = new ExifInterface(srcFile.getPath());
            //int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int orientation = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
            Log.e(TAG, "+ saveToFile(): srcFile orientation=" + orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    srcPicDegree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    srcPicDegree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    srcPicDegree = 270;
                    break;
            }

            Log.e(TAG, "+ saveToFile(): srcPicDegree=" + srcPicDegree);

            BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
            bmpOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(srcFile.getPath(), bmpOptions);
            Log.e(TAG, "+ saveToFile(): srcFile size= " + bmpOptions.outWidth + "x" + bmpOptions.outHeight);

            //  업로드용 사진을 1080 x 1080 이하로 제한
            //int value1 = (int) Math.ceil((double) bmpOptions.outWidth / Key.SIZE_FEED_IMAGE);
            //int value2 = (int) Math.ceil((double) bmpOptions.outHeight / Key.SIZE_FEED_IMAGE);
            //bmpOptions = new BitmapFactory.Options();
            //bmpOptions.inSampleSize = Math.max(value1, value2);
            //Log.e(TAG, "+ saveToFile(): scaleFactor=" + bmpOptions.inSampleSize);
            // 촬영 사진 축소 bitmap
            //Bitmap srcBmp = BitmapFactory.decodeFile(srcFile.getPath(), bmpOptions);

            // 촬영 사진 원본 bitmap
            Bitmap srcBmp = BitmapFactory.decodeFile(srcFile.getPath());

            // 보정 Bitmap
            Bitmap adjustBmp = Bitmap.createBitmap(srcBmp.getWidth(), srcBmp.getHeight(), srcBmp.getConfig());
            Log.e(TAG, "+ saveToFile(): adjustBmp size=" + adjustBmp.getWidth() + "x" + adjustBmp.getHeight());

            Canvas previewCanvas = new Canvas(adjustBmp);
            adjustBmp = adjustPhotoRotation(srcPicDegree, srcBmp, previewCanvas);

            File adjustFile = new File(Key.getDebugStorage(), fileName);
            FileOutputStream adjustFos = new FileOutputStream(adjustFile);
            adjustBmp.compress(Bitmap.CompressFormat.JPEG, 90, adjustFos);  // CAUTION 100으로 지정 시, 용량이 더 커지므로 100 미만 적용
            adjustFos.close();
            Log.e(TAG, "+ saveToFile(): adjustFile=" + adjustFile.getPath() + " / " + TextUtil.formatFileSize(adjustFile.length()));

            // TODO 삭제
            notifyNewFile(adjustFile.getPath(), "image/jpeg");

            Bitmap finalAdjustBmp = adjustBmp;
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    ((ReceiptCameraActivity) getActivity()).onPictureTaken(adjustFile.getAbsolutePath());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized File saveToFile(Image image) {
        try {
            if (image == null)
                return null;

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            File srcFile = new File(Key.getDebugStorage(), fileName);
            FileOutputStream srcFos = new FileOutputStream(srcFile);
            srcFos.write(bytes);
            srcFos.close();
            Log.e(TAG, "+ saveToFile(): srcFile length=" + TextUtil.formatFileSize(srcFile.length()));

            int srcPicDegree = 0;

            // 폰모델에 따라 촬영된 사진 각도가 다름
            // G5:          ORIENTATION_UNDEFINED(0)
            // 삼성, G pro: ORIENTATION_ROTATE_90(6)
            ExifInterface exif = new ExifInterface(srcFile.getPath());
            //int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int orientation = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
            Log.e(TAG, "+ saveToFile(): srcFile orientation=" + orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    srcPicDegree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    srcPicDegree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    srcPicDegree = 270;
                    break;
            }

            Log.e(TAG, "+ saveToFile(): srcPicDegree=" + srcPicDegree);

            BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
            bmpOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(srcFile.getPath(), bmpOptions);
            Log.e(TAG, "+ saveToFile(): srcFile size= " + bmpOptions.outWidth + "x" + bmpOptions.outHeight);

            // 업로드용 사진을 1080 x 1080 이하로 제한
            //int value1 = (int) Math.ceil((double) bmpOptions.outWidth / Key.SIZE_FEED_IMAGE);
            //int value2 = (int) Math.ceil((double) bmpOptions.outHeight / Key.SIZE_FEED_IMAGE);
            //bmpOptions = new BitmapFactory.Options();
            //bmpOptions.inSampleSize = Math.max(value1, value2);
            //Log.e(TAG, "+ saveToFile(): scaleFactor=" + bmpOptions.inSampleSize);
            // 촬영 사진 축소 bitmap
            //Bitmap srcBmp = BitmapFactory.decodeFile(srcFile.getPath(), bmpOptions);

            // 촬영 사진 원본 bitmap
            Bitmap srcBmp = BitmapFactory.decodeFile(srcFile.getPath());

            // 보정 Bitmap
            Bitmap adjustBmp = Bitmap.createBitmap(srcBmp.getWidth(), srcBmp.getHeight(), srcBmp.getConfig());
            Log.e(TAG, "+ saveToFile(): adjustBmp size=" + adjustBmp.getWidth() + "x" + adjustBmp.getHeight());

            Canvas previewCanvas = new Canvas(adjustBmp);
            adjustBmp = adjustPhotoRotation(srcPicDegree, srcBmp, previewCanvas);

            File adjustFile = new File(Key.getDebugStorage(), fileName);
            FileOutputStream adjustFos = new FileOutputStream(adjustFile);
            adjustBmp.compress(Bitmap.CompressFormat.JPEG, 90, adjustFos);  // CAUTION 100으로 지정 시, 용량이 더 커지므로 100 미만 적용
            adjustFos.close();
            Log.e(TAG, "+ saveToFile(): adjustFile=" + adjustFile.getPath() + " / " + TextUtil.formatFileSize(adjustFile.length()));

            // TODO 삭제
            notifyNewFile(adjustFile.getPath(), "image/jpeg");

            return adjustFile;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // TODO 삭제
    public static void notifyNewFile(String photoPath, String MIME_TYPE) {
        Intent mediaScanIntent = new Intent ("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File (photoPath);
        Uri contentUri = Uri.fromFile (f);
        mediaScanIntent.setData (contentUri);
        LibApp.get ().sendBroadcast (mediaScanIntent);

        //ContentValues values = new ContentValues(2);
        //values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); // ex "image/jpeg" , image/png"
        //values.put(MediaStore.Images.Media.DATA,photoPath);
        //Uri contentUriFile = LibApp.get().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private Bitmap adjustPhotoRotation(int takenPicDegree, Bitmap bmp, Canvas canvas) {
        // 후면 카메라로 촬영된 사진 회전 처리
        if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_BACK) {
            // G5
            if (takenPicDegree == 0) {
                Rect picRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
                canvas.drawBitmap(bmp, null, picRect, null); // Rect 미지정시 textureView 화면에 꽉 차지 않고, 촬영 사진 원본 크기로 draw 됨
            }
            // 삼성, G pro
            else {
                Matrix picMatrix = new Matrix();
                picMatrix.setRotate(takenPicDegree, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), picMatrix, true);
                canvas.drawBitmap(bmp, new Matrix(), null);
            }
        }
        // 전면 카메라로 촬영된 사진 회전 처리
        else if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
            Matrix picMatrix = new Matrix();

            // 전방 카메라 촬영된 미러링 된 사진을 원래 각도로 변경
            float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
            Matrix matrixMirrorY = new Matrix();
            matrixMirrorY.setValues(mirrorY);

            picMatrix.postConcat(matrixMirrorY);
            //picMatrix.preRotate(takenPicDegree + 180);
            picMatrix.preRotate(takenPicDegree);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), picMatrix, true);

            Rect picRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.drawBitmap(bmp, null, picRect, null); // Rect 미지정시 textureView 화면에 꽉 차지 않고, 촬영 사진 원본 크기로 draw 됨
        }

        return bmp;
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity).setMessage(getArguments().getString(ARG_MESSAGE)).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    activity.finish();
                }
            }).create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    //public static class ConfirmationDialog extends DialogFragment {
    //
    //    @NonNull
    //    @Override
    //    public Dialog onCreateDialog(Bundle savedInstanceState) {
    //        final Fragment parent = getParentFragment();
    //        return new AlertDialog.Builder(getActivity()).setMessage(R.string.request_permission).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    //            @Override
    //            public void onClick(DialogInterface dialog, int which) {
    //                parent.requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    //            }
    //        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
    //            @Override
    //            public void onClick(DialogInterface dialog, int which) {
    //                Activity activity = parent.getActivity();
    //                if (activity != null) {
    //                    activity.finish();
    //                }
    //            }
    //        }).create();
    //    }
    //}

}
