package com.ke.avcomponents;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.ke.screencapture.ScreenCaptureListener;
import com.ke.screencapture.ScreenCaptureManager;

public class ScreenCaptureActivity extends Activity implements ScreenCaptureListener, View.OnClickListener {
    private static final String TAG = ScreenCaptureActivity.class.getSimpleName();
    private Button mScreenCaptureBtn;
    private int mScreenCaptureStatus = 4;
    private static final int SCREEN_CAPTURE_STATUS_STARTING = 1;
    private static final int SCREEN_CAPTURE_STATUS_STARTED = 2;
    private static final int SCREEN_CAPTURE_STATUS_STOPPING = 3;
    private static final int SCREEN_CAPTURE_STATUS_STOPPED = 4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screencapture);
        ScreenCaptureManager.getInstance(this).registerListener(this);
        mScreenCaptureBtn = findViewById(R.id.screen_capture_controller);
        mScreenCaptureBtn.setOnClickListener(this);
    }

    @Override
    public void onScreenCaptureStarted() {
        Log.d(TAG, "onScreenCaptureStarted");
        mScreenCaptureStatus = SCREEN_CAPTURE_STATUS_STARTED;
        mScreenCaptureBtn.setText("停止录屏");

    }

    @Override
    public void onScreenCaptureStopped() {
        Log.d(TAG, "onScreenCaptureStopped");
        mScreenCaptureStatus = SCREEN_CAPTURE_STATUS_STOPPED;
        mScreenCaptureBtn.setText("开始录屏");
    }

    @Override
    public void onScreenCaptureError(int errorCode, String errorMsg) {
        Log.e(TAG, "onScreenCaptureError code:" + errorCode + " msg:" + errorMsg);
        mScreenCaptureStatus = SCREEN_CAPTURE_STATUS_STOPPED;
        mScreenCaptureBtn.setText("开始录屏");
    }

    @Override
    public void onScreenCaptureBitmap(Bitmap bitmap) {
        Log.d(TAG, "onScreenCaptureBitmap " + " bitmap:" + bitmap);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScreenCaptureManager.getInstance(this).unregisterListener(this);
    }

    @Override
    public void onClick(View view) {
        if (mScreenCaptureStatus == SCREEN_CAPTURE_STATUS_STOPPED) {
            ScreenCaptureManager.getInstance(this).startScreenCapture();
            mScreenCaptureBtn.setText("启动中");
            mScreenCaptureStatus = SCREEN_CAPTURE_STATUS_STARTING;
        } else if (mScreenCaptureStatus == SCREEN_CAPTURE_STATUS_STARTED) {
            ScreenCaptureManager.getInstance(this).stopScreenCapture();
            mScreenCaptureBtn.setText("停止中");
            mScreenCaptureStatus = SCREEN_CAPTURE_STATUS_STOPPING;
        }
    }
}
