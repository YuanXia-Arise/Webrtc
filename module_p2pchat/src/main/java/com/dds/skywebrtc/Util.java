package com.dds.skywebrtc;

import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static java.lang.System.in;

public class Util {

    private String fileName = "/byte.txt";

    public void createFileWithByte(byte[] bytes) {
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
            bufferedOutputStream.write(bytes); // 往文件所在的缓冲输出流中写byte数据
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
            System.out.println("File doesn't exist!");
            return null;
        }
        try {
            FileInputStream in = new FileInputStream(file);
            long inSize = in.getChannel().size(); // 判断FileInputStream中是否有内容
            if (inSize == 0) {
                System.out.println("The FileInputStream has no content!");
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

}
