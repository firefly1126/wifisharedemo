package com.lxm.wifiShareDemo;

import java.io.*;
import java.net.Socket;

/**
 * socket 客户端实现
 * @author simon.L
 * @version 1.0.0
 */
public class SocketClient {

    private static final String LOG_TAG = "SocketClient";
    private Socket mClient;

    public SocketClient(String ipString) {
        try {
            mClient = new Socket(ipString, Utils.DEFAULT_PORT);
            mClient.setSoTimeout(Utils.SO_TIME_OUT);
            mClient.setSendBufferSize(Utils.SEND_BUFFER_SIZE);
            mClient.setReceiveBufferSize(Utils.RECEIVE_BUFFER_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            mClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(final SendCallback callback) {
        if (mClient.isConnected()) {
            new Thread() {
                @Override
                public void run() {
                    if (callback != null) {
                        try {
                            callback.send(mClient.getOutputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            .start();
        }
    }

    public void receive(final ReceiveCallback callback) {
        if (mClient.isConnected()) {
            new Thread() {
                @Override
                public void run() {
                    if (callback != null) {
                        try {
                            callback.receive(mClient.getInputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            .start();
        }
    }

    public static interface SendCallback {
        public void send(OutputStream outputStream);
    }

    public static interface ReceiveCallback {
        public void receive(InputStream inputStream);
    }
}
