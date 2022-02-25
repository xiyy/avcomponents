package com.ke.avcomponents;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ke.screencapture.BitmapUtil;
import com.ke.screencapture.ScreenCaptureListener;
import com.ke.screencapture.ScreenCaptureManager;

public class ScreenCaptureActivity extends Activity implements ScreenCaptureListener, View.OnClickListener {
    private static final String TAG = ScreenCaptureActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private Button mScreenCaptureBtn;
    private int mScreenCaptureStatus = 4;
    private static final int SCREEN_CAPTURE_STATUS_STARTING = 1;
    private static final int SCREEN_CAPTURE_STATUS_STARTED = 2;
    private static final int SCREEN_CAPTURE_STATUS_STOPPING = 3;
    private static final int SCREEN_CAPTURE_STATUS_STOPPED = 4;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
        setContentView(R.layout.activity_screencapture);
        mScreenCaptureBtn = findViewById(R.id.screen_capture_controller);
        mScreenCaptureBtn.setOnClickListener(this);
        ScreenCaptureManager.getInstance(this).registerListener(this);

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
        BitmapUtil.bitmapToJpeg(bitmap, Environment.getExternalStorageDirectory() + "/screenCapture/", System.currentTimeMillis() + ".jpeg");


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

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        } else {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                boolean result = true;
                for (int each : grantResults) {
                    if (each != PackageManager.PERMISSION_GRANTED) {
                        result = false;
                        break;
                    }
                }
                if (!result) {
                    Toast.makeText(this, "权限申请失败", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }
}
