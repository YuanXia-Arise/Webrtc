package com.dds.webrtclib.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dds.libusbcamera.UVCCameraHelper;
import com.dds.libusbcamera.utils.FileUtils;
import com.dds.webrtclib.R;
import com.dds.webrtclib.Util;
import com.dds.webrtclib.utils.Utils;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;

import org.webrtc.PrefSingleton;

/**
 * 单聊控制界面
 * Created by dds on 2019/1/7.
 * android_shuai@163.com
 */
public class ChatSingleFragment extends Fragment{

    public View rootView;
    private boolean videoEnable;
    private ChatSingleActivity activity;

    private ImageButton DisCalling;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (ChatSingleActivity) getActivity();
        Bundle bundle = getArguments();
        if (bundle != null) {
            videoEnable = bundle.getBoolean("videoEnable");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = onInitloadView(inflater, container, savedInstanceState);
            initView(rootView);
            initListener();
        }
        return rootView;
    }

    private View onInitloadView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wr_fragment_room_control_single, container, false);
    }

    private void initView(View rootView) {
        DisCalling = rootView.findViewById(R.id.DisCalling);
    }

    private void initListener() {
        DisCalling.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.hangUp();
            }
        });
    }


}
