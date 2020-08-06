package org.Util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;


import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class JWebSocketClient extends WebSocketClient {

    public JWebSocketClient(URI serverUri) {
        super(serverUri, new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.e("JWebSocketClient", "onOpen()");
    }

    @Override
    public void onMessage(String message) {
        Log.e("JWebSocketClient", "onMessage()");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("JWebSocketClient", "onClose()");
        //new JWebSocketClient(URI.create("ws://139.224.12.24:2346")).reconnect();
    }

    @Override
    public void onError(Exception ex) {
        Log.e("JWebSocketClient", "onError()");
        //new JWebSocketClient(URI.create("ws://139.224.12.24:2346")).reconnect();
    }
}
