package com.ke.screencapture;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class BitmapUtil {
    private static final String TAG = BitmapUtil.class.getSimpleName();

    public static void bitmapToJpeg(Bitmap bitmap, String path, String fileName) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(path + fileName);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, ((OutputStream) fileOutputStream));
            fileOutputStream.flush();
            fileOutputStream.close();
            Log.d(TAG, "bitmapToJpeg success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "bitmapToJpeg Exception:" + e.getLocalizedMessage());
        }
    }
}
