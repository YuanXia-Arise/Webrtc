package org.Util;

import android.app.Activity;
import android.text.TextUtils;

import com.dds.webrtclib.WebRTCManager;
import com.dds.webrtclib.bean.MediaType;
import com.dds.webrtclib.bean.MyIceServer;
import com.dds.webrtclib.ui.ChatSingleActivity;
import com.dds.webrtclib.ws.IConnectEvent;

import org.activity.ChatRoomActivity;


/**
 * Created by dds on 2019/1/7.
 * android_shuai@163.com
 */
public class WebrtcUtil {

    //public static final String HOST = "101.132.186.228";
    public static String HOST;

    // turn and stun
    private static MyIceServer[] iceServers = {
            new MyIceServer("stun:stun.l.google.com:19302"),

            // 地址1
            new MyIceServer("stun:" + HOST + ":3478?transport=udp"),
            new MyIceServer("turn:" + HOST + ":3478?transport=udp","ddssingsong","123456"),
            new MyIceServer("turn:" + HOST + ":3478?transport=tcp","ddssingsong","123456"),
    };

    //private static String WSS = "wss://101.132.186.228/wss"; // 默认IP地址

    // one to one
    public static void callSingle(Activity activity, String wss, String roomId, boolean videoEnable) {
        /*if (TextUtils.isEmpty(wss)) {
            wss = WSS;
        }*/
        WebRTCManager.getInstance().init(wss, iceServers, new IConnectEvent() {
            @Override
            public void onSuccess() {
                ChatSingleActivity.openActivity(activity, videoEnable);
            }

            @Override
            public void onFailed(String msg) {

            }
        });
        WebRTCManager.getInstance().connect(videoEnable ? MediaType.TYPE_VIDEO : MediaType.TYPE_AUDIO, roomId);
    }

    // meeting
    public static void call(Activity activity, String wss, String roomId) {
        /*if (TextUtils.isEmpty(wss)) {
            wss = WSS;
        }*/
        HOST = wss;
        String Wss = "wss://" + wss + "/wss";
        WebRTCManager.getInstance().init(Wss, iceServers, new IConnectEvent() {
            @Override
            public void onSuccess() {
                ChatRoomActivity.openActivity(activity);
            }

            @Override
            public void onFailed(String msg) {

            }
        });
        WebRTCManager.getInstance().connect(MediaType.TYPE_MEETING, roomId);
    }

}
