package org.java.voip;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.dds.libusbcamera.UVCCameraHelper;
import com.dds.libusbcamera.utils.FileUtils;
import com.dds.skywebrtc.CallSession;
import com.dds.skywebrtc.EnumType;
import com.dds.skywebrtc.SkyEngineKit;
import com.dds.skywebrtc.Util;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.webrtc.R;
import org.webrtc.PrefSingleton;
import org.webrtc.SurfaceViewRenderer;
import java.util.ArrayList;
import java.util.List;



/**
 * 一对一视频通话控制界面
 */
public class FragmentVideo extends Fragment implements CallSession.CallSessionCallback, View.OnClickListener
        , CameraViewInterface.Callback, CameraDialog.CameraDialogParent{

    private FrameLayout fullscreenRenderer;
    private FrameLayout pipRenderer;

    private CallSingleActivity activity;
    private SkyEngineKit gEngineKit;
    private boolean isOutgoing;
    private boolean isFromFloatingView;
    private SurfaceViewRenderer localSurfaceView;
    private SurfaceViewRenderer remoteSurfaceView;

    private ImageButton DisCalling;

    private int video_type = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        initView(view);
        init();
        return view;
    }


    private void initView(View view) {
        fullscreenRenderer = view.findViewById(R.id.fullscreen_video_view);
        pipRenderer = view.findViewById(R.id.pip_video_view);

        DisCalling = view.findViewById(R.id.DisCalling);
        DisCalling.setOnClickListener(this);

        video_type = PrefSingleton.getInstance().getInt("video");
        if (video_type == 2) {
            if (mCameraHelper != null) {
                mCameraHelper.closeCamera();
                mCameraHelper.release();
            }
            Resolv();
            PrefSingleton.getInstance().putBoolean("USB_Camera",true);
            mTextureView = view.findViewById(R.id.camera_view);
            mUVCCameraView = (CameraViewInterface) mTextureView;
            mUVCCameraView.setCallback(this);
            mCameraHelper = UVCCameraHelper.getInstance();
            mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
            mCameraHelper.initUSBMonitor(getActivity(), mUVCCameraView, listener, WIDTH, HEIGHT);
            //mCameraHelper.initUSBMonitor(getActivity(), mUVCCameraView, listener);

            mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
                @Override
                public void onPreviewResult(byte[] nv21Yuv) {
                    System.out.println("123==00");
                    try {
                        Thread.sleep(100);
                        new Util().createFileWithByte(nv21Yuv);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }


    private void init() {
        gEngineKit = activity.getEngineKit();
        CallSession session = gEngineKit.getCurrentSession();
        if (session == null || EnumType.CallState.Idle == session.getState()) {
            activity.finish();
        } else if (EnumType.CallState.Connected == session.getState()) {
            startRefreshTime();
        } else {
            // do nothing now
        }
        if (isFromFloatingView) {
            didCreateLocalVideoTrack();
            didReceiveRemoteVideoTrack();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (CallSingleActivity) getActivity();
        if (activity != null) {
            isOutgoing = activity.isOutgoing();
            isFromFloatingView = activity.isFromFloatingView();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }

    @Override
    public void didCallEndWithReason(EnumType.CallEndReason var1) {

    }

    @Override
    public void didChangeState(EnumType.CallState state) {
        runOnUiThread(() -> {
            if (state == EnumType.CallState.Connected) {
                startRefreshTime(); // 开启计时器
            } else {
                // do nothing now
            }
        });
    }

    @Override
    public void didChangeMode(boolean isAudio) {
        runOnUiThread(() -> activity.switchAudio());

    }

    @Override
    public void didCreateLocalVideoTrack() {
        SurfaceViewRenderer surfaceView = gEngineKit.getCurrentSession().createRendererView();
        if (surfaceView != null) {
            surfaceView.setZOrderMediaOverlay(true);
            localSurfaceView = surfaceView;
            if (isOutgoing && remoteSurfaceView == null) {
                fullscreenRenderer.addView(surfaceView);
            } else {
                pipRenderer.addView(surfaceView);
            }
            gEngineKit.getCurrentSession().setupLocalVideo(surfaceView);
        }
    }

    @Override
    public void didReceiveRemoteVideoTrack() {
        //pipRenderer.setVisibility(View.VISIBLE);
        if (isOutgoing && localSurfaceView != null) {
            ((ViewGroup) localSurfaceView.getParent()).removeView(localSurfaceView);
            pipRenderer.addView(localSurfaceView);
            gEngineKit.getCurrentSession().setupLocalVideo(localSurfaceView);
        }

        SurfaceViewRenderer surfaceView = gEngineKit.getCurrentSession().createRendererView();
        if (surfaceView != null) {
            remoteSurfaceView = surfaceView;
            fullscreenRenderer.removeAllViews();
            fullscreenRenderer.addView(surfaceView);
            gEngineKit.getCurrentSession().setupRemoteVideo(surfaceView);
        }
    }

    @Override
    public void didError(String error) {

    }

    private void runOnUiThread(Runnable runnable) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (localSurfaceView != null) {
            localSurfaceView.release();
        }
        if (remoteSurfaceView != null) {
            remoteSurfaceView.release();
        }
        fullscreenRenderer.removeAllViews();
        pipRenderer.removeAllViews();

        //挂断
        CallSession session = gEngineKit.getCurrentSession();
        if (session != null) {
            SkyEngineKit.Instance().endCall();

            if(PrefSingleton.getInstance().getInt("video") == 2) {
                PrefSingleton.getInstance().putBoolean("USB_Camera",false);
                FileUtils.releaseFile();
                mCameraHelper.closeCamera();
                if (mCameraHelper != null) {
                    mCameraHelper.release();
                }
            }

            activity.finish();
        } else {
            activity.finish();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        CallSession session = gEngineKit.getCurrentSession();

        if (id == R.id.DisCalling) { // 挂断
            if (session != null) {
                SkyEngineKit.Instance().endCall();

                if(PrefSingleton.getInstance().getInt("video") == 2) {
                    PrefSingleton.getInstance().putBoolean("USB_Camera",false);
                    FileUtils.releaseFile();
                    mCameraHelper.closeCamera();
                    if (mCameraHelper != null) {
                        mCameraHelper.release();
                    }
                }

                activity.finish();
            } else {
                activity.finish();
            }
        }
    }

    private void startRefreshTime() {
        CallSession session = SkyEngineKit.Instance().getCurrentSession();
        if (session == null) {
            return;
        }
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
                            Thread.sleep(1000);
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
        }
    }

}
