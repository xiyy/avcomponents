package com.ke.screencapture;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.MEDIA_PROJECTION_SERVICE;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ScreenCaptureManager {
    private static final String TAG = ScreenCaptureManager.class.getSimpleName();
    public static final String ACTIVITY_RESULT_INTENT = "activityResultIntent";
    private static volatile ScreenCaptureManager mInstance;
    private WeakReference<Activity> mActivity;
    private List<ScreenCaptureListener> mScreenCaptureListenerList = new ArrayList<>();

    private ScreenCaptureManager(Activity activity) {
        mActivity = new WeakReference<>(activity);
    }

    public static ScreenCaptureManager getInstance(Activity context) {
        if (mInstance == null) {
            synchronized (ScreenCaptureManager.class) {
                if (mInstance == null) {
                    mInstance = new ScreenCaptureManager(context);
                }
            }
        }
        return mInstance;
    }

    public void registerListener(ScreenCaptureListener screenCaptureListener) {
        if (!mScreenCaptureListenerList.contains(screenCaptureListener)) {
            mScreenCaptureListenerList.add(screenCaptureListener);
        }

    }

    public void unregisterListener(ScreenCaptureListener screenCaptureListener) {
        if (mScreenCaptureListenerList.contains(screenCaptureListener)) {
            mScreenCaptureListenerList.remove(screenCaptureListener);
        }
    }

    public void startScreenCapture() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity, ScreenCaptureActivity.class);
            activity.startActivity(intent);
        }

    }

    public void stopScreenCapture() {
        Activity activity = getActivity();
        if (activity != null) {
            ScreenCaptureProjection.getInstance().stopScreenCapture();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent intent = new Intent(activity, ScreenCaptureService.class);
                activity.stopService(intent);
            }
        }


    }

    public void onActivityResult(int resultCode, Intent data) {
        Activity activity = getActivity();
        if (activity == null) return;
        if (resultCode == RESULT_OK) {
            ScreenCaptureProjection.getInstance().initContext(activity);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent intent = new Intent(activity, ScreenCaptureService.class);
                intent.putExtra(ACTIVITY_RESULT_INTENT, data);
                activity.startForegroundService(intent);
            } else {
                MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(MEDIA_PROJECTION_SERVICE);
                MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(RESULT_OK, data);
                ScreenCaptureProjection.getInstance().startScreenCapture(mediaProjection);
            }

        } else if (resultCode == RESULT_CANCELED) {
            onError(ErrorCode.ERROR_PERMISSION_DENIED, ErrorMessage.ERROR_PERMISSION_DENIED);
        }

    }

    public void onError(int errorCode, String errorMessage) {
        for (ScreenCaptureListener screenCaptureListener : mScreenCaptureListenerList) {
            if (screenCaptureListener != null) {
                screenCaptureListener.onScreenCaptureError(errorCode, errorMessage);
            }
        }

    }

    public void onScreenCaptureStarted() {
        for (ScreenCaptureListener screenCaptureListener : mScreenCaptureListenerList) {
            if (screenCaptureListener != null) {
                screenCaptureListener.onScreenCaptureStarted();
            }
        }
    }


    public void onScreenCaptureStopped() {
        for (ScreenCaptureListener screenCaptureListener : mScreenCaptureListenerList) {
            if (screenCaptureListener != null) {
                screenCaptureListener.onScreenCaptureStopped();
            }
        }
    }

    public void onScreenCaptureBitmap(Bitmap bitmap) {
        for (ScreenCaptureListener screenCaptureListener : mScreenCaptureListenerList) {
            if (screenCaptureListener != null) {
                screenCaptureListener.onScreenCaptureBitmap(bitmap);
            }
        }
    }

    private Activity getActivity() {
        Activity activity = mActivity.get();
        if (activity == null) {
            Log.w(TAG, "startScreenCapture activity weakReference is null");
            return null;
        }
        return activity;
    }

}
