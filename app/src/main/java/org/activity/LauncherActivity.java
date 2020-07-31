package org.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.webrtc.R;

import org.Util.DisplayUtil;
import org.Util.JWebSocketClient;
import org.Util.MyApp;
import org.Util.WebSocket;
import org.Util.WebrtcUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PrefSingleton;

import java.net.URI;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.activity.App.BUS;


public class LauncherActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    public static final int REQUEST_CAMERA_PERMISSION = 1003;

    private ImageButton imageButton,Calling;
    private Switch aSwitch,bSwitch,cSwitch;
    private RadioGroup radioGroup;	//单选框组
    private RadioButton radioButton1, radioButton2;
    private ImageView imageView1,imageView2;
    private TextView switch1,switch2,switch3;

    private boolean isOpen;
    private int baudRate;
    private byte stopBit;
    private byte dataBit;
    private byte parity;
    private byte flowControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayUtil.setDensity(LauncherActivity.this, getApplication());  // 机型屏幕适配
        setContentView(R.layout.activity_launcher);

        PrefSingleton.getInstance().Initialize(getApplicationContext());

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
        PrefSingleton.getInstance().putBoolean("flow_mode", false);
        PrefSingleton.getInstance().putBoolean("key_mode", false);
        PrefSingleton.getInstance().putBoolean("voice_mode", false);
        switch1 = findViewById(R.id.switch1);
        switch2 = findViewById(R.id.switch2);
        switch3 = findViewById(R.id.switch3);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioButton1 = (RadioButton) findViewById(R.id.video_one);
        radioButton2 = (RadioButton) findViewById(R.id.video_two);
        radioGroup.setOnCheckedChangeListener(new RadioListener());
        imageView1 = findViewById(R.id.image1);
        PrefSingleton.getInstance().putInt("video",1);
        imageView2 = findViewById(R.id.image2);
        imageView2.setVisibility(View.INVISIBLE);

        BUS.register(this);
        Permission(); // 动态权限
        CH34x(); // 配置ch34x参数

    }

    // 动态权限申请
    public void Permission() {
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

    // CH34x芯片参数配置
    public void CH34x() {
        baudRate = 115200;
        stopBit = 1;
        dataBit = 8;
        parity = 0;
        flowControl = 0;
        MyApp.driver = new CH34xUARTDriver((UsbManager) getSystemService(Context.USB_SERVICE), this, ACTION_USB_PERMISSION);
        if (!MyApp.driver.UsbFeatureSupported()) {
            Dialog dialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
                    .setPositiveButton("确认",
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
                    showWaringDialog();
                }
                return;
            }
        }
    }

    private void showWaringDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("警告！")
                .setMessage("请前往设置->应用->PermissionDemo->权限中打开相关权限，否则功能无法正常运行！")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }

    //web_socket
    URI uri = URI.create("ws://139.224.12.24:2346");
    JWebSocketClient client = new JWebSocketClient(uri) {
        @Override
        public void onMessage(String message) {
            Log.e("JWebSClientService", message);
            try {
                JSONObject jsonObj = new JSONObject(message);
                if (jsonObj.length() == 2 && jsonObj.get("methon").equals("ZOOM")) {
                    android.hardware.Camera camera = null;
                    Camera.Parameters parameters = camera.getParameters();
                    parameters.setZoom(99);
                    camera.setParameters(parameters);
                } else {
                    boolean key_mode = PrefSingleton.getInstance().getBoolean("key_mode");
                    if (key_mode) {
                        new WebSocket().GetData(message);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NullPointerException e){

            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.settings: // 设置
                startActivity(new Intent(this, SetupActivity.class));
                break;
            case R.id.Calling: // 视频
                onCall();
                break;
            default:
                break;
        }
    }


    //Switch (ON/OFF)
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.button1:
                if (buttonView.isChecked()) {
                    switch1.setText("开");
                    PrefSingleton.getInstance().putBoolean("flow_mode", true);
                } else {
                    switch1.setText("关");
                    PrefSingleton.getInstance().putBoolean("flow_mode", false);
                }
                break;
            case R.id.button2:
                if (buttonView.isChecked()) {
                    onConnect();
                    if (isOpen) {
                        buttonView.setChecked(true);
                        switch2.setText("开");
                        PrefSingleton.getInstance().putBoolean("key_mode", true);
                    } else {
                        buttonView.setChecked(false);
                        switch2.setText("关");
                        PrefSingleton.getInstance().putBoolean("key_mode", false);
                    }
                } else {
                    onDisConnect();
                    if (!isOpen) {
                        buttonView.setChecked(false);
                        switch2.setText("关");
                        PrefSingleton.getInstance().putBoolean("key_mode", false);
                    } else {
                        buttonView.setChecked(true);
                        switch2.setText("开");
                        PrefSingleton.getInstance().putBoolean("key_mode", true);
                    }
                }
                break;
            case R.id.button3:
                if(buttonView.isChecked()) {
                    PrefSingleton.getInstance().putBoolean("voice_mode", true);
                    switch3.setText("开");
                } else {
                    PrefSingleton.getInstance().putBoolean("voice_mode", false);
                    switch3.setText("关");
                }
                break;
        }
    }

    // 定义单选监听器
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

    //拨打视频
    public void onCall(){
        /*String wss = PrefSingleton.getInstance().getString("Url");
        String uesr_id = "001";
        SocketManager.getInstance().connect(wss, uesr_id, 0);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SocketManager.getInstance().addUserStateCallback(this);
        int status = SocketManager.getInstance().getUserState();
        if (status == 1) {
            String phone = "002";
            SkyEngineKit.init(new VoipEvent());
            CallSingleActivity.openActivity(this, phone, true, false);
        }*/

        String wss = PrefSingleton.getInstance().getString("Url");
        String room_id = "122";
        try {
            Thread.sleep(100);
            //WebrtcUtil.callSingle(this, wss, room_id,true); // one to one
            WebrtcUtil.call(this, wss, room_id); // meeting
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //ch34x连接设备
    public void onConnect() {
        if (!isOpen) {
            int retval = MyApp.driver.ResumeUsbPermission();
            if (retval == 0) {
                retval = MyApp.driver.ResumeUsbList();
                if (retval == -1) {
                    Toast.makeText(getApplicationContext(), "Open failed!", Toast.LENGTH_SHORT).show();
                    MyApp.driver.CloseDevice();
                } else if (retval == 0){
                    if (MyApp.driver.mDeviceConnection != null) {
                        if (!MyApp.driver.UartInit()) {
                            Toast.makeText(getApplicationContext(), "Initialization failed!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "Device opened", Toast.LENGTH_SHORT).show();
                        isOpen = true;
                    } else {
                        Toast.makeText(getApplicationContext(), "Open failed!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("未授权限");
                    builder.setMessage("确认退出吗?");
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    });
                    builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                        }
                    });
                    builder.show();
                }
                MyApp.driver.SetConfig(baudRate, dataBit, stopBit, parity, flowControl); // 加载配置文件
                try {
                    if (!client.isOpen()) {
                        client.connectBlocking(); // web_socket连接
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onDisConnect() {
        if (isOpen) {
            isOpen = false;
            MyApp.driver.CloseDevice();
            Toast.makeText(getApplicationContext(), "Device closed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        BUS.unregister(this);
        isOpen = false;
        MyApp.driver.CloseDevice();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
