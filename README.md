# avcomponents
Android音视频基础组件<br/>
一、录屏<br/>
1、注册回调接口<br/>
ScreenCaptureManager.getInstance(this).registerListener(this)<br/>
2、开启录屏<br/>
ScreenCaptureManager.getInstance(this).startScreenCapture()<br/>
3、停止录屏<br/>
ScreenCaptureManager.getInstance(this).stopScreenCapture()<br/>
4、回调接口<br/>
录屏已经启动onScreenCaptureStarted<br/>
录屏已经停止onScreenCaptureStopped<br/>
录屏获取数据onScreenCaptureBitmap(Bitmap bitmap)<br/>
录屏报错onScreenCaptureError(int errorCode, String errorMsg)<br/>
