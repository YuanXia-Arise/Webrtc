package org.Util;

import android.util.Log;


import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PrefSingleton;

import java.net.URI;


public class JWebSocketClient extends WebSocketClient {

    public JWebSocketClient(URI serverUri) {
        super(serverUri, new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.e("JWebSocketClient", "onOpen()");
        PrefSingleton.getInstance().putBoolean("websocket",true);
    }

    @Override
    public void onMessage(String message) {
        Log.e("JWebSocketClient", "onMessage()-" + message);
        try {
            JSONObject jsonObj = new JSONObject(message);
            boolean key_mode = PrefSingleton.getInstance().getBoolean("key_mode");
            if (key_mode && ((String) jsonObj.get("serial")).equals(PrefSingleton.getInstance().getString("Imei_id"))){
                new WebSocket().GetData(message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("JWebSocketClient", "onClose()" + remote);
        PrefSingleton.getInstance().putBoolean("websocket",false);
        if (remote) {
            Log.e("JWebSocketClient", "断开重连");
            while (!PrefSingleton.getInstance().getBoolean("websocket")){
                try {
                    Thread.sleep(100);
                    new JWebSocketClient(URI.create("ws://192.168.10.198:3080/key_data_ws?name="
                            + PrefSingleton.getInstance().getString("Imei_id"))).connectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e("JWebSocketClient", "onError()");
    }

}
