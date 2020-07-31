package com.dds.webrtclib.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.dds.libusbcamera.UVCCameraHelper;
import com.dds.libusbcamera.utils.FileUtils;
import com.dds.webrtclib.IViewCallback;
import com.dds.webrtclib.PeerConnectionHelper;
import com.dds.webrtclib.ProxyVideoSink;
import com.dds.webrtclib.R;
import com.dds.webrtclib.Util;
import com.dds.webrtclib.WebRTCManager;
import com.dds.webrtclib.bean.MemberBean;
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
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群聊界面
 */
public class ChatRoomActivity extends AppCompatActivity implements IViewCallback, CameraViewInterface.Callback, CameraDialog.CameraDialogParent {

    private FrameLayout wr_video_view;

    private WebRTCManager manager;
    private Map<String, SurfaceViewRenderer> _videoViews = new HashMap<>();
    private Map<String, ProxyVideoSink> _sinks = new HashMap<>();
    private List<MemberBean> _infos = new ArrayList<>();
    private VideoTrack _localVideoTrack;

    private EglBase rootEglBase;

    private ImageButton DisCalling;

    public static void openActivity(Activity activity) {
        Intent intent = new Intent(activity, ChatRoomActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 全屏
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wr_activity_chat_room);
        initView();
        initVar();

        if (video_type == 1) {
            startCall();
        }

    }


    private void initView() {
        wr_video_view = findViewById(R.id.wr_video_view);

        DisCalling = findViewById(R.id.DisCalling);
        DisCalling.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hangUp();
            }
        });
    }

    private void initVar() {
        rootEglBase = EglBase.create();

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
                public void onPreviewResult(byte[] nv21Yuv) { // UVC回调帧数据
                    try {
                        new Util().createFileWithByte(nv21Yuv, WIDTH, HEIGHT);
                        if (status == 0) {
                            startCall();
                            status = 1;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void startCall() {
        manager = WebRTCManager.getInstance();
        manager.setCallback(this);
        if (!PermissionUtil.isNeedRequestPermission(ChatRoomActivity.this)) {
            manager.joinRoom(getApplicationContext(), rootEglBase);
        }
    }

    @Override
    public void onSetLocalStream(MediaStream stream, String userId) {
        List<VideoTrack> videoTracks = stream.videoTracks;
        if (videoTracks.size() > 0) {
            _localVideoTrack = videoTracks.get(0);
        }
        runOnUiThread(() -> {
            addView(userId, stream);
        });
    }

    @Override
    public void onAddRemoteStream(MediaStream stream, String userId) {
        /*runOnUiThread(() -> {
            addView(userId, stream);
        });*/
    }

    @Override
    public void onCloseWithId(String userId) {
        runOnUiThread(() -> {
            removeView(userId);
        });
    }


    private void addView(String id, MediaStream stream) {
        SurfaceViewRenderer renderer = new SurfaceViewRenderer(ChatRoomActivity.this);
        renderer.init(rootEglBase.getEglBaseContext(), null);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        renderer.setMirror(false);
        // set render
        ProxyVideoSink sink = new ProxyVideoSink();
        sink.setTarget(renderer);
        if (stream.videoTracks.size() > 0) {
            stream.videoTracks.get(0).addSink(sink);
        }
        wr_video_view.addView(renderer);
    }


    private void removeView(String userId) {
        ProxyVideoSink sink = _sinks.get(userId);
        SurfaceViewRenderer renderer = _videoViews.get(userId);
        if (sink != null) {
            sink.setTarget(null);
        }
        if (renderer != null) {
            renderer.release();
        }
        wr_video_view.removeView(renderer);
    }


    @Override  // 屏蔽返回键
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
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
        exit();
        super.onDestroy();
    }

    // 切换摄像头
    public void switchCamera() {
        manager.switchCamera();
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
        exit();
        this.finish();
    }

    // 静音
    public void toggleMic(boolean enable) {
        manager.toggleMute(enable); // 静音
        manager.toggleSpeaker(enable); // 非免提
    }

    // 免提
    public void toggleSpeaker(boolean enable) {
        manager.toggleSpeaker(enable);
    }

    // 打开关闭摄像头
    public void toggleCamera(boolean enableCamera) {
        if (_localVideoTrack != null) {
            _localVideoTrack.setEnabled(enableCamera);
        }
    }

    private void exit() {
        if (manager != null) {
            manager.exitRoom();
        }
        for (SurfaceViewRenderer renderer : _videoViews.values()) {
            renderer.release();
        }
        for (ProxyVideoSink sink : _sinks.values()) {
            sink.setTarget(null);
        }
        _videoViews.clear();
        _sinks.clear();
        _infos.clear();
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
     * 集成UVC视频采集
     */
    public View mTextureView;
    private UVCCameraHelper mCameraHelper;
    private CameraViewInterface mUVCCameraView;
    private boolean isRequest;
    private boolean isPreview;

    private int video_type = 0; // 视频源(1,2)
    private int status = 0;

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
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            PrefSingleton.getInstance().putBoolean("USB_Camera",false);
            finish();
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
