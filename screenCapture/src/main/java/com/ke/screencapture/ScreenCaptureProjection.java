package com.ke.screencapture;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class ScreenCaptureProjection {
    private static final String TAG = ScreenCaptureProjection.class.getSimpleName();
    private static volatile ScreenCaptureProjection mInstance;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private WeakReference<Activity> mActivity;

    public void startScreenCapture(MediaProjection mediaProjection) {
        mMediaProjection = mediaProjection;
        createVirtualDisplay();
    }

    public void initContext(Activity activity) {
        mActivity = new WeakReference<>(activity);
    }

    private void createVirtualDisplay() {
        Activity activity = getActivity();
        if (activity == null) return;
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int density = metrics.densityDpi;
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getRealSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        mImageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        AtomicInteger index = new AtomicInteger();
        mImageReader.setOnImageAvailableListener(imageReader -> {
            Image image = imageReader.acquireNextImage();
            if (image != null) {
                index.getAndIncrement();
                if (index.get() == 1) {
                    ScreenCaptureManager.getInstance(activity).onScreenCaptureStarted();
                }
                int width = image.getWidth();
                int height = image.getHeight();
                Log.d(TAG, "image width:" + width + " height:" + height);
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int padding = (rowStride - pixelStride * width) / pixelStride;
                Bitmap bitmap;
                bitmap = Bitmap.createBitmap(width + padding, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                ScreenCaptureManager.getInstance(activity).onScreenCaptureBitmap(bitmap);
                image.close();
            }
        }, null);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(ScreenCaptureProjection.class.getSimpleName(), screenWidth, screenHeight, density, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mImageReader.getSurface(), null, null);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
    }

    public void stopScreenCapture() {
        mMediaProjection.stop();
    }

    private ScreenCaptureProjection() {

    }

    public static ScreenCaptureProjection getInstance() {
        if (mInstance == null) {
            synchronized (ScreenCaptureProjection.class) {
                if (mInstance == null) {
                    mInstance = new ScreenCaptureProjection();
                }
            }
        }
        return mInstance;
    }

    private MediaProjection.Callback mMediaProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            super.onStop();
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
            }
            if (mImageReader != null) {
                mImageReader.close();
            }
            if (mMediaProjection != null) {
                mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            }
            Activity activity = getActivity();
            if (activity != null) {
                ScreenCaptureManager.getInstance(activity).onScreenCaptureStopped();
                Log.d(TAG, "MediaProjection onStop");
            }

        }
    };

    private Activity getActivity() {
        if (mActivity != null) {
            return mActivity.get();
        }
        return null;
    }
}
