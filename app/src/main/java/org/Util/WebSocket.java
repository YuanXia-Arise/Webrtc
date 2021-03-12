package org.Util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;

/**
 * 处理websocket回传的数据
 */
public class WebSocket {

    public void GetData(String inData) {
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(inData);
            if (jsonObj.length() == 3){
                String methon = (String) jsonObj.get("methon");
                String arg1 = String.valueOf(jsonObj.get("arg1"));
                send_data_a(methon, arg1); // 键盘
            } else if (jsonObj.length() == 4){
                String methon = (String) jsonObj.get("methon");
                int arg1 = (int) jsonObj.get("arg1");
                int arg2 = (int) jsonObj.get("arg2");
                send_data(methon, arg1, arg2); // 鼠标
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void send_data(String methon, int arg1, int arg2) {
        byte a1 = (byte) 0xf0; // 起始符
        byte a2 = 0x00;
        byte a7 = 0x00;
        byte x1,x2,y1,y2,a8;
        byte a9 = (byte) 0xff; // 结束符
        if (methon.equals("MouseMove")) {
            a2 = 0x01; // 指令
        }
        if (arg1 >= 0 && arg2 >= 0){
            a7 = (byte) 0xee;
        } else if (arg1 >= 0 && arg2 < 0){
            a7 = (byte) 0xe0;
        } else if (arg1 < 0 && arg2 < 0){
            a7 = (byte) 0x00;
        } else if (arg1 < 0 && arg2 >= 0){
            a7 = (byte) 0x0e;
        }

        if (arg1 >= 0) { // X轴
            x1 = (byte) (arg1/0xef);
            x2 = (byte) (arg1%0xef);
        } else {
            x1 = (byte) (-arg1/0xef);
            x2 = (byte) (-arg1%0xef);
        }
        if (arg2 >= 0) { // Y轴
            y1 = (byte) (arg2/0xef);
            y2 = (byte) (arg2%0xef);
        } else {
            y1 = (byte) (-arg2/0xef);
            y2 = (byte) (-arg2%0xef);
        }

        /*if (arg1 >= 0) { // X轴
            x1 = (byte) (toByteArray(String.valueOf(-arg1))[0]/0xef);
            x2 = 0x00%0xef;
        } else {
            x1 = 0x00/0xef;
            x2 = (byte) (toByteArray(String.valueOf(arg1))[0]%0xef);
        }
        if (arg2 >= 0) { // Y轴
            y1 = (byte) (toByteArray(String.valueOf(-arg2))[0]/0xef);
            y2 = 0x00%0xef;
        } else {
            y1 = 0x00/0xef;
            y2 = (byte) (toByteArray(String.valueOf(arg2))[0]%0xef);
        }*/

        a8 = (byte) ((a2 + x1 + x2 + y1 + y2 + a7) & 0xef); // 检验符

        byte[] to_send = {a1,a2,x1,x2,y1,y2,a7,a8,a9};
        Log.d(TAG, "鼠标:" + toHexString(to_send,to_send.length));
        //MyApp.driver.writeData(to_send, to_send.length); // 返回值为写入成功的字节数
        if (MyApp.driver.writeData(to_send, to_send.length) == 9) {
            Log.d(TAG, "写人成功");
        } else {
            Log.d(TAG, "写人失败");
        }
    }

    public void send_data_a(String methon, String arg1) {
        byte a1 = (byte) 0xf0; // 起始符
        byte a2 = 0x00,a3 = 0x00,a4 = 0x00,a5 = 0x00,a6 = 0x00,a8;
        byte a7 = 0x00;
        byte a9 = (byte) 0xff; // 结束符

        if (methon.equals("MouseUp")) {
            a2 = 0x04; // 指令
            //a3 = toByteArray(arg1)[0];
            if (Integer.valueOf(arg1) == 1) { // 左键返回1，中键返回2，右键返回3
                arg1 = "1";
            } else if (Integer.valueOf(arg1) == 2) {
                arg1 = "3";
            } else if (Integer.valueOf(arg1) == 3) {
                arg1 = "2";
            }
            a3 = Byte.parseByte(arg1);
        } else if (methon.equals("MouseDown")){
            a2 = 0x02; // 指令
            //a3 = toByteArray(arg1)[0];
            if (Integer.valueOf(arg1) == 1) { // 左键返回1，中键返回2，右键返回3
                arg1 = "1";
            } else if (Integer.valueOf(arg1) == 2) {
                arg1 = "3";
            } else if (Integer.valueOf(arg1) == 3) {
                arg1 = "2";
            }
            a3 = Byte.parseByte(arg1);
        } else if (methon.equals("KeyDown") && !arg1.equals("ReleaseAll")){
            a2 = 0x03;
            //a3 = toByteArray(convertStringToHex(arg1))[0];
            a3 = aByte(arg1);
        } else if (methon.equals("Keyrelease")){
            a2 = 0x05;
            //a3 = toByteArray(convertStringToHex(arg1))[0];
            a3 = aByte(arg1);
        }

        a8 = (byte) ((a2 + a3 + a4 + a5 + a6 + a7) & 0xef); // 检验符

        byte[] to_send = {a1,a2,a3,a4,a5,a6,a7,a8,a9};
        Log.d(TAG, "键盘:" + toHexString(to_send,to_send.length));
        //MyApp.driver.writeData(to_send, to_send.length); // 返回值为写入成功的字节数
        if (MyApp.driver.writeData(to_send, to_send.length) == 9) {
            Log.d(TAG, "写人成功");
        } else {
            Log.d(TAG, "写人失败");
        }
    }

    public byte aByte(String s){
        byte a;
        switch (s) {
            case "Escape":
                a = (byte) 0x90;
                break;
            case "F1":
                a = (byte) 0x11;
                break;
            case "F2":
                a = (byte) 0x12;
                break;
            case "F3":
                a = (byte) 0x13;
                break;
            case "F4":
                a = (byte) 0x14;
                break;
            case "F5":
                a = (byte) 0x15;
                break;
            case "F6":
                a = (byte) 0x16;
                break;
            case "F7":
                a = (byte) 0x17;
                break;
            case "F8":
                a = (byte) 0x18;
                break;
            case "F9":
                a = (byte) 0x19;
                break;
            case "F10":
                a = (byte) 0x1a;
                break;
            case "F11":
                a = (byte) 0x1c;
                break;
            case "F12":
                a = (byte) 0x1d;
                break;
            case "Home":
                a = (byte) 0x91;
                break;
            case "End":
                a = (byte) 0x92;
                break;
            case "Insert":
                a = (byte) 0x93;
                break;
            case "Delete":
                a = (byte) 0xa0;
                break;
            case "Tab":
                a = (byte) 0x95;
                break;
            case "CapsLock":
                a = (byte) 0x00;
                break;
            case "Shift":
                a = (byte) 0x10;
                break;
            case "Control":
                a = (byte) 0xa2;
                break;
            case "Alt":
                a = (byte) 0x98;
                break;
            case " ":
                a = (byte) 0x20;
                break;
            case "PageUp":
                a = (byte) 0x99;
                break;
            case "PageDown":
                a = (byte) 0x9a;
                break;
            case "ArrowLeft":
                a = (byte) 0x9d;
                break;
            case "ArrowUp":
                a = (byte) 0x9b;
                break;
            case "ArrowRight":
                a = (byte) 0x9e;
                break;
            case "ArrowDown":
                a = (byte) 0x9c;
                break;
            case "Enter":
                a = (byte) 0x0a;
                break;
            case "Backspace":
                a = (byte) 0xa1;
                break;
            case "{":
                a = (byte) 0x7b;
                break;
            case "}":
                a = (byte) 0x7d;
                break;
            case "\"":
                a = (byte) 0x22;
                break;
            case "\\":
                a = (byte) 0x5c;
                break;
            case "Meta":
                a = (byte) 0x9f;
                break;
            case "ReleaseAll":
                a = (byte) 0x8f;
                break;
            default:
                a = toByteArray(convertStringToHex(s))[0];
                break;
        }
        return a;
    }

    // Ascii码转String
    public String convertStringToHex(String str){
        char[] chars = str.toCharArray();
        StringBuffer hex = new StringBuffer();
        for(int i = 0; i < chars.length; i++){
            hex.append(Integer.toHexString((int)chars[i]));
        }
        return hex.toString();
    }

    // String转byte[]数组
    private byte[] toByteArray(String arg) {
        if (arg != null) {
            char[] NewArray = new char[1000];
            char[] array = arg.toCharArray();
            int length = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i] != ' ') {
                    NewArray[length] = array[i];
                    length++;
                }
            }
            int EvenLength = (length % 2 == 0) ? length : length + 1;
            if (EvenLength != 0) {
                int[] data = new int[EvenLength];
                data[EvenLength - 1] = 0;
                for (int i = 0; i < length; i++) {
                    if (NewArray[i] >= '0' && NewArray[i] <= '9') {
                        data[i] = NewArray[i] - '0';
                    } else if (NewArray[i] >= 'a' && NewArray[i] <= 'f') {
                        data[i] = NewArray[i] - 'a' + 10;
                    } else if (NewArray[i] >= 'A' && NewArray[i] <= 'F') {
                        data[i] = NewArray[i] - 'A' + 10;
                    }
                }
                byte[] byteArray = new byte[EvenLength / 2];
                for (int i = 0; i < EvenLength / 2; i++) {
                    byteArray[i] = (byte) (data[i * 2] * 16 + data[i * 2 + 1]);
                }
                return byteArray;
            }
        }
        return new byte[] {};
    }

    private byte[] toByteArray2(String arg) {
        if (arg != null) {
            char[] NewArray = new char[1000];
            char[] array = arg.toCharArray();
            int length = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i] != ' ') {
                    NewArray[length] = array[i];
                    length++;
                }
            }
            byte[] byteArray = new byte[length];
            for (int i = 0; i < length; i++) {
                byteArray[i] = (byte)NewArray[i];
            }
            return byteArray;
        }
        return new byte[] {};
    }

    private String toHexString(byte[] arg, int length) {
        String result = new String();
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                result = result + (Integer.toHexString(arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                        + Integer.toHexString(arg[i] < 0 ? arg[i] + 256 : arg[i])
                        : Integer.toHexString(arg[i] < 0 ? arg[i] + 256 : arg[i])) + " ";
            }
            return result;
        }
        return "";
    }

}
