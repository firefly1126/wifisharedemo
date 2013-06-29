package com.lxm.wifiShareDemo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * socket服务端实现
 * @author simon.L
 * @version 1.0.0
 */
public class SocketServer {

    private static final String LOG_TAG = "SocketServer";

    private ServerSocket mServerSocket;
    private SocketListener mSocketListener;
    private ServerDaemon mServerDaemon;

    private boolean mIsRunning;

    public SocketServer(SocketListener listener) {
        mIsRunning = true;
        mSocketListener = listener;

        try {
            mServerSocket = new ServerSocket(Utils.DEFAULT_PORT, Utils.DEFAULT_BACKLOG);
            mServerSocket.setSoTimeout(Utils.SO_TIME_OUT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mServerDaemon = new ServerDaemon();
        mServerDaemon.setDaemon(true);
        mServerDaemon.setPriority(Thread.MAX_PRIORITY);
        mServerDaemon.start();
    }

    public void exit() {
        mIsRunning = false;

        try {
            mServerDaemon.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        close();
    }

    private void close() {
        try {
            if (!mServerSocket.isClosed()) {
                mServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ServerDaemon extends Thread {
        @Override
        public void run() {
            while (mIsRunning) {
                try {
                    Socket connection = null;
                    if (mServerSocket != null) {
                        connection = mServerSocket.accept();
                    }

                    if (connection != null) {
                        if (mSocketListener != null) {
                            mSocketListener.onReceive(connection.getInputStream());

                            mSocketListener.onSend(connection.getOutputStream());
                        }

                        if (!connection.isClosed()) {
                            connection.close();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static interface SocketListener {

        public void onReceive(InputStream inputStream);

        public void onSend(OutputStream outputStream);
    }
}
