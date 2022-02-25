package com.ke.screencapture;

import static android.app.Activity.RESULT_OK;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class ScreenCaptureService extends Service {
    private static final String TAG = ScreenCaptureService.class.getSimpleName();


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        NotificationBuilder builder = new NotificationBuilder(this);
        Notification notification = builder.buildNotification();
        startForeground(NotificationBuilder.NOTIFICATION_ID, notification);
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(RESULT_OK, intent.getParcelableExtra(ScreenCaptureManager.ACTIVITY_RESULT_INTENT));
        ScreenCaptureProjection.getInstance().startScreenCapture(mediaProjection);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        stopForeground(true);
    }

    public static class NotificationBuilder {
        public static final String NOTIFICATION_CHANNEL_ID = "NotificationBuilder";
        public static final int NOTIFICATION_ID = 0xD660;
        private final Context mContext;
        private final NotificationManager mNotificationManager;

        public NotificationBuilder(Context context) {
            mContext = context;
            mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        public Notification buildNotification() {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID);

            return builder.setContentTitle(mContext.getString(R.string.screen_capture_notification_title)).setContentText(AppUtil.getAppName(mContext) + mContext.getString(R.string.screen_capture_notification_text)).setSmallIcon(R.drawable.screen_capture_notification_icon)
                    .build();
        }

    }

}