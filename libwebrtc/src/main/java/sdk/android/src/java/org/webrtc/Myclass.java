package org.webrtc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.provider.Settings;

import org.webrtc.Media.NV21ToBitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static androidx.core.math.MathUtils.clamp;


public class Myclass {

    public byte[] dealByte(byte[] dst, int width, int height, Context context) {
        //Bitmap bitmapAll = nv21ToBitmap(dst, width, height);
        //Bitmap bitmapAllNew = bitmapAll.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap bitmapAllNew = new NV21ToBitmap(context).nv21ToBitmap(dst, width, height);
        Canvas canvas = new Canvas(bitmapAllNew);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40*height/1080);
        paint.setTextAlign(Paint.Align.CENTER);
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
        //String a = sdf2.format(new Date(Long.parseLong(String.valueOf(System.currentTimeMillis()))));
        String a = sdf2.format(new Date());
        canvas.drawText(a, width/2, height*1/6, paint);
        byte[] newBytes = bitmapToNv21(bitmapAllNew, width, height);
        //byte[] newBytes = fetchNV21(bitmapAllNew);
        if (newBytes != null) {
            return newBytes;
        } else {
            return null;
        }
    }

    public byte[] fetchNV21(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int size = w * h;
        int[] pixels = new int[size];
        bitmap.getPixels(pixels,0, w,0,0, w, h);
        byte[] nv21 = new byte[size * 3 / 2];
        //w &= ~1; h &= ~1;
        int i, j;
        for (i = 0; i < h; i++) {
            for (j = 0; j < w; j++) {
                int yIndex = i * w + j;

                int argb = pixels[yIndex];
                //int a = (argb >> 24) & 0xff;
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

    public Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    //bitmap to NV21
    public byte[] bitmapToNv21(Bitmap src, int width, int height) {
        if (src != null && src.getWidth() >= width && src.getHeight() >= height) {
            int[] argb = new int[width * height];
            src.getPixels(argb, 0, width, 0, 0, width, height);
            return argbToNv21(argb, width, height);
        } else {
            return null;
        }
    }

    public byte[] argbToNv21(int[] argb, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int index = 0;
        byte[] nv21 = new byte[width * height * 3 / 2];
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                int R = (argb[index] & 0xFF0000) >> 16;
                int G = (argb[index] & 0x00FF00) >> 8;
                int B = argb[index] & 0x0000FF;
                int Y = (66 * R + 129 * G + 25 * B + 128 >> 8) + 16;
                int U = (-38 * R - 74 * G + 112 * B + 128 >> 8) + 128;
                int V = (112 * R - 94 * G - 18 * B + 128 >> 8) + 128;
                nv21[yIndex++] = (byte) (Y < 0 ? 0 : (Y > 255 ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < nv21.length - 2) {
                    nv21[uvIndex++] = (byte) (V < 0 ? 0 : (V > 255 ? 255 : V));
                    nv21[uvIndex++] = (byte) (U < 0 ? 0 : (U > 255 ? 255 : U));
                }
                ++index;
            }
        }
        return nv21;
    }

    // to image file
    public void bytesToImageFile(byte[] bytes,String str,int width,int height) {
        try {
            // 格式成YUV格式
            YuvImage yuvimage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
            Bitmap bitmap = bytes2Bimap(baos.toByteArray());
            saveImage(bitmap,str);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Bitmap bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    public void saveImage(Bitmap bmp,String str) {
        File appDir = new File("/storage/emulated/0/Download/2021/", "pic");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = str + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
