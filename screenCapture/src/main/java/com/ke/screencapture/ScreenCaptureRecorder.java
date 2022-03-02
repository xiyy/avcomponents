
package com.ke.screencapture;

import android.app.Activity;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenCaptureRecorder {
    private static final String TAG = ScreenCaptureRecorder.class.getSimpleName();
    private static volatile ScreenCaptureRecorder mInstance;
    private WeakReference<Activity> mActivity;
    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private int mDpi;
    private String mDstPath;
    private MediaProjection mMediaProjection;
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30; // 30 fps
    private static final int IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    private static final int TIMEOUT_US = 10000;//10毫秒

    private MediaCodec mEncoder;
    private Surface mSurface;
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted = false;
    private int mVideoTrackIndex = -1;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private VirtualDisplay mVirtualDisplay;

    private ScreenCaptureRecorder() {

    }

    public void initContext(Activity activity) {
        mActivity = new WeakReference<>(activity);
        initScreenParam(activity);
    }

    private void initScreenParam(Activity activity) {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        mDpi = metrics.densityDpi;
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getRealSize(size);
        mWidth = size.x;
        mHeight = size.y;
    }

    public Activity getActivity() {
        if (mActivity != null) return mActivity.get();
        return null;
    }

    public static ScreenCaptureRecorder getInstance() {
        if (mInstance == null) {
            synchronized (ScreenCaptureRecorder.class) {
                if (mInstance == null) {
                    mInstance = new ScreenCaptureRecorder();
                }

            }
        }
        return mInstance;
    }

    public void startScreenCapture(MediaProjection mediaProjection, int bitrate, String videoPath) {
        mMediaProjection = mediaProjection;
        mDstPath = videoPath;
        mBitRate = bitrate;
        Executors.newSingleThreadExecutor().execute(new CodecTask());

    }

    public void stopScreenCapture() {
        mQuit.set(true);
    }


    private void recordVirtualDisplay() {
        int readIndex = 0;
        while (!mQuit.get()) {
            int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                resetOutputFormat();
                Log.d(TAG, "recordVirtualDisplay format changed,resetOutputFormat,index:" + index);
            } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.w(TAG, "recordVirtualDisplay retrieving buffers time out，index:" + index);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (index >= 0) {
                if (!mMuxerStarted) {
                    throw new IllegalStateException("MediaMuxer dose not call addTrack(format) ");
                }
                readIndex++;
                if (readIndex == 1) {
                    ScreenCaptureManager.getInstance(getActivity()).onScreenCaptureStarted();
                }
                encodeToVideoTrack(index);
                mEncoder.releaseOutputBuffer(index, false);
            }
        }
    }

    private void encodeToVideoTrack(int index) {
        ByteBuffer encodedData = mEncoder.getOutputBuffer(index);
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.d(TAG, "encodeToVideoTrack ignoring BUFFER_FLAG_CODEC_CONFIG");
            mBufferInfo.size = 0;
        }
        if (mBufferInfo.size == 0) {
            Log.d(TAG, "encodeToVideoTrack info.size == 0, drop it");
            encodedData = null;
        } else {
            Log.d(TAG, "get buffer, info: size=" + mBufferInfo.size
                    + ", presentationTimeUs=" + mBufferInfo.presentationTimeUs
                    + ", offset=" + mBufferInfo.offset);
        }
        if (encodedData != null) {
            encodedData.position(mBufferInfo.offset);
            encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
            mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);
            Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
        }
    }

    private void resetOutputFormat() {
        MediaFormat newFormat = mEncoder.getOutputFormat();
        Log.d(TAG, "resetOutputFormat output format changed new format: " + newFormat.toString());
        mVideoTrackIndex = mMuxer.addTrack(newFormat);
        mMuxer.start();
        mMuxerStarted = true;
        Log.d(TAG, "resetOutputFormat started media muxer, videoIndex=" + mVideoTrackIndex);
    }

    private void prepareEncoder() {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            Log.d(TAG, "prepareEncoder created video format: " + format);
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mEncoder.createInputSurface();
            Log.d(TAG, "prepareEncoder created input surface: " + mSurface);
            mEncoder.start();
        } catch (Exception e) {
            Log.e(TAG, "prepareEncoder Exception:" + e.getLocalizedMessage());
            ScreenCaptureManager.getInstance(getActivity()).onError(ErrorCode.ERROR_PREPARE_ENCODER, ErrorMessage.ERROR_PREPARE_ENCODER + e.getMessage());
        }

    }

    private void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
        Log.d(TAG, "release");
        ScreenCaptureManager.getInstance(getActivity()).onScreenCaptureStopped();
    }

    public class CodecTask implements Runnable {
        @Override
        public void run() {
            Looper.prepare();
            try {
                prepareEncoder();
                mMuxer = new MediaMuxer(mDstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG,
                        mWidth, mHeight, mDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                        mSurface, new VirtualDisplay.Callback() {
                            @Override
                            public void onPaused() {
                                super.onPaused();
                                Log.d(TAG, "CodecTask VirtualDisplay onPaused");
                            }

                            @Override
                            public void onResumed() {
                                super.onResumed();
                                Log.d(TAG, "CodecTask VirtualDisplay onResumed");
                            }

                            @Override
                            public void onStopped() {
                                super.onStopped();
                                Log.d(TAG, "CodecTask VirtualDisplay onStopped");
                            }
                        }, null);
                Log.d(TAG, "CodecTask created virtual display: " + mVirtualDisplay);
                mMediaProjection.registerCallback(new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        super.onStop();
                        Log.d(TAG, "CodecTask mMediaProjection onStop");
                    }
                }, null);
                recordVirtualDisplay();
            } catch (Exception e) {
                Log.e(TAG, "CodecTask  Exception:" + e.getLocalizedMessage());
            } finally {
                release();
            }
        }
    }

}
