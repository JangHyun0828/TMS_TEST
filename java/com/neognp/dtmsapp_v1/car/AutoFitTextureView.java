package com.neognp.dtmsapp_v1.car;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitTextureView extends TextureView {

    String TAG = AutoFitTextureView.class.getSimpleName();

    //private int mRatioWidth = 0;
    //private int mRatioHeight = 0;
    //
    //public AutoFitTextureView(Context context) {
    //    this(context, null);
    //}
    //
    //public AutoFitTextureView(Context context, AttributeSet attrs) {
    //    this(context, attrs, 0);
    //}
    //
    //public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
    //    super(context, attrs, defStyle);
    //}
    //
    ///**
    // * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
    // * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
    // * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
    // *
    // * @param width  Relative horizontal size
    // * @param height Relative vertical size
    // */
    //public void setAspectRatio(int width, int height) {
    //    if (width < 0 || height < 0) {
    //        throw new IllegalArgumentException("Size cannot be negative.");
    //    }
    //    mRatioWidth = width;
    //    mRatioHeight = height;
    //    requestLayout();
    //}
    //
    //@Override
    //protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    //    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    //
    //    int width = MeasureSpec.getSize(widthMeasureSpec);
    //    int height = MeasureSpec.getSize(heightMeasureSpec);
    //    if (0 == mRatioWidth || 0 == mRatioHeight) {
    //        setMeasuredDimension(width, height);
    //    } else {
    //        if (width < height * mRatioWidth / mRatioHeight) {
    //            setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
    //        } else {
    //            setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
    //        }
    //    }
    //}

    /*
    1:1 square preview 만들기
    https://stackoverflow.com/questions/34638651/how-to-get-an-android-camera2-with-11-ratio-like-instagram
     */

    private int mCameraWidth = 0;  // mPreviewSize width
    private int mCameraHeight = 0; // mPreviewSize height
    private boolean mSquarePreview = false;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAspectRatio(int width, int height, boolean squarePreview) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mCameraWidth = width;
        mCameraHeight = height;
        mSquarePreview = squarePreview;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        Log.e(TAG, "+ onMeasure(): width x height= " + width + "x" + height);

        //if (0 == mCameraWidth || 0 == mCameraHeight) {
        //    setMeasuredDimension(width, height);
        //} else {
        //    /**
        //     * Vertical orientation
        //     */
        //    if (width < height) {
        //        if (mSquarePreview) {
        //            setTransform(squareTransform(width, height));
        //            setMeasuredDimension(width, width);
        //        } else {
        //            setMeasuredDimension(width, width * mCameraHeight / mCameraWidth);
        //        }
        //    }
        //    /**
        //     * Horizontal orientation
        //     */
        //    else {
        //        if (mSquarePreview) {
        //            setTransform(squareTransform(width, height));
        //            setMeasuredDimension(height, height);
        //        } else {
        //            setMeasuredDimension(height * mCameraWidth / mCameraHeight, height);
        //        }
        //    }
        //}

        setTransform(squareTransform(width, width));
        setMeasuredDimension(width, width);
    }

    private Matrix setupTransform(int sw, int sh, int dw, int dh) {
        Matrix matrix = new Matrix();
        RectF src = new RectF(0, 0, sw, sh);
        RectF dst = new RectF(0, 0, dw, dh);
        RectF screen = new RectF(0, 0, dw, dh);

        matrix.postRotate(-90, screen.centerX(), screen.centerY());
        matrix.mapRect(dst);

        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
        matrix.mapRect(src);

        matrix.setRectToRect(screen, src, Matrix.ScaleToFit.FILL);
        matrix.postRotate(-90, screen.centerX(), screen.centerY());

        return matrix;
    }

    private Matrix squareTransform(int viewWidth, int viewHeight) {
        Matrix matrix = new Matrix();

        //if (viewWidth < viewHeight) {
        //    //MyLogger.log(AutoFitTextureView.class, "Horizontal");
        //    matrix.setScale(1, (float) mCameraHeight / (float) mCameraWidth, viewWidth / 2, viewHeight / 2);
        //} else {
        //    //MyLogger.log(AutoFitTextureView.class, "Vertical");
        //    matrix.setScale((float) mCameraHeight / (float) mCameraWidth, 1, viewWidth / 2, viewHeight / 2);
        //}

        // 1:1 view 사이즈에 맞게끔 컨텐츠 비율의 높이를 축소
        // Galaxy 10> 1 : 0.75     : 50% : 50%  /  mPreviewSize : 1440x1080 / mTextureView: 1080x1080
        // G5>          1 : 0.5625  : 50% : 50%  / mPreviewSize  : 1920x1080 / mTextureView: 1440x1440
        matrix.setScale(1, (float) mCameraHeight / (float) mCameraWidth, viewWidth / 2, viewHeight / 2);

        return matrix;
    }

}
