package org.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;
import com.webrtc.R;

import org.Util.DisplayUtil;
import org.Util.MyApp;
import org.Util.WebSocket;
import org.Util.WebrtcUtil;
import org.ch34x.CH34xUARTDriver;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PrefSingleton;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;


import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.activity.App.BUS;


public class LauncherActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    public final int REQUEST_CAMERA_PERMISSION = 1003;

    private ImageButton imageButton,Calling;
    private Switch aSwitch,bSwitch,cSwitch,dSwitch,eSwitch,fSwitch,gSwitch;
    private RadioGroup radioGroup;	//????????????
    private RadioButton radioButton1, radioButton2;
    private ImageView imageView1,imageView2;
    private TextView switch1,switch2,switch3,switch5,switch6,switch7,switch8;

    private TextView ch34_status;

    private boolean isOpen;
    private int baudRate;
    private byte stopBit;
    private byte dataBit;
    private byte parity;
    private byte flowControl;
    private Switch ButtonView;
    private TextView Imei;

    private IntentFilter usbDeviceStateFilter;

    private ImageView image1,image2,image3;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayUtil.setDensity(LauncherActivity.this, getApplication());  // ????????????UI??????
        setContentView(R.layout.activity_launch);
        //PrefSingleton.getInstance().Initialize(getApplicationContext());
        SetImageView();
        ch34_status = findViewById(R.id.ch34_status);
        ButtonView = findViewById(R.id.button2);
        Imei = findViewById(R.id.Imei);

        usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbDeviceStateFilter);

        mLauncherActivityRunningInstance = this;

        imageButton = findViewById(R.id.settings);
        imageButton.setOnClickListener(this);
        Calling = findViewById(R.id.Calling);
        Calling.setOnClickListener(this);
        aSwitch = findViewById(R.id.button1);
        aSwitch.setOnCheckedChangeListener(this);
        bSwitch = findViewById(R.id.button2);
        bSwitch.setOnCheckedChangeListener(this);
        cSwitch = findViewById(R.id.button3);
        cSwitch.setOnCheckedChangeListener(this);
        dSwitch = findViewById(R.id.button5);
        dSwitch.setOnCheckedChangeListener(this);
        eSwitch = findViewById(R.id.button6);
        eSwitch.setOnCheckedChangeListener(this);
        fSwitch = findViewById(R.id.button7);
        fSwitch.setOnCheckedChangeListener(this);
        gSwitch = findViewById(R.id.button8);
        gSwitch.setOnCheckedChangeListener(this);
        /*PrefSingleton.getInstance().putBoolean("flow_mode", false);
        PrefSingleton.getInstance().putBoolean("key_mode", false);
        PrefSingleton.getInstance().putBoolean("voice_mode", false);
        PrefSingleton.getInstance().putBoolean("speak_mode", false);
        PrefSingleton.getInstance().putBoolean("focus_mode", false);
        PrefSingleton.getInstance().putBoolean("recorder_mode", false);
        PrefSingleton.getInstance().putBoolean("water_mode", false);*/
        switch1 = findViewById(R.id.switch1);
        switch2 = findViewById(R.id.switch2);
        switch3 = findViewById(R.id.switch3);
        switch5 = findViewById(R.id.switch5);
        switch6 = findViewById(R.id.switch6);
        switch7 = findViewById(R.id.switch7);
        switch8 = findViewById(R.id.switch8);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioButton1 = (RadioButton) findViewById(R.id.video_one);
        radioButton2 = (RadioButton) findViewById(R.id.video_two);
        radioGroup.setOnCheckedChangeListener(new RadioListener());
        imageView1 = findViewById(R.id.image1);
        PrefSingleton.getInstance().putInt("video",1);
        imageView2 = findViewById(R.id.image2);
        imageView2.setVisibility(View.INVISIBLE);

        BUS.register(this);
        Permission(); // ????????????
        CH34x(); // ??????ch34x??????

        UsbDevice device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null && (device.getProductId() == 29987 || device.getProductId() == 21795 || device.getProductId() == 21778)) {
            ch34_status.setText("USB???????????????");
            ch34_status.setTextColor(Color.WHITE);
            onConnect();
            if (isOpen) {
                ButtonView.setChecked(true);
                switch2.setText("???");
                PrefSingleton.getInstance().putBoolean("key_mode", true);
            } else {
                ButtonView.setChecked(false);
                switch2.setText("???");
                PrefSingleton.getInstance().putBoolean("key_mode", false);
            }
        } else {
            ch34_status.setText("????????????USB??????");
            ch34_status.setTextColor(Color.RED);
        }

    }

    public void SetImageView() {
        image1 = findViewById(R.id.image1);
        Picasso.get()
                .load(R.drawable.ic_switch)
                .fit()
                .into(image1);

        image2 = findViewById(R.id.image2);
        Picasso.get()
                .load(R.drawable.ic_switch)
                .fit()
                .into(image2);

        image3 = findViewById(R.id.image3);
        Picasso.get()
                .load(R.drawable.ic_calls)
                .fit()
                .into(image3);

    }

    //BroadcastReceiver??????UI??????
    private static LauncherActivity mLauncherActivityRunningInstance;
    public void updateUI(final boolean boo) {
        LauncherActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                bSwitch.setChecked(boo);
                switch2.setText("???");
                PrefSingleton.getInstance().putBoolean("key_mode", false);
            }
        });
    }
    public static LauncherActivity getInstace(){
        return mLauncherActivityRunningInstance;
    }

    // USB ch34x ????????????????????????
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (device != null && (device.getProductId() == 29987 || device.getProductId() == 21795 || device.getProductId() == 21778)) {
                    System.out.println("???????????????");
                    ch34_status.setText("USB???????????????");
                    ch34_status.setTextColor(Color.RED);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)
                    && (device.getProductId() == 29987 || device.getProductId() == 21795 || device.getProductId() == 21778)) {
                System.out.println("???????????????");
                ch34_status.setText("USB???????????????");
                ch34_status.setTextColor(Color.WHITE);
                onConnect();
                if (isOpen) {
                    ButtonView.setChecked(true);
                    switch2.setText("???");
                    PrefSingleton.getInstance().putBoolean("key_mode", true);
                } else {
                    ButtonView.setChecked(false);
                    switch2.setText("???");
                    PrefSingleton.getInstance().putBoolean("key_mode", false);
                }
                //Toast.makeText(getApplicationContext(), device.getProductId() + " " + device.getVendorId(), Toast.LENGTH_SHORT).show();
            }
        }
    };


    // ??????????????????
    public void Permission() {
        // checkPermission(this); // ????????????????????????
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION);
            return;
        } else {
            // resume
        }
    }

    // ????????????????????????
    public static boolean checkPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            activity.startActivityForResult(
                    new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + activity.getPackageName())), 0);
            return false;
        }
        return true;
    }

    // CH34x??????????????????
    public void CH34x() {
        baudRate = 115200;
        stopBit = 1;
        dataBit = 8;
        parity = 0;
        flowControl = 0;
        MyApp.driver = new CH34xUARTDriver((UsbManager) getSystemService(Context.USB_SERVICE), this, ACTION_USB_PERMISSION);
        MyApp.driver.setShowToast(true);
        if (!MyApp.driver.usbFeatureSupported()) {
            Dialog dialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("??????")
                    .setMessage("?????????????????????USB HOST?????????????????????????????????")
                    .setPositiveButton("??????",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    System.exit(0);
                                }
                            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestPermission();
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_NETWORK_STATE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE},1);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE},1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    //showWaringDialog();
                }
                return;
            }
        }
    }

    private void showWaringDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("?????????")
                .setMessage("???????????????->??????->Permission->???????????????????????????????????????????????????????????????")
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }


    // web_socket
    public void Ws_connect(String imei) {
        String url = PrefSingleton.getInstance().getString("Url");
        if (url.equals("")) {
            Toast.makeText(getApplicationContext(), "?????????????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        URI uri;
        try {
            uri = new URI("ws://" + url + ":3080/key_data_ws?name=" + imei);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        WebSocketClient webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.e("WebSocketClient", "onOpen()");
                PrefSingleton.getInstance().putBoolean("websocket",true);
            }

            @Override
            public void onMessage(String message) {
                Log.e("WebSocketClient", message);
                try {
                    JSONObject jsonObj = new JSONObject(message);
                    boolean key_mode = PrefSingleton.getInstance().getBoolean("key_mode");
                    if (key_mode && ((String) jsonObj.get("serial")).equals(imei)){
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
                Log.e("WebSocketClient", "onClose()" + remote);
                PrefSingleton.getInstance().putBoolean("websocket",false);
                if (remote) {
                    Log.e("WebSocketClient", "????????????");
                    while (!PrefSingleton.getInstance().getBoolean("websocket")){
                        try {
                            Thread.sleep(100);
                            Ws_connect(imei);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onError(Exception ex) {
                Log.e("WebSocketClient", "onError()");
            }
        };
        try {
            if (!webSocketClient.isOpen()) {
                webSocketClient.connectBlocking(); // web_socket??????
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "?????????????????????1", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "?????????????????????2", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.settings: // ??????
                startActivity(new Intent(this, SetupActivity.class));
                break;
            case R.id.Calling: // ??????
                onCall();
                break;
            default:
                break;
        }
    }


    //Switch (ON/OFF)
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.button1:
                if (buttonView.isChecked()) {
                    switch1.setText("???");
                    PrefSingleton.getInstance().putBoolean("flow_mode", true);
                } else {
                    switch1.setText("???");
                    PrefSingleton.getInstance().putBoolean("flow_mode", false);
                }
                break;
            case R.id.button2:
                if (buttonView.isChecked()) {
                    onConnect();
                    if (isOpen) {
                        buttonView.setChecked(true);
                        switch2.setText("???");
                        PrefSingleton.getInstance().putBoolean("key_mode", true);
                    } else {
                        buttonView.setChecked(false);
                        switch2.setText("???");
                        PrefSingleton.getInstance().putBoolean("key_mode", false);
                    }
                } else {
                    onDisConnect();
                    if (!isOpen) {
                        buttonView.setChecked(false);
                        switch2.setText("???");
                        PrefSingleton.getInstance().putBoolean("key_mode", false);
                    } else {
                        buttonView.setChecked(true);
                        switch2.setText("???");
                        PrefSingleton.getInstance().putBoolean("key_mode", true);
                    }
                }
                break;
            case R.id.button3:
                if(buttonView.isChecked()) {
                    PrefSingleton.getInstance().putBoolean("voice_mode", true);
                    switch3.setText("???");
                } else {
                    PrefSingleton.getInstance().putBoolean("voice_mode", false);
                    switch3.setText("???");
                }
                break;
            case R.id.button5:
                if(buttonView.isChecked()) {
                    PrefSingleton.getInstance().putBoolean("speak_mode", true);
                    switch5.setText("???");
                } else {
                    PrefSingleton.getInstance().putBoolean("speak_mode", false);
                    switch5.setText("???");
                }
                break;
            case R.id.button6:
                if(buttonView.isChecked()) {
                    PrefSingleton.getInstance().putBoolean("focus_mode", true);
                    switch6.setText("???");
                } else {
                    PrefSingleton.getInstance().putBoolean("focus_mode", false);
                    switch6.setText("???");
                }
                break;
            case R.id.button7:
                if(buttonView.isChecked()) {
                    PrefSingleton.getInstance().putBoolean("recorder_mode", true);
                    switch7.setText("???");
                    //gSwitch.setChecked(false);
                } else {
                    PrefSingleton.getInstance().putBoolean("recorder_mode", false);
                    switch7.setText("???");
                }
                break;
            case R.id.button8:
                if(buttonView.isChecked()) {
                    Toast.makeText(getApplicationContext(), "???????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    PrefSingleton.getInstance().putBoolean("water_mode", true);
                    switch8.setText("???");
                    //fSwitch.setChecked(false);
                } else {
                    PrefSingleton.getInstance().putBoolean("water_mode", false);
                    switch8.setText("???");
                }
                break;
        }
    }

    // ?????????????????????
    private class RadioListener implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == R.id.video_one) {
                PrefSingleton.getInstance().putInt("video",1);
                imageView1.setVisibility(View.VISIBLE);
                imageView2.setVisibility(View.INVISIBLE);
            } else if (checkedId == R.id.video_two) {
                PrefSingleton.getInstance().putInt("video",2);
                imageView1.setVisibility(View.INVISIBLE);
                imageView2.setVisibility(View.VISIBLE);
            }
        }
    }

    //????????????
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onCall(){
        String wss = PrefSingleton.getInstance().getString("Url");
        if (wss.equals("")) {
            Toast.makeText(getApplicationContext(), "?????????????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        String room_id = PrefSingleton.getInstance().getString("Imei_id");
        if (room_id.equals("")){
            room_id = getPhoneIMEI();
        }
        try {
            Thread.sleep(100);
            //WebrtcUtil.callSingle(this, wss, room_id,true); // one to one
            WebrtcUtil.call(this, wss, room_id); // meeting
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //ch34x????????????
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onConnect() {
        if (!isOpen) {
            int retval = MyApp.driver.resumeUsbPermission();
            if (retval == 0) {
                retval = MyApp.driver.resumeUsbList();
                if (retval == -1) {
                    Toast.makeText(getApplicationContext(), "??????????????????", Toast.LENGTH_SHORT).show();
                    MyApp.driver.closeDevice();
                } else if (retval == 0){
                    if (MyApp.driver.getUsbDeviceConnection() != null) {
                        if (!MyApp.driver.uartInit()) {
                            Toast.makeText(getApplicationContext(), "Initialization failed!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ch34_status.setText("USB???????????????");
                        ch34_status.setTextColor(Color.WHITE);
                        Toast.makeText(getApplicationContext(), "???????????????", Toast.LENGTH_SHORT).show();
                        isOpen = true;
                    } else {
                        Toast.makeText(getApplicationContext(), "??????????????????", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("????????????");
                    builder.setMessage("????????????????");
                    builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    });
                    builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                        }
                    });
                    builder.show();
                }
                MyApp.driver.setConfig(baudRate, dataBit, stopBit, parity, flowControl); // ??????????????????

                String imei = PrefSingleton.getInstance().getString("Imei_id");
                if (imei.equals("")){
                    imei = getPhoneIMEI();
                }
                Ws_connect(imei);
            }
        }
    }


    public void onDisConnect() {
        if (isOpen && MyApp.driver.isConnected()) {
            isOpen = false;
            MyApp.driver.closeDevice();
            Toast.makeText(getApplicationContext(), "???????????????", Toast.LENGTH_SHORT).show();
        } else {
            isOpen = false;
        }
    }

    //?????????????????????IMEI
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getPhoneIMEI() {
        String DeviceIMEI = "";
        DeviceIMEI = stringToAscii(Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID)).replace(",","").substring(0,10);
        PrefSingleton.getInstance().putString("Imei_id",DeviceIMEI);
        return DeviceIMEI;
    }


     // ??????????????????ASCII
    public static String stringToAscii(String value) {
        StringBuffer sbu = new StringBuffer();
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if(i != chars.length - 1) {
                sbu.append((int)chars[i]).append(",");
            } else {
                sbu.append((int)chars[i]);
            }
        }
        return sbu.toString();
    }

    @Override  // ???????????????
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
        if(keyCode == KeyEvent.KEYCODE_BACK) { //??????/??????/???????????????
            System.exit(0);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        BUS.unregister(this);
        isOpen = false;
        MyApp.driver.closeDevice();
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onResume() {
        Imei.setText("???????????????:" + getPhoneIMEI());

        onConnect();
        if (isOpen) {
            ButtonView.setChecked(true);
            switch2.setText("???");
            PrefSingleton.getInstance().putBoolean("key_mode", true);
        } else {
            ButtonView.setChecked(false);
            switch2.setText("???");
            PrefSingleton.getInstance().putBoolean("key_mode", false);
        }
        //20210128
        PrefSingleton.getInstance().Initialize(getApplicationContext());
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

}
