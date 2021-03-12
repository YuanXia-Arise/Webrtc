package org.ch34x;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.widget.Toast;

import org.activity.LauncherActivity;

/**
 * 类描述：CH34广播接收器。
 */
public class Ch34BroadcastReceiver extends BroadcastReceiver {

    private CH34xUARTDriver ch34xUARTDriver;

    public Ch34BroadcastReceiver(CH34xUARTDriver driver) {
        super();
        this.ch34xUARTDriver = driver;
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        //广播 USB启动
        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
            Log.d(CH34xUARTDriver.TAG, "Step1! USB启动");
            //广播 TODO USB权限
            //} else if (CH34xUARTDriver.a(this.ch34xUARTDriver).equals(var5)) {
        } else if (this.ch34xUARTDriver.getBroadcastReceiverFilter().equals(action)) {
            Log.d(CH34xUARTDriver.TAG, "Step2! 判断权限");
            Class var7 = CH34xUARTDriver.class;
            synchronized (CH34xUARTDriver.class) {
                UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra("device");
                if (intent.getBooleanExtra("permission", false)) {
                    //TODO 有USB权限
                    //CH34xUARTDriver.a(this.ch34xUARTDriver, var9);
                    this.ch34xUARTDriver.openDevice(usbDevice);
                } else {
                    //广播 TODO 拒绝USB权限
                    if (this.ch34xUARTDriver.isShowToast()) {
                        Toast.makeText(this.ch34xUARTDriver.getContext(), "拒绝USB权限，Deny USB Permission!", Toast.LENGTH_LONG).show();
                    }
                    Log.d(CH34xUARTDriver.TAG, "拒绝USB权限，Deny USB Permission!");
                }
            }

            //广播 USB设备移除
        } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
            UsbDevice var6; // USB设备
            //USB设备名称
            String var3 = (var6 = (UsbDevice) intent.getParcelableExtra("device")).getDeviceName();
            Log.d(CH34xUARTDriver.TAG, "Step3! USB设备移除，USB Disconnect! 设备名称=" + var3);
            for (int i = 0; i < this.ch34xUARTDriver.getSupportTypeSize(); ++i) {
                if (String.format("%04x:%04x", var6.getVendorId(), var6.getProductId()).equals(this.ch34xUARTDriver.getSupportVendorProduct().get(i))) {
                    if (this.ch34xUARTDriver.isShowToast()) {
                        //Toast.makeText(this.ch34xUARTDriver.getContext(), "USB 设备已移除，设备断开，请重新连接!", Toast.LENGTH_LONG).show();
                        /*Dialog dialog = new android.app.AlertDialog.Builder(context)
                                .setTitle("提示")
                                .setMessage("USB 设备已移除，设备断开，请重新连接")
                                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface arg0, int arg1) {
                                                arg0.dismiss();
                                            }
                                        }).create();
                        dialog.setCanceledOnTouchOutside(false);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                        } else {
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        }
                        dialog.show();*/

                        /*AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                        dialogBuilder.setTitle("提示");
                        dialogBuilder.setMessage("USB 设备已移除，连接断开，请重新连接");
                        dialogBuilder.setCancelable(false);
                        dialogBuilder.setPositiveButton("确定", null);
                        AlertDialog alertDialog = dialogBuilder.create();
                        alertDialog.setCanceledOnTouchOutside(true);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                        } else {
                            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        }
                        alertDialog.show();*/

                        LauncherActivity.getInstace().updateUI(false); // 更新UI界面

                    }
                    this.ch34xUARTDriver.closeDevice();
                }
            }
        } else {
            Log.e(CH34xUARTDriver.TAG, "没匹配到USB设备广播=" + action);
        }
    }

}
