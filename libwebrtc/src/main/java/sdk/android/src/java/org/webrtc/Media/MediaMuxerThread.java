package org.webrtc.Media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import org.webrtc.PrefSingleton;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * @author huangche
 * @date 2021/01/27
 * 音视频混合线程
 */
public class MediaMuxerThread extends Thread {

    private static final String TAG = "MediaMuxerThread";

    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;

    private final Object lock = new Object();

    private static MediaMuxerThread mediaMuxerThread;

    private AudioEncoderThread audioThread;
    private VideoEncoderThread videoThread;

    private MediaMuxer mediaMuxer;
    private Vector<MuxerData> muxerDatas;

    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;

    private FileUtils fileSwapHelper;

    // 音轨添加状态
    private volatile boolean isVideoTrackAdd;
    private volatile boolean isAudioTrackAdd;

    private volatile boolean isExit = false;

    private MediaMuxerThread() {
        // 构造函数
    }

    // 开始音视频混合任务
    public static void startMuxer() {
        if (mediaMuxerThread == null) {
            synchronized (MediaMuxerThread.class) {
                if (mediaMuxerThread == null) {
                    mediaMuxerThread = new MediaMuxerThread();
                    mediaMuxerThread.start();
                }
            }
        }
    }

    // 停止音视频混合任务
    public static void stopMuxer() {
        if (mediaMuxerThread != null) {
            mediaMuxerThread.exit();
            try {
                mediaMuxerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaMuxerThread = null;
        }
    }

    private void readyStart() throws IOException {
        fileSwapHelper.requestSwapFile(true);
        readyStart(fileSwapHelper.getNextFileName());
    }

    private void readyStart(String filePath) throws IOException {
        isExit = false;
        isVideoTrackAdd = false;
        isAudioTrackAdd = false;
        muxerDatas.clear();

        mediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        if (audioThread != null) {
            audioThread.setMuxerReady(true);
        }
        if (videoThread != null) {
            videoThread.setMuxerReady(true);
        }
    }

    // 添加视频帧数据
    public static void addVideoFrameData(byte[] data) {
        if (mediaMuxerThread != null) {
            mediaMuxerThread.addVideoData(data);
        }
    }

    public void addMuxerData(MuxerData data) {
        if (!isMuxerStart()) {
            return;
        }

        muxerDatas.add(data);
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * 添加视频／音频轨
     *
     * @param index
     * @param mediaFormat
     */
    public synchronized void addTrackIndex(int index, MediaFormat mediaFormat) {
        if (isMuxerStart()) {
            return;
        }

        /* 如果已经添加了，就不做处理了 */
        if ((index == TRACK_AUDIO && isAudioTrackAdd()) || (index == TRACK_VIDEO && isVideoTrackAdd())) {
            return;
        }

        if (mediaMuxer != null) {
            int track = 0;
            try {
                track = mediaMuxer.addTrack(mediaFormat);
            } catch (Exception e) {
                return;
            }

            if (index == TRACK_VIDEO) {
                videoTrackIndex = track;
                isVideoTrackAdd = true;
            } else {
                audioTrackIndex = track;
                isAudioTrackAdd = true;
            }
            requestStart();
        }
    }

    /**
     * 请求混合器开始启动
     */
    private void requestStart() {
        synchronized (lock) {
            if (isMuxerStart()) {
                mediaMuxer.start();
                lock.notify();
            }
        }
    }

    /**
     * 当前是否添加了音轨
     *
     * @return
     */
    public boolean isAudioTrackAdd() {
        return isAudioTrackAdd;
    }

    /**
     * 当前是否添加了视频轨
     *
     * @return
     */
    public boolean isVideoTrackAdd() {
        return isVideoTrackAdd;
    }

    /**
     * 当前音视频合成器是否运行了
     *
     * @return
     */
    public boolean isMuxerStart() {
        return isAudioTrackAdd && isVideoTrackAdd;
    }


    // 添加视频数据
    private void addVideoData(byte[] data) {
        if (videoThread != null) {
            videoThread.add(data);
        }
    }

    private void initMuxer() {
        muxerDatas = new Vector<>();
        fileSwapHelper = new FileUtils();
        audioThread = new AudioEncoderThread((new WeakReference<MediaMuxerThread>(this)));
        //videoThread = new VideoEncoderThread(1920, 1080, new WeakReference<MediaMuxerThread>(this));
        videoThread = new VideoEncoderThread(PrefSingleton.getInstance().getInt("width"),
                PrefSingleton.getInstance().getInt("height"), new WeakReference<MediaMuxerThread>(this));
        audioThread.start();
        videoThread.start();
        try {
            readyStart();
        } catch (IOException e) {
        }
    }

    @Override
    public void run() {
        super.run();
        // 初始化混合器
        initMuxer();
        while (!isExit) {
            if (isMuxerStart()) {
                if (muxerDatas.isEmpty()) {
                    synchronized (lock) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (fileSwapHelper.requestSwapFile()) {
                        //需要切换文件
                        String nextFileName = fileSwapHelper.getNextFileName();
                        restart(nextFileName);
                    } else {
                        MuxerData data = muxerDatas.remove(0);
                        int track;
                        if (data.trackIndex == TRACK_VIDEO) {
                            track = videoTrackIndex;
                        } else {
                            track = audioTrackIndex;
                        }
                        try {
                            mediaMuxer.writeSampleData(track, data.byteBuf, data.bufferInfo);
                        } catch (Exception e) {
                        }
                    }
                }
            } else {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        readyStop();
    }

    private void restart() {
        fileSwapHelper.requestSwapFile(true);
        String nextFileName = fileSwapHelper.getNextFileName();
        restart(nextFileName);
    }

    private void restart(String filePath) {
        restartAudioVideo();
        readyStop();

        try {
            readyStart(filePath);
        } catch (Exception e) {
            restart();
            return;
        }
    }


    private void readyStop() {
        if (mediaMuxer != null) {
            try {
                mediaMuxer.stop();
            } catch (Exception e) {
            }
            try {
                mediaMuxer.release();
            } catch (Exception e) {
            }
            mediaMuxer = null;
        }
    }

    private void restartAudioVideo() {
        if (audioThread != null) {
            audioTrackIndex = -1;
            isAudioTrackAdd = false;
            audioThread.restart();
        }
        if (videoThread != null) {
            videoTrackIndex = -1;
            isVideoTrackAdd = false;
            videoThread.restart();
        }
    }

    private void exit() {
        if (videoThread != null) {
            videoThread.exit();
            try {
                videoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (audioThread != null) {
            audioThread.exit();
            try {
                audioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        isExit = true;
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {

        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }


}