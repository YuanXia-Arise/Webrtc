package com.dds.webrtclib;

import android.graphics.Bitmap;
import android.os.Environment;

import org.webrtc.PrefSingleton;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static androidx.core.math.MathUtils.clamp;
import static java.lang.System.in;

public class Util {

    private String fileName = "/byte.txt";

    //write
    public void createFileWithByte(byte[] bytes, int width, int height) {
        // TODO Auto-generated method stub
        File file = new File(Environment.getExternalStorageDirectory() + fileName);
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            outputStream = new FileOutputStream(file); // 获取FileOutputStream对象
            bufferedOutputStream = new BufferedOutputStream(outputStream); // 获取BufferedOutputStream对象

            if (PrefSingleton.getInstance().getBoolean("flow_mode")) {
                //bufferedOutputStream.write(fetchNV21(createBitmap(bytes, width, height)));
                bufferedOutputStream.write(fetch(btoi(bytes, width, height),width, height));
            } else {
                bufferedOutputStream.write(bytes);
            }
            //bufferedOutputStream.write(bytes); // 往文件所在的缓冲输出流中写byte数据
            //bufferedOutputStream.write(fetchNV21(createBitmap(bytes, width, height)));
            bufferedOutputStream.flush(); // 刷新缓冲流
        } catch (Exception e) {
            e.printStackTrace();
        } finally { // 关闭创建的流对象
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }


    public byte[] readFileToByteArray() {
        File file = new File(Environment.getExternalStorageDirectory() + fileName);
        if(!file.exists()) {
            return null;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            long inSize = in.getChannel().size(); // 判断FileInputStream中是否有内容
            if (inSize == 0) {
                return null;
            }

            byte[] buffer = new byte[in.available()];
            in.read(buffer);  // 将文件中的数据读到buffer中
            return buffer;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                return null;
            }
        }
    }


    /**
     * UVC byte[] nv21Yuv 数据灰度处理
     * byte转bitmap
     * bitmap中提取YUV分量
     */
    public static Bitmap createBitmap(byte[] values, int picW, int picH) {
        if(values == null || picW <= 0 || picH <= 0)
            return null;
        Bitmap bitmap = Bitmap.createBitmap(picW, picH, Bitmap.Config.ARGB_8888);
        int pixels[] = new int[picW * picH];
        for (int i = 0; i < pixels.length; ++i) {
            pixels[i] = values[i] * 256 * 256 + values[i] * 256 + values[i] + 0xFF000000;
        }
        bitmap.setPixels(pixels, 0, picW, 0, 0, picW, picH);
        values = null;
        pixels = null;
        return bitmap;
    }

    public byte[] fetchNV21(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int size = w * h;
        int[] pixels = new int[size];
        bitmap.getPixels(pixels,0, w,0,0, w, h);
        byte[] nv21 = new byte[size * 3 / 2];

        w &= ~1;
        h &= ~1;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int yIndex = i * w + j;

                int argb = pixels[yIndex];
                int a = (argb >> 24) & 0xff;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;

                int y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                y = clamp(y, 16, 255);
                nv21[yIndex] = (byte)y;

                if (i % 2 == 0 && j % 2 == 0) {
                    int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                    int v = ((112 * r - 94 * g -18 * b + 128) >> 8) + 128;

                    u = clamp(u, 0, 255);
                    v = clamp(v, 0, 255);

                    nv21[size + i / 2 * w + j] = (byte) v;
                    nv21[size + i / 2 * w + j + 1] = (byte) u;
                }
            }
        }
        return nv21;
    }

    //灰度处理延时优化
    public static int[] btoi(byte[] values, int picW, int picH){
        if (values == null || picW <= 0 || picH <= 0)
            return null;
        int pixels[] = new int[picW * picH];
        int size = pixels.length;
        for (int i = 0; i < size; i++) {
            pixels[i] = values[i] * 256 * 256 + values[i] * 256 + values[i] + 0xFF000000;
        }
        return pixels;
    }

    public byte[] fetch(int[] pixels, int w, int h) {
        int size = w * h;
        Bitmap bitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        byte[] nv21 = new byte[size * 3 / 2];
        int i, j;
        for (i = 0; i < h; i++) {
            for (j = 0; j < w; j++) {
                int yIndex = i * w + j;

                int argb = pixels[yIndex];
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;

                int y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                y = clamp(y, 16, 255);
                nv21[yIndex] = (byte)y;

                if (i % 2 == 0 && j % 2 == 0) {
                    int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                    int v = ((112 * r - 94 * g -18 * b + 128) >> 8) + 128;
                    u = clamp(u, 0, 255);
                    v = clamp(v, 0, 255);
                    nv21[size + i / 2 * w + j] = (byte) v;
                    nv21[size + i / 2 * w + j + 1] = (byte) u;
                }
            }
        }
        return nv21;
    }


}
