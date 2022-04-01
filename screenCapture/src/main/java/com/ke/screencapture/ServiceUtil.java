package com.ke.screencapture;

import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;

import java.util.List;

public class ServiceUtil {
    public static boolean isServiceRunning(Context context, String serviceName) {
        if (context == null) return false;
        if (TextUtils.isEmpty(serviceName)) return false;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;
        List<ActivityManager.RunningServiceInfo> runningServiceInfoList = activityManager.getRunningServices(500);
        if (runningServiceInfoList == null) return false;
        for (int i = 0; i < runningServiceInfoList.size(); i++) {
            if (runningServiceInfoList.get(i).service.getClassName().equals(serviceName)) {
                return true;
            }

        }
        return false;

    }
}
