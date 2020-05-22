package org.easydarwin.easypusher;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.easydarwin.bus.StopRecord;
import org.easydarwin.bus.StreamStat;
import org.easydarwin.bus.SupportResolution;
import org.easydarwin.push.EasyPusher;
import org.easydarwin.push.InitCallback;
import org.easydarwin.push.MediaStream;
import org.easydarwin.util.Config;
import org.easydarwin.util.SPUtil;
import org.easydarwin.util.Util;

import org.JWebSocketClient;
import org.MyApp;
import org.WebSocket;

import com.dds.skywebrtc.SkyEngineKit;
import com.squareup.otto.Subscribe;

import org.java.socket.SocketManager;
import org.java.voip.CallSingleActivity;
import org.java.voip.VoipEvent;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.easydarwin.easypusher.EasyApplication.BUS;

/**
 * 预览+推流等主页
 * */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {
    static final String TAG = "StreamActivity";

    public static final int REQUEST_MEDIA_PROJECTION = 1002;
    public static final int REQUEST_CAMERA_PERMISSION = 1003;
    public static final int REQUEST_STORAGE_PERMISSION = 1004;


    // 默认分辨率
    int width = 1920, height = 1080; //1280*720

    TextView txtStreamAddress;
    ImageView btnSwitchCemera;
    Spinner spnResolution;
    TextView txtStatus, streamStat;
    TextView textRecordTick;

    List<String> listResolution = new ArrayList<>();

    MediaStream mMediaStream;

    static Intent mResultIntent;
    static int mResultCode;

    private BackgroundCameraService mService;
    private ServiceConnection conn;

    private boolean mNeedGrantedPermission;

    private static final String STATE = "state";
    private static final int MSG_STATE = 1;

    private long mExitTime;//声明一个long类型变量：用于存放上一点击“返回键”的时刻


    private boolean isOpen;
    private TextView open;
    public int baudRate;
    public byte stopBit;
    public byte dataBit;
    public byte parity;
    public byte flowControl;
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
    private LinearLayout mLinearLayout;
    private LinearLayout mCall;

    // 录像时的线程
    private Runnable mRecordTickRunnable = new Runnable() {
        @Override
        public void run() {
            long duration = System.currentTimeMillis() - EasyApplication.getEasyApplication().mRecordingBegin;
            duration /= 1000;
            textRecordTick.setText(String.format("%02d:%02d", duration / 60, (duration) % 60));
            if (duration % 2 == 0) {
                textRecordTick.setCompoundDrawablesWithIntrinsicBounds(R.drawable.recording_marker_shape, 0, 0, 0);
            } else {
                textRecordTick.setCompoundDrawablesWithIntrinsicBounds(R.drawable.recording_marker_interval_shape, 0, 0, 0);
            }
            textRecordTick.removeCallbacks(this);
            textRecordTick.postDelayed(this, 1000);
        }
    };

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STATE:
                    String state = msg.getData().getString("state");
                    txtStatus.setText(state);
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); //全屏
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BUS.register(this);
        //notifyAboutColorChange();
        // 动态获取camera和audio权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQUEST_CAMERA_PERMISSION);
            mNeedGrantedPermission = true;
            return;
        } else {
            // resume
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //设置横屏

        isOpen = false;
        open = findViewById(R.id.open);
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

        mLinearLayout = findViewById(R.id.onConnect);
        mLinearLayout.setOnClickListener(this);
        mCall = findViewById(R.id.onCall);
        mCall.setOnClickListener(this);

        try {
            client.connectBlocking(); //web_socket连接
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String wss = "ws://47.93.186.97:5000/ws";
        String uesr_id = "001";
        SocketManager.getInstance().connect(wss, uesr_id, 0);

    }

    @Override
    protected void onPause() {
        if (!mNeedGrantedPermission) {
            unbindService(conn);
            handler.removeCallbacksAndMessages(null);
        }
        boolean isStreaming = mMediaStream != null && mMediaStream.isStreaming();

        if (mMediaStream != null) {
            mMediaStream.stopPreview();

            if (isStreaming && SPUtil.getEnableBackgroundCamera(this)) {
                mService.activePreview();
            } else {
                mMediaStream.stopStream();
                mMediaStream.release();
                mMediaStream = null;
                stopService(new Intent(this, BackgroundCameraService.class));
            }
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //设置横屏
        if (!mNeedGrantedPermission) {
            goonWithPermissionGranted();
        }
    }

    @Override
    protected void onDestroy() {
        BUS.unregister(this);
        isOpen = false;
        MyApp.driver.CloseDevice();
        super.onDestroy();
    }

    /*
     * android6.0权限，onRequestPermissionsResult回调
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult granted");
                } else {
                    Log.i(TAG, "onRequestPermissionsResult denied");
                    showWaringDialog();
                }
                return;
            }
            /*case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    update.doDownload();
                }
                break;*/
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mNeedGrantedPermission = false;
                    goonWithPermissionGranted();
                } else {
                    finish();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                Log.e(TAG, "get capture permission success!");

                mResultCode = resultCode;
                mResultIntent = data;
            }
        }
    }

    private void goonWithPermissionGranted() {
        spnResolution = findViewById(R.id.spn_resolution);
        streamStat = findViewById(R.id.stream_stat);
        txtStatus = findViewById(R.id.txt_stream_status);
        btnSwitchCemera = findViewById(R.id.btn_switchCamera);
        txtStreamAddress = findViewById(R.id.txt_stream_address);
        textRecordTick = findViewById(R.id.tv_start_record);
        final TextureView surfaceView = findViewById(R.id.sv_surfaceview);

        streamStat.setText(null);
        btnSwitchCemera.setOnClickListener(this);
        surfaceView.setSurfaceTextureListener(this);
        surfaceView.setOnClickListener(this);
        /*surfaceView.setOnTouchListener(new MyClickListener(new MyClickListener.MyClickCallBack() {
                    @Override
                    public void oneClick() {

                    }

                    @Override
                    public void doubleClick() {
                    }
                }));*/

        // create background service for background use.
        Intent intent = new Intent(this, BackgroundCameraService.class);
        startService(intent);

        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mService = ((BackgroundCameraService.LocalBinder) iBinder).getService();

                if (surfaceView.isAvailable()) {
                    goonWithAvailableTexture(surfaceView.getSurfaceTexture());
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };

        bindService(new Intent(this, BackgroundCameraService.class), conn, 0);

        if (EasyApplication.getEasyApplication().mRecording) {
            textRecordTick.setVisibility(View.VISIBLE);
            textRecordTick.removeCallbacks(mRecordTickRunnable);
            textRecordTick.post(mRecordTickRunnable);
        } else {
            textRecordTick.setVisibility(View.INVISIBLE);
            textRecordTick.removeCallbacks(mRecordTickRunnable);
        }

    }

    /*
     * 初始化MediaStream
     * */
    private void goonWithAvailableTexture(SurfaceTexture surface) {
        final File easyPusher = new File(Config.recordPath());
        easyPusher.mkdir();

        MediaStream ms = mService.getMediaStream();
        if (ms != null) {    // switch from background to front
            ms.stopPreview();
            mService.inActivePreview();
            ms.setSurfaceTexture(surface);
            ms.startPreview();
            mMediaStream = ms;

            if (ms.isStreaming()) {
                String url = Config.getServerURL(this);
                txtStreamAddress.setText(url);
                sendMessage("推流中");
                ImageView startPush = findViewById(R.id.streaming_activity_push);
                startPush.setImageResource(R.drawable.start_push_pressed);
            }
        } else {
            boolean enableVideo = SPUtil.getEnableVideo(this);
            ms = new MediaStream(getApplicationContext(), surface, enableVideo);
            ms.setRecordPath(easyPusher.getPath());
            mMediaStream = ms;
            startCamera();
            mService.setMediaStream(ms);
        }
    }


    private void startCamera() {
        mMediaStream.updateResolution(width, height);
        mMediaStream.setDgree(getDisplayRotationDegree());
        mMediaStream.createCamera();
        mMediaStream.startPreview();

        if (mMediaStream.isStreaming()) {
            sendMessage("推流中");
            txtStreamAddress.setText(Config.getServerURL(this));
        }
    }

    // 屏幕的角度
    private int getDisplayRotationDegree() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }
        return degrees;
    }

    /*
     * 初始化下拉控件的列表（显示分辨率）
     * */
    private void initSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spn_item, listResolution);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnResolution.setAdapter(adapter);

        int position = listResolution.indexOf(String.format("%dx%d", width, height));
        spnResolution.setSelection(position, false);

        spnResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mMediaStream != null && mMediaStream.isStreaming()) {
                    int pos = listResolution.indexOf(String.format("%dx%d", width, height));
                    if (pos == position)
                        return;
                    spnResolution.setSelection(pos, false);
                    Toast.makeText(getApplicationContext(), "正在推送中,无法切换分辨率", Toast.LENGTH_SHORT).show();
                    return;
                }

                String r = listResolution.get(position);
                String[] splitR = r.split("x");

                int wh = Integer.parseInt(splitR[0]);
                int ht = Integer.parseInt(splitR[1]);

                if (width != wh || height != ht) {
                    width = wh;
                    height = ht;
                    if (mMediaStream != null) {
                        mMediaStream.updateResolution(width, height);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /*
     * 得知停止录像
     * */
    @Subscribe
    public void onStopRecord(StopRecord sr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textRecordTick.setVisibility(View.INVISIBLE);
                textRecordTick.removeCallbacks(mRecordTickRunnable);

                /*ImageView ib = findViewById(R.id.streaming_activity_record);
                ib.setImageResource(R.drawable.record);*/
            }
        });
    }

    /*
     * 开始推流，获取fps、bps
     * */
    @Subscribe
    public void onStreamStat(final StreamStat stat) {
        streamStat.post(() ->
                streamStat.setText(getString(R.string.stream_stat,
                        stat.framePerSecond,
                        stat.bytesPerSecond * 8 / 1024))
        );
    }

    /*
     * 获取可以支持的分辨率
     * */
    @Subscribe
    public void onSupportResolution(SupportResolution res) {
        runOnUiThread(() -> {
            listResolution = Util.getSupportResolution(getApplicationContext());
            boolean supportdefault = listResolution.contains(String.format("%dx%d", width, height));
            if (!supportdefault) {
                String r = listResolution.get(0);
                String[] splitR = r.split("x");

                width = Integer.parseInt(splitR[0]);
                height = Integer.parseInt(splitR[1]);
            }
            initSpinner();
        });
    }

    /*
     * 显示推流的状态
     * */
    private void sendMessage(String message) {
        Message msg = Message.obtain();
        msg.what = MSG_STATE;
        Bundle bundle = new Bundle();
        bundle.putString(STATE, message);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    /* ========================= 点击事件 ========================= */

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        boolean isStreaming = mMediaStream != null && mMediaStream.isStreaming();
        if (isStreaming && SPUtil.getEnableBackgroundCamera(this)) {
            new AlertDialog.Builder(this).setTitle("是否允许后台上传？")
                    .setMessage("您设置了使能摄像头后台采集,是否继续在后台采集并上传视频？如果是，记得直播结束后,再回来这里关闭直播。")
                    .setNeutralButton("后台采集", (dialogInterface, i) -> {
                        MainActivity.super.onBackPressed();
                    })
                    .setPositiveButton("退出程序", (dialogInterface, i) -> {
                        mMediaStream.stopStream();
                        MainActivity.super.onBackPressed();
                        Toast.makeText(getApplicationContext(), "程序已退出。", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        //与上次点击返回键时刻作差
        if ((System.currentTimeMillis() - mExitTime) > 2000) {//大于2000ms则认为是误操作，使用Toast进行提示
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            //并记录下本次点击“返回键”的时刻，以便下次进行判断
            mExitTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sv_surfaceview:
                try {
                    mMediaStream.getCamera().autoFocus(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_switchCamera:
                mMediaStream.switchCamera();
                break;
            case R.id.onConnect:
                onConnect();
                break;
            case R.id.onCall:
                onCall();
                break;
            default:
                break;
        }
    }

    /*
     * 切换分辨率
     * */
    public void onClickResolution(View view) {
        findViewById(R.id.spn_resolution).performClick();
    }

    /*
     * 切换屏幕方向
     * */
    public void onSwitchOrientation(View view) {
        if (mMediaStream != null) {
            if (mMediaStream.isStreaming()){
                Toast.makeText(this,"正在推送中,无法更改屏幕方向", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int orientation = getRequestedOrientation();
        if (orientation == SCREEN_ORIENTATION_UNSPECIFIED || orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    /*
     * 推流/停止
     * */
    public void onStartOrStopPush(View view) {
        ImageView ib = findViewById(R.id.streaming_activity_push);

        if (!mMediaStream.isStreaming()) {
            String url = Config.getServerURL(this); //rtsp://139.224.12.24:554/1111
            String ip = Config.getIp(this); //139.224.12.24
            String port = Config.getPort(this); //554
            String id = Config.getId(this); //1111
            mMediaStream.startStream(ip, port, id, new InitCallback() {
                @Override
                public void onCallback(int code) {
                    switch (code) {
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_INVALID_KEY:
                            sendMessage("无效Key");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_SUCCESS:
                            sendMessage("激活成功");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_CONNECTING:
                            sendMessage("连接中");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_CONNECTED:
                            sendMessage("连接成功");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_CONNECT_FAILED:
                            sendMessage("连接失败");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_CONNECT_ABORT:
                            sendMessage("连接异常中断");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_PUSHING:
                            sendMessage("推流中");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_PUSH_STATE_DISCONNECTED:
                            sendMessage("断开连接");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_PLATFORM_ERR:
                            sendMessage("平台不匹配");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_COMPANY_ID_LEN_ERR:
                            sendMessage("COMPANY不匹配");
                            break;
                        case EasyPusher.OnInitPusherCallback.CODE.EASY_ACTIVATE_PROCESS_NAME_LEN_ERR:
                            sendMessage("进程名称长度不匹配");
                            break;
                    }
                }
            });

            ib.setImageResource(R.drawable.start_push_pressed);
            txtStreamAddress.setText(url);
        } else {
            mMediaStream.stopStream();
            ib.setImageResource(R.drawable.start_push);
            sendMessage("断开连接");
        }
    }

    /*
     * 设置
     * */
    public void onSetting(View view) {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivityForResult(intent, 0);
        overridePendingTransition(R.anim.slide_right_in,R.anim.slide_left_out);
    }

    /* ========================= TextureView.SurfaceTextureListener ========================= */

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
        if (mService != null) {
            goonWithAvailableTexture(surface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    //web_socket
    URI uri = URI.create("ws://139.224.12.24:2346");
    JWebSocketClient client = new JWebSocketClient(uri) {
        @Override
        public void onMessage(String message) {
            Log.e("JWebSClientService", message);
            try {
                JSONObject jsonObj = new JSONObject(message);
                if (jsonObj.length() == 2 && jsonObj.get("methon").equals("ZOOM")) { //焦距调节
                    int zoom = Integer.valueOf((Integer) jsonObj.get("arg1"));
                    Camera.Parameters p = mMediaStream.getCamera().getParameters();
                    p.setZoom(Integer.valueOf((int) (zoom * (99f / 100f))));
                    mMediaStream.getCamera().setParameters(p);
                } else {
                    new WebSocket().GetData(message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    };

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
                        open.setText("断开");
                    } else {
                        Toast.makeText(getApplicationContext(), "Open failed!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("未授权限");
                    builder.setMessage("确认退出吗？");
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
            }
        } else {
            open.setText("连接");
            isOpen = false;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            MyApp.driver.CloseDevice();
        }
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "checkSelfPermission");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_NETWORK_STATE)) {
                Log.i(TAG, "shouldShowRequestPermissionRationale");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE},1);
            } else {
                Log.i(TAG, "requestPermissions");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                        1);
            }
        }
    }


    private void showWaringDialog() {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("警告！")
                .setMessage("请前往设置->应用->PermissionDemo->权限中打开相关权限，否则功能无法正常运行！")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }

    //一对一拨打视频
    public void onCall(){
        boolean isStreaming = mMediaStream != null && mMediaStream.isStreaming();
        if (mMediaStream != null) {
            mMediaStream.stopPreview();
            if (isStreaming && SPUtil.getEnableBackgroundCamera(this)) {
                mService.activePreview();
            } else {
                mMediaStream.stopStream();
                mMediaStream.release();
                mMediaStream = null;
                stopService(new Intent(this, BackgroundCameraService.class));
            }
        }
        /*String wss = "ws://47.93.186.97:5000/ws";
        String uesr_id = "001";
        SocketManager.getInstance().connect(wss, uesr_id, 0);*/
        String phone = "002";
        SkyEngineKit.init(new VoipEvent());
        CallSingleActivity.openActivity(this, phone, true, false);
    }
}

