package com.ke.screencapture;

import android.graphics.Bitmap;

public interface ScreenCaptureListener {
    void onScreenCaptureStarted();
    void onScreenCaptureStopped();
    void onScreenCaptureError(int errorCode,String errorMsg);
    void onScreenCaptureBitmap(Bitmap bitmap);
}
