package com.ke.screencapture;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.MEDIA_PROJECTION_SERVICE;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ScreenCaptureManager {
    private static final String TAG = ScreenCaptureManager.class.getSimpleName();
    public static final String ACTIVITY_RESULT_INTENT = "activityResultIntent";
    public static final String START_SCENE = "startScene";
    public static final String VIDEO_PATH = "videoPath";
    private static volatile ScreenCaptureManager mInstance;
    private WeakReference<Activity> mActivity;
    private List<ScreenCaptureListener> mScreenCaptureListenerList = new ArrayList<>();
    private String mVideoPath;
    private int mScene;

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

    /**
     * @param scene     ScreenCaptureConfig.SCREEN_CAPTURE_WITH_BITMAP 回调bitmap  ScreenCaptureConfig.SCREEN_CAPTURE_WITH_VIDEO 生成mp4文件
     * @param videoPath scene为SCREEN_CAPTURE_WITH_VIDEO时生成MP4的路径
     */
    public void startScreenCapture(int scene, String videoPath) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            onError(ErrorCode.ERROR_PERMISSION_LOW_VERSION_SYSTEM, ErrorMessage.ERROR_PERMISSION_LOW_VERSION_SYSTEM);
            return;
        }
        Activity activity = getActivity();
        if (activity == null) return;
        mScene = scene;
        mVideoPath = videoPath;
        if (scene == ScreenCaptureConfig.SCREEN_CAPTURE_WITH_BITMAP || scene == ScreenCaptureConfig.SCREEN_CAPTURE_WITH_VIDEO) {
            Intent intent = new Intent(activity, ScreenCaptureActivity.class);
            intent.putExtra(START_SCENE, mScene);
            intent.putExtra(VIDEO_PATH, mVideoPath);
            activity.startActivity(intent);
        } else {
            onError(ErrorCode.ERROR_PARAMS_ILLEGAL_START, ErrorMessage.ERROR_PARAMS_ILLEGAL_START + " scene is " + mScene);
        }
    }

    public void stopScreenCapture() {
        Activity activity = getActivity();
        if (activity == null) return;
        if (mScene == ScreenCaptureConfig.SCREEN_CAPTURE_WITH_BITMAP) {
            ScreenCaptureProjection.getInstance().stopScreenCapture();
        } else if (mScene == ScreenCaptureConfig.SCREEN_CAPTURE_WITH_VIDEO) {
            ScreenCaptureRecorder.getInstance().stopScreenCapture();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ServiceUtil.isServiceRunning(getActivity().getApplicationContext(), ScreenCaptureService.class.getClass().getName())) {
            Intent intent = new Intent(activity, ScreenCaptureService.class);
            activity.stopService(intent);
        }


    }

    public void onActivityResult(int resultCode, Intent data) {
        Activity activity = getActivity();
        if (activity == null) return;
        if (resultCode == RESULT_OK) {
            if (mScene == ScreenCaptureConfig.SCREEN_CAPTURE_WITH_BITMAP) {
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
            } else if (mScene == ScreenCaptureConfig.SCREEN_CAPTURE_WITH_VIDEO) {
                ScreenCaptureRecorder.getInstance().initContext(activity);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent intent = new Intent(activity, ScreenCaptureService.class);
                    intent.putExtra(ACTIVITY_RESULT_INTENT, data);
                    activity.startForegroundService(intent);
                } else {
                    MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) activity.getSystemService(MEDIA_PROJECTION_SERVICE);
                    MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(RESULT_OK, data);
                    ScreenCaptureRecorder.getInstance().startScreenCapture(mediaProjection,ScreenCaptureConfig.SCREEN_CAPTURE_DEFAULT_BITRATE,mVideoPath);

                }
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
                screenCaptureListener.onScreenCaptureStopped(mVideoPath);
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
