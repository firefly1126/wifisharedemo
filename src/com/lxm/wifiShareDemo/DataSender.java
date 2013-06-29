package com.lxm.wifiShareDemo;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据发送者
 * @author simon.L
 * @version 1.0.0
 */
public class DataSender {
    /**结束字符*/
    public static final String END_SYMBOL = "\r\n";

    private static final int STATE_IDLE = 0;
    private static final int STATE_BUSY = 1;

    private static final String LOG_TAG = "DataSender";

    private static final int WHAT_SEND = 1;

    private List<MediaItem> mSendList;

    private int mWorkState;

    private Handler mUiHandler;
    private DataTransferListener mDataTransferListener;
    private String mIpString;

    /**
     * 用ip字串来构造对象
     * @param ipString ip字串，格式:xxx.xxx.xxx.xxx,例如：192.168.1.1
     */
    public DataSender(String ipString, DataTransferListener listener) {
        mIpString = ipString;
        mSendList = new ArrayList<MediaItem>();
        mWorkState = STATE_IDLE;
        mDataTransferListener = listener;

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case WHAT_SEND:
                        sendEvent(mSendList.remove(0));
                        break;
                    default:
                        break;
                }
            }
        };
    }

    /**
     * 传输 media数据
     * @param item  MediaItem
     */
    public void send(MediaItem item) {
        mSendList.add(item);
        if (mWorkState == STATE_IDLE) {
            mUiHandler.sendEmptyMessage(WHAT_SEND);
        }
    }

    private void sendHead(OutputStream outputStream, final MediaItem mediaItem) {
        if (mDataTransferListener != null) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDataTransferListener.onBegin(mediaItem);
                }
            });

        }
        PrintWriter writer = new PrintWriter(outputStream);
        writer.print(MediaStore.Audio.Media._ID + ":" + mediaItem.mId + END_SYMBOL);
        writer.print(MediaStore.Audio.Media.DATA + ":" + mediaItem.mData + END_SYMBOL);
        writer.print(MediaStore.Audio.Media.TITLE + ":" + mediaItem.mTitle + END_SYMBOL);
        writer.print(MediaStore.Audio.Media.MIME_TYPE + ":" + mediaItem.mMimeType + END_SYMBOL);
        writer.print(MediaStore.Audio.Media.ARTIST + ":" + mediaItem.mArtist + END_SYMBOL);
        writer.print(MediaStore.Audio.Media.ALBUM + ":" + mediaItem.mAlbum + END_SYMBOL);
        writer.print(MediaStore.Audio.Media.SIZE + ":" + mediaItem.mSize + END_SYMBOL);
        writer.print(MediaStore.Audio.Media.DURATION + ":" + mediaItem.mDuration + END_SYMBOL);
        writer.print(END_SYMBOL);
        writer.flush();
    }

    private void sendBody(OutputStream outputStream, final MediaItem mediaItem) {
        try {
            byte[] readBytes = new byte[Utils.SEND_BUFFER_SIZE];
            FileInputStream inputStream = new FileInputStream(mediaItem.mData);

            Log.i(LOG_TAG, mediaItem.mId + ":" + mediaItem.mTitle + ":" + mediaItem.mData);

            long transferSize = 0;
            int len;
            while ((len = inputStream.read(readBytes)) > 0) {
                outputStream.write(readBytes, 0, len);
                transferSize += len;

                if (mDataTransferListener != null) {
                    final long dataSize = transferSize;
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mDataTransferListener.onProgress(mediaItem, dataSize, mediaItem.mSize);
                        }
                    });
                }
            }
            outputStream.flush();
            outputStream.close();
            Log.i(LOG_TAG, "send finished");

            inputStream.close();

            if (mDataTransferListener != null) {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDataTransferListener.onEnd(mediaItem);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void sendEvent(final MediaItem mediaItem) {
        mWorkState = STATE_BUSY;
        final SocketClient client = new SocketClient(mIpString);
        client.send(new SocketClient.SendCallback() {
            @Override
            public void send(OutputStream outputStream) {
                long start = System.currentTimeMillis();
                Log.i(LOG_TAG, "send begin:" + start);
                sendHead(outputStream, mediaItem);
                sendBody(outputStream, mediaItem);

                client.close();
                Log.i(LOG_TAG, "send end:" + (System.currentTimeMillis() - start));
                mWorkState = STATE_IDLE;

                if (!mSendList.isEmpty()) {
                    mUiHandler.sendEmptyMessage(WHAT_SEND);
                }
            }
        });
    }
}
