package com.lxm.wifiShareDemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.*;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Wifi热点下音频媒体数据传输的Activity
 * @author simon.L
 * @version 1.0.0
 */
public class WifiShareActivity extends Activity implements WifiApManager.WifiStateListener {

    public static final String END_SYMBOL = "\r\n";
    private static final String LOG_TAG = "WifiShareActivity";

    public static final String ACTION_UPDATE_RECEIVER = "action_update_receiver";
    public static final String EXTRA_DATA = "extra_data";
    private Button mWifiEnableButton;
    private Button mWifiReceiveButton;
    private TextView mInfo;
    private ListView mListView;

    private WifiApManager mWifiApManager;

    private boolean mWifiApEnable;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_UPDATE_RECEIVER.equals(action)) {
                mInfo.setText(intent.getStringExtra(EXTRA_DATA));
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mWifiEnableButton = (Button)findViewById(R.id.wifi_enable);
        mWifiReceiveButton = (Button)findViewById(R.id.wifi_receive);
        mInfo = (TextView)findViewById(R.id.wifi_info);
        mListView = (ListView)findViewById(R.id.media_list_view);

        mWifiApManager = new WifiApManager(this, this);

        mWifiApEnable = mWifiApManager.isWifiApEnabled();
        mWifiEnableButton.setText(mWifiApEnable ? "关闭热点" : "创建热点以接收数据");
        mWifiEnableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWifiApEnable = !mWifiApEnable;
                mWifiEnableButton.setText(mWifiApEnable ? "关闭热点" : "创建热点以接收数据");
                if (mWifiApEnable) {
                    mInfo.setText(mWifiApManager.startWifiAp() ? "wifi热点创建中...." : "wifi热点创建失败");
                } else {
                    mWifiApManager.stopWifiAp();
                    mInfo.setText("WIFI热点已关闭，不能接收数据");
                }
            }
        });

        mWifiReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWifiApManager.startScan();
                mInfo.setText("正在扫描wifi热点");
            }
        });

        registerReceiver(mReceiver, new IntentFilter(ACTION_UPDATE_RECEIVER));
    }

    private List<MediaItem> queryExtrernalMedias() {
        Cursor cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaItem.COLUMNS, null, null, null);
        if (cursor != null && !cursor.isClosed() && !cursor.isAfterLast()) {
            List<MediaItem> medias = new ArrayList<MediaItem>();
            while (cursor.moveToNext()) {
                medias.add(new MediaItem(cursor));
            }

            return medias;
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);

        mWifiApManager.destroy(this);
    }

    @Override
    public void onScanFinished(List<ScanResult> scanResults) {
        Log.i(LOG_TAG, "onScanFinished");
    }

    @Override
    public void onSupplicantStateChanged(SupplicantState state, int supplicantError) {
        Log.i(LOG_TAG, "supplicationStatChanged:" + state);
        WifiInfo info = mWifiApManager.getConnectionInfo();
        if (info != null) {
            Log.i(LOG_TAG, info.getSSID() + ":network:" + info.getNetworkId() + "::" + info.getIpAddress());
        }
    }

    @Override
    public void onSupplicantConnectionChanged(boolean connected) {
    }

    @Override
    public void onWifiStateChanged(int wifiState, int prevWifiState) {
        Log.i(LOG_TAG, "wifiStateChanged:" + wifiState);
        WifiInfo info = mWifiApManager.getConnectionInfo();
        if (info != null) {
            Log.i(LOG_TAG, info.getSSID() + ":network:" + info.getNetworkId() + "::" + info.getIpAddress());
        }
    }

    private MediaListAdapter mReceiverMediaListAdapter;
    private DataReceiver mDataReceiver;
    private static final String STORE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separatorChar + "download";
    @Override
    public void onWifiApStateChanged(int wifiApState) {
        WifiConfiguration configuration = mWifiApManager.getWifiApConfiguration();
        if (wifiApState == WifiApManager.WIFI_AP_STATE_ENABLED) {
            Log.i(LOG_TAG, "WIFI_AP_STATE_ENABLED");
            if (configuration != null) {
                mInfo.setText("已建立WIFI热点：" + configuration.SSID);

                mWifiEnableButton.setEnabled(false);
                mWifiReceiveButton.setEnabled(false);

                if (mReceiverMediaListAdapter == null) {
                    mReceiverMediaListAdapter = new MediaListAdapter(WifiShareActivity.this, null, false);
                    if (mDataReceiver == null) {
                        mDataReceiver = new DataReceiver(STORE_DIR, mReceiverMediaListAdapter);
                    }

                    mListView.setAdapter(mReceiverMediaListAdapter);
                }
            }
        }
    }

    @Override
    public void onNetworkIdsChanged() {
    }

    @Override
    public void onRSSIChanged(int rssi) {
    }

    @Override
    public void onPickWifiNetwork() {
    }

    @Override
    public void onConnectionPreparing(String ssid) {
        mInfo.setText("热点" + ssid + "准备中....");
    }

    @Override
    public void onConnectionPrepared(boolean success, String ssid) {
        mInfo.setText(success ? "正在连接热点" + ssid : "热点" + ssid + "不能连接");
    }

    private MediaListAdapter mSenderMediaListAdapter;
    private DataSender mDataSender;
    @Override
    public void onConnectNetworkSucceeded(NetworkInfo networkInfo, final WifiInfo wifiInfo) {
        if (!wifiInfo.getSSID().contains(WifiApManager.SSID_PREFIX)) {
            return;
        }

        mWifiEnableButton.setEnabled(false);
        mWifiReceiveButton.setEnabled(false);

        mInfo.setText("热点" + wifiInfo.getSSID() + "连接成功");

        if (mSenderMediaListAdapter == null) {
            mSenderMediaListAdapter = new MediaListAdapter(this, queryExtrernalMedias(), true);
            mSenderMediaListAdapter.setSendListener(new MediaListAdapter.OnSendClickListener() {
                @Override
                public void onSendEvent(final MediaListAdapter.TransmittableMediaItem item) {
                    DhcpInfo dhcpInfo = mWifiApManager.getWifiManager().getDhcpInfo();

                    if (mDataSender == null) {
                        mDataSender = new DataSender(Utils.intToIpString(dhcpInfo.gateway), mSenderMediaListAdapter);
                    }

                    mDataSender.send(item.mMediaItem);
                }
            });
            mListView.setAdapter(mSenderMediaListAdapter);
        }

    }

    @Override
    public void onConnectNetworkFailed(NetworkInfo networkInfo) {
    }
}
