package com.dds.webrtclib.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.dds.libusbcamera.UVCCameraHelper;
import com.dds.libusbcamera.utils.FileUtils;
import com.dds.webrtclib.IViewCallback;
import com.dds.webrtclib.PeerConnectionHelper;
import com.dds.webrtclib.ProxyVideoSink;
import com.dds.webrtclib.R;
import com.dds.webrtclib.Util;
import com.dds.webrtclib.WebRTCManager;
import com.dds.webrtclib.utils.PermissionUtil;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.PrefSingleton;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

/**
 * 单聊界面
 * 1. 一对一视频通话
 * 2. 一对一语音通话
 */
public class ChatSingleActivity extends AppCompatActivity implements CameraViewInterface.Callback, CameraDialog.CameraDialogParent {
    private SurfaceViewRenderer local_view;
    private SurfaceViewRenderer remote_view;
    private ProxyVideoSink localRender;
    private ProxyVideoSink remoteRender;

    private WebRTCManager manager;

    private boolean videoEnable;
    private boolean isSwappedFeeds;

    private EglBase rootEglBase;

    private int video_type = 0;
    private int status = 0;

    public static void openActivity(Activity activity, boolean videoEnable) {
        Intent intent = new Intent(activity, ChatSingleActivity.class);
        intent.putExtra("videoEnable", videoEnable);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wr_activity_chat_single);
        initVar();

    }



    private void initVar() {
        Intent intent = getIntent();
        videoEnable = intent.getBooleanExtra("videoEnable", false);

        ChatSingleFragment chatSingleFragment = new ChatSingleFragment();
        replaceFragment(chatSingleFragment, videoEnable);
        rootEglBase = EglBase.create();
        if (videoEnable) {
            status = 0;
            video_type = PrefSingleton.getInstance().getInt("video");
            if (video_type == 2) {
                if (mCameraHelper != null) {
                    mCameraHelper.closeCamera();
                    mCameraHelper.release();
                }
                Resolv();
                PrefSingleton.getInstance().putBoolean("USB_Camera",true);
                mTextureView = findViewById(R.id.camera_view);
                mUVCCameraView = (CameraViewInterface) mTextureView;
                mUVCCameraView.setCallback(this);
                mCameraHelper = UVCCameraHelper.getInstance();
                mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
                mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener, WIDTH, HEIGHT);
                mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
                    @Override
                    public void onPreviewResult(byte[] nv21Yuv) {
                        System.out.println("123==00");
                        try {
                            Thread.sleep(100);
                            new Util().createFileWithByte(nv21Yuv, WIDTH, HEIGHT);
                            if (status == 0) {
                                System.out.println("123==2");
                                startCall(); // one to one
                                status = 1;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            local_view = findViewById(R.id.local_view_render);
            remote_view = findViewById(R.id.remote_view_render);
            remote_view.setVisibility(View.GONE);

            // 本地图像初始化
            local_view.init(rootEglBase.getEglBaseContext(), null);
            //local_view.init(null, null);
            local_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            local_view.setZOrderMediaOverlay(true);
            local_view.setMirror(false);
            localRender = new ProxyVideoSink();

            // 远端图像初始化
            remote_view.init(rootEglBase.getEglBaseContext(), null);
            //remote_view.init(null, null);
            remote_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
            remote_view.setMirror(false);
            remoteRender = new ProxyVideoSink();
            setSwappedFeeds(true);

            local_view.setOnClickListener(v -> setSwappedFeeds(!isSwappedFeeds));
        }

        if (video_type == 1) {
            startCall();
        }
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        this.isSwappedFeeds = isSwappedFeeds;
        /*localRender.setTarget(isSwappedFeeds ? remote_view : local_view);
        remoteRender.setTarget(isSwappedFeeds ? local_view : remote_view);*/
        localRender.setTarget(local_view);
        remoteRender.setTarget(remote_view);
    }

    private void startCall() {
        manager = WebRTCManager.getInstance();
        if (PrefSingleton.getInstance().getBoolean("voice_mode")) {
            toggleMic(true);
        } else {
            toggleMic(false);
        }
        manager.setCallback(new IViewCallback() {
            @Override
            public void onSetLocalStream(MediaStream stream, String socketId) {
                if (stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(localRender);
                }
                if (videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                }
            }

            @Override
            public void onAddRemoteStream(MediaStream stream, String socketId) {
                if (stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(remoteRender);
                }
                if (videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                    runOnUiThread(() -> setSwappedFeeds(false));
                }
            }

            @Override
            public void onCloseWithId(String socketId) {
                runOnUiThread(() -> {
                    disConnect();
                    ChatSingleActivity.this.finish();
                });

            }
        });
        if (!PermissionUtil.isNeedRequestPermission(ChatSingleActivity.this)) {
            manager.joinRoom(getApplicationContext(), rootEglBase);
        }

    }

    private void replaceFragment(Fragment fragment, boolean videoEnable) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("videoEnable", videoEnable);
        fragment.setArguments(bundle);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.wr_container, fragment).commit();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }


    // 挂断
    public void hangUp() {
        if(PrefSingleton.getInstance().getInt("video") == 2) {
            PrefSingleton.getInstance().putBoolean("USB_Camera",false);
            FileUtils.releaseFile();
            mCameraHelper.closeCamera();
            if (mCameraHelper != null) {
                mCameraHelper.release();
            }
        }
        disConnect();
        this.finish();
    }

    // 静音
    public void toggleMic(boolean enable) {
        manager.toggleMute(enable); // 静音
        manager.toggleSpeaker(enable); // 免提
    }


    @Override
    protected void onDestroy() {
        if(PrefSingleton.getInstance().getInt("video") == 2) {
            PrefSingleton.getInstance().putBoolean("USB_Camera",false);
            FileUtils.releaseFile();
            mCameraHelper.closeCamera();
            if (mCameraHelper != null) {
                mCameraHelper.release();
            }
        }
        disConnect();
        super.onDestroy();

    }

    private void disConnect() {
        manager.exitRoom();
        if (localRender != null) {
            localRender.setTarget(null);
            localRender = null;
        }
        if (remoteRender != null) {
            remoteRender.setTarget(null);
            remoteRender = null;
        }

        if (local_view != null) {
            local_view.release();
            local_view = null;
        }
        if (remote_view != null) {
            remote_view.release();
            remote_view = null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            Log.i(PeerConnectionHelper.TAG, "[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }
        manager.joinRoom(getApplicationContext(), rootEglBase);
    }

    /**
     * 集成UVC采集视频
     */
    public View mTextureView;
    private UVCCameraHelper mCameraHelper;
    private CameraViewInterface mUVCCameraView;
    private boolean isRequest;
    private boolean isPreview;

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                isPreview = false;
            } else {
                isPreview = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.prepare();
                        if(mCameraHelper != null && mCameraHelper.isCameraOpened()) {

                        }
                        Looper.loop();
                    }
                }).start();
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            return;
        }
    };

    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(mUVCCameraView);
            isPreview = true;
        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }
    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
        }
    }

    /**
     * 分辨率数据获取
     */
    private int WIDTH = 1920;
    private int HEIGHT = 1080;
    public void Resolv() {
        int resolv = PrefSingleton.getInstance().getInt("resolv");
        switch (resolv) {

            case 4:
                WIDTH = 640;
                HEIGHT = 480;
                break;
            case 5:
                WIDTH = 800;
                HEIGHT = 600;
                break;
            case 6:
                WIDTH = 1280;
                HEIGHT = 960;
                break;
                default:
                    WIDTH = 1920;
                    HEIGHT = 1080;
                    break;
        }
    }

}
