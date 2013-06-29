package com.lxm.wifiShareDemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.*;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * wifi 热点管理,wifi连接管理
 * @author simon.L
 * @version 1.0.0
 */
public class WifiApManager {

    private static final String LOG_TAG = "WifiApManager";
    /**无密码类型*/
    public static final int TYPE_NO_PASSWD = 0x11;
    /**wep类型*/
    public static final int TYPE_WEP = 0x12;
    /**wpa类型*/
    public static final int TYPE_WPA = 0x13;
    /**默认的网络类型*/
    public static final int DEFAULT_TYPE = TYPE_WPA;
    /**网络id前缀*/
    public static final String SSID_PREFIX = "_DOPTT_";
    /**默认的网络id*/
    public static final String DEFAULT_SSID = SSID_PREFIX + Build.MODEL;
    /**默认的密码*/
    public static final String DEFAULT_PASSWORD = "ttpod123";

    private static final int DEFAULT_PRIORITY = 10000;

    private static int mWifiApStateDisabled;
    private static int mWifiApStateDisabling;
    private static int mWifiApStateEnabled;
    private static int mWifiApStateEnabling;
    private static int mWifiApStateFailed;

    private static String mWifiApStateChangedAction;

    private static String mExtraWifiApState;
    private static String mExtraPreviousWifiApState;

    static {
        try {
            mWifiApStateDisabled = WifiManager.class.getField("WIFI_AP_STATE_DISABLED").getInt(WifiManager.class);
            mWifiApStateDisabling = WifiManager.class.getField("WIFI_AP_STATE_DISABLING").getInt(WifiManager.class);
            mWifiApStateEnabled = WifiManager.class.getField("WIFI_AP_STATE_ENABLED").getInt(WifiManager.class);
            mWifiApStateEnabling = WifiManager.class.getField("WIFI_AP_STATE_ENABLING").getInt(WifiManager.class);
            mWifiApStateFailed = WifiManager.class.getField("WIFI_AP_STATE_FAILED").getInt(WifiManager.class);

            mWifiApStateChangedAction = (String)WifiManager.class.getField("WIFI_AP_STATE_CHANGED_ACTION").get(WifiManager.class);

            mExtraWifiApState = (String)WifiManager.class.getField("EXTRA_WIFI_AP_STATE").get(WifiManager.class);
            mExtraPreviousWifiApState = (String)WifiManager.class.getField("EXTRA_PREVIOUS_WIFI_AP_STATE").get(WifiManager.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**wifi热点状态 已经失效*/
    public static final int WIFI_AP_STATE_DISABLED = mWifiApStateDisabled;
    /**wifi热点状态 正在失效*/
    public static final int WIFI_AP_STATE_DISABLING = mWifiApStateDisabling;
    /**wifi热点状态 有效状态*/
    public static final int WIFI_AP_STATE_ENABLED = mWifiApStateEnabled;
    /**wifi热点状态 正在生效*/
    public static final int WIFI_AP_STATE_ENABLING = mWifiApStateEnabling;
    /**wifi热点状态 失败状态*/
    public static final int WIFI_AP_STATE_FAILED = mWifiApStateFailed;
    /**wifi热点状态改变的消息*/
    public static final String WIFI_AP_STATE_CHANGED_ACTION = mWifiApStateChangedAction;
    /**wifi热点状态信息*/
    public static final String EXTRA_WIFI_AP_STATE = mExtraWifiApState;
    /**之前的wifi热点状态信息*/
    public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = mExtraPreviousWifiApState;

    private WifiManager mWifiManager;
    private WifiStateListener mWifiStateListener;
    private boolean mIsNetworkEnabled = false;
    private WifiManager.WifiLock mWifiLock;

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(LOG_TAG, action);
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                List<ScanResult> results = mWifiManager.getScanResults();
                scanFinishedEvent(results);

                if (mWifiStateListener != null) {
                    mWifiStateListener.onScanFinished(results);
                }
            } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
                if (mWifiStateListener != null) {
                    mWifiStateListener.onRSSIChanged(intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0));
                }
            } else if (WifiManager.NETWORK_IDS_CHANGED_ACTION.equals(action)) {
                if (mWifiStateListener != null) {
                    mWifiStateListener.onNetworkIdsChanged();
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                WifiInfo wifiInfo = (WifiInfo)intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                if (mWifiStateListener != null) {
                    if (networkInfo.isAvailable() && networkInfo.isConnected() && wifiInfo != null) {
                        mWifiStateListener.onConnectNetworkSucceeded(networkInfo, wifiInfo);
                    } else {
                        mWifiStateListener.onConnectNetworkFailed(networkInfo);
                    }
                }
            } else if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action)) {
                if (mWifiStateListener != null) {
                    mWifiStateListener.onSupplicantConnectionChanged(intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
                }
            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
                if (mWifiStateListener != null) {
                    mWifiStateListener.onSupplicantStateChanged((SupplicantState)intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)
                        , intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, WifiManager.ERROR_AUTHENTICATING));
                }
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                if (mWifiStateListener != null) {
                    mWifiStateListener.onWifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                        , intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
                }
            } else if (WifiManager.ACTION_PICK_WIFI_NETWORK.equals(action)) {
                if (mWifiStateListener != null) {
                    mWifiStateListener.onPickWifiNetwork();
                }
            } else if (WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                if (mWifiStateListener != null) {
                    mWifiStateListener.onWifiApStateChanged(intent.getIntExtra(EXTRA_WIFI_AP_STATE, WIFI_AP_STATE_FAILED));
                }
            }

        }
    };

    /**
     * 构造函数
     * @param context 上下文
     * @param listener WifiStateListener
     */
    public WifiApManager(Context context, WifiStateListener listener) {
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        mWifiStateListener = listener;

        mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, SSID_PREFIX);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);
        if (!TextUtils.isEmpty(WIFI_AP_STATE_CHANGED_ACTION)) {
            intentFilter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
        }

        context.registerReceiver(mWifiReceiver, intentFilter);
    }

    /**
     * 设置wifi热点是否有效
     * @param configuration WifiConfiguration
     * @param enabled 是否有效
     * @return 设置是否成功
     */
    public boolean setWifiApEnabled(WifiConfiguration configuration, boolean enabled) {
        try {
            if (enabled) {
                mWifiManager.setWifiEnabled(false);
            }

            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (Boolean)method.invoke(mWifiManager, configuration, enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 获取Wifi热点的状态
     * @return Wifi热点状态
     */
    public int getWifiApState() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApState");
            return (Integer)method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mWifiApStateFailed;
    }

    /**
     * 获取Wifi热点配置
     * @return WifiConfiguration
     */
    public WifiConfiguration getWifiApConfiguration() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration config = (WifiConfiguration)method.invoke(mWifiManager);
            loadWifiConfigurationFromProfile(config);
            return config;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 设置Wifi热点配置
     * @param configuration WifiConfiguration
     * @return 设置是否成功
     */
    public boolean setWifiApConfiguration(WifiConfiguration configuration) {
        try {
            Method method = mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            return (Boolean)method.invoke(mWifiManager, configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * wifi热点是否有效
     * @return wifi热点是否有效
     */
    public boolean isWifiApEnabled() {
        try {
            Method method = mWifiManager.getClass().getMethod("isWifiApEnabled");
            return (Boolean)method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 启动wifi连接
     * @return 启动wifi连接是否成功
     */
    public boolean startWifi() {
        try {
            Method method = mWifiManager.getClass().getMethod("startWifi");
            return (Boolean)method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 停止wifi连接
     * @return 停止wifi连接是否成功
     */
    public boolean stopWifi() {
        try {
            Method method = mWifiManager.getClass().getMethod("stopWifi");
            return (Boolean)method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 创建wifi热点
     * @return 打开wifi热点是否成功
     */
    public boolean startWifiAp() {
        return openWifiAp();
    }

    /**
     * 停止wifi热点，并恢复之前的网络状态
     */
    public void stopWifiAp() {
        //关闭wifi热点
        closeWifiAp();
        //恢复wifi
        openWifi();
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    /**
     * 停止wifi热点，解绑定监听器
     * @param context 上下文
     */
    public void destroy(Context context) {
        stopWifiAp();
        removeNetwork(SSID_PREFIX);
        context.unregisterReceiver(mWifiReceiver);
    }

    private void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    private void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    private void closeWifiAp() {
        if (isWifiApEnabled()) {
            setWifiApEnabled(getWifiApConfiguration(), false);
        }
    }

    private boolean openWifiAp() {
        //先关闭wifi
        closeWifi();
        //关闭已经打开的热点
        closeWifiAp();
        //激活需要创建的热点

        mWifiLock.acquire();

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = DEFAULT_SSID;
        wifiConfig.preSharedKey = DEFAULT_PASSWORD;
        setWifiConfigAsWPA(wifiConfig);
        return setWifiApEnabled(wifiConfig, true);
    }

    private void setWifiConfigAsWPA(WifiConfiguration wifiConfig) {
        wifiConfig.hiddenSSID = false;
        wifiConfig.status = WifiConfiguration.Status.ENABLED;
        wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

        setWifiConfigurationProfile(wifiConfig);
    }

    private boolean scanFinishedEvent(List<ScanResult> scanResults) {
        List<ScanResult> filter = fileterAccessPoint(scanResults);

        return filter != null
                && !filter.isEmpty()
                && connectToHotspot(filter.get(0));
    }

    private List<ScanResult> fileterAccessPoint(List<ScanResult> results) {
        if (results != null) {
            List<ScanResult> filteredResult = new ArrayList<ScanResult>();
            for (ScanResult result : results) {
                if (result.SSID.contains(SSID_PREFIX)) {
                    filteredResult.add(result);
                }
            }

            return filteredResult;
        }

        return null;
    }

    private boolean connectToHotspot(ScanResult result) {
        if (mIsNetworkEnabled) {
            return true;
        }

        if (mWifiStateListener != null) {
            mWifiStateListener.onConnectionPreparing(result.SSID);
        }
        removeNetwork(SSID_PREFIX);
        //如果当前网络不是需要连接的网络，则断开
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        if (!result.SSID.equals(connectionInfo.getSSID())) {
            mWifiManager.disableNetwork(connectionInfo.getNetworkId());
            mWifiManager.disconnect();
        }

        //将所有其他网络的的优先级降低
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (!config.SSID.equals("\"" + result.SSID + "\"")) {
                config.priority = 0;
                mWifiManager.updateNetwork(config);
            }
        }

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + result.SSID + "\"";
        wifiConfig.preSharedKey = "\"" + DEFAULT_PASSWORD + "\"";
        wifiConfig.BSSID = result.BSSID;
        wifiConfig.priority = DEFAULT_PRIORITY;

        setWifiConfigAsWPA(wifiConfig);

        try {
            Field field = wifiConfig.getClass().getField("ipAssignment");
            field.set(wifiConfig, Enum.valueOf((Class<Enum>)field.getType(), "DHCP"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        int networkId = mWifiManager.addNetwork(wifiConfig);

        mIsNetworkEnabled = mWifiManager.enableNetwork(networkId, true);

        if (mWifiStateListener != null) {
            mWifiStateListener.onConnectionPrepared(mIsNetworkEnabled, result.SSID);
        }

        return mIsNetworkEnabled;
    }

    /**
     * 开始扫描wifi热点
     */
    public void startScan() {
        //关闭wifi热点
        closeWifiAp();
        //打开wifi
        openWifi();
        mWifiLock.acquire();

        mWifiManager.startScan();
        mIsNetworkEnabled = false;
    }

    private void setWifiConfigurationProfile(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration != null) {
            try {
                Field wifiApProfileField = WifiConfiguration.class.getDeclaredField("mWifiApProfile");
                wifiApProfileField.setAccessible(true);
                Object wifiApProfile = wifiApProfileField.get(wifiConfiguration);
                wifiApProfileField.setAccessible(false);

                if (wifiApProfile != null) {
                    Field ssidField = wifiApProfile.getClass().getDeclaredField("SSID");
                    ssidField.setAccessible(true);
                    ssidField.set(wifiApProfile, wifiConfiguration.SSID);
                    ssidField.setAccessible(false);

                    Field bssidField = wifiApProfile.getClass().getDeclaredField("BSSID");
                    bssidField.setAccessible(true);
                    bssidField.set(wifiApProfile, wifiConfiguration.BSSID);
                    bssidField.setAccessible(false);

                    Field dhcpField = wifiApProfile.getClass().getDeclaredField("dhcpEnable");
                    dhcpField.setAccessible(true);
                    dhcpField.set(wifiApProfile, 1);
                    dhcpField.setAccessible(false);

                    Field keyField = wifiApProfile.getClass().getDeclaredField("key");
                    keyField.setAccessible(true);
                    keyField.set(wifiApProfile, wifiConfiguration.preSharedKey);
                    keyField.setAccessible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadWifiConfigurationFromProfile(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration != null) {
            if (TextUtils.isEmpty(wifiConfiguration.SSID) || TextUtils.isEmpty(wifiConfiguration.BSSID)) {
                try {
                    Field wifiApProfileField = WifiConfiguration.class.getDeclaredField("mWifiApProfile");
                    wifiApProfileField.setAccessible(true);
                    Object wifiApProfile = wifiApProfileField.get(wifiConfiguration);
                    wifiApProfileField.setAccessible(false);

                    if (wifiApProfile != null) {
                        Field ssidField = wifiApProfile.getClass().getDeclaredField("SSID");
                        ssidField.setAccessible(true);
                        Object value2 = ssidField.get(wifiApProfile);
                        if (value2 != null) {
                            wifiConfiguration.SSID = (String)value2;
                        }
                        ssidField.setAccessible(false);

                        Field bssidField = wifiApProfile.getClass().getDeclaredField("BSSID");
                        bssidField.setAccessible(true);
                        Object value3 = bssidField.get(wifiApProfile);
                        if (value3 != null) {
                            wifiConfiguration.BSSID = (String)value3;
                        }
                        bssidField.setAccessible(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取WifiManager
     * @return WifiManager
     */
    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    /**
     * 获取指定网络id的wifi配置
     * @param ssid 网络id
     * @return WifiConfiguration
     */
    public WifiConfiguration getWifiConfiguration(String ssid) {
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            loadWifiConfigurationFromProfile(config);
            if (config.SSID.equals("\"" + ssid + "\"")) {
                return config;
            }
        }

        return null;
    }

    /**
     * 清除已连接的网络的配置信息
     */
    public void removeConnection() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        mWifiManager.removeNetwork(wifiInfo.getNetworkId());
        mWifiManager.saveConfiguration();
    }

    /**
     * 获取连接信息
     * @return WifiInfo
     */
    public WifiInfo getConnectionInfo() {
        return mWifiManager.getConnectionInfo();
    }

    /**
     * 清除指定网络的配置信息
     * @param ssid 网络id
     */
    public void removeNetwork(String ssid) {
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                loadWifiConfigurationFromProfile(config);
                if (config.SSID.contains(ssid)) {
                    mWifiManager.disableNetwork(config.networkId);
                    mWifiManager.removeNetwork(config.networkId);
                }
            }
        }
        mWifiManager.saveConfiguration();
    }

    /**
     * 设置移动网络是否允许打开
     * @param context 上下文
     * @param enabled 是否允许打开移动网络
     */
    public void setMobileDataEnabled(Context context, boolean enabled) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method method = connectivityManager.getClass().getMethod("setMobileDataEnabled", boolean.class);
            method.invoke(connectivityManager, enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取当前移动网络是否打开
     * @param context 上下文
     * @return 移动网络是否打开
     */
    public boolean getMobileDataEnabled(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method method = connectivityManager.getClass().getMethod("getMobileDataEnabled");

            return (Boolean)method.invoke(connectivityManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * wifi网络状态监听器
     */
    public static interface WifiStateListener {
        /**
         * 扫描结束的回调
         * @param scanResults List
         */
        public void onScanFinished(List<ScanResult> scanResults);
        /**
         * 网络请求状态的变更回调
         * @param state SupplicantState
         * @param supplicantError error
         */
        public void onSupplicantStateChanged(SupplicantState state, int supplicantError);
        /**
         * 网络请求连接变更的回调
         * @param connected 是否连接
         */
        public void onSupplicantConnectionChanged(boolean connected);
        /**
         * wifi状态变更的回调
         * @param wifiState wifi状态
         * @param prevWifiState 前一个wifi状态
         * @see android.net.wifi.WifiManager#EXTRA_WIFI_STATE
         */
        public void onWifiStateChanged(int wifiState, int prevWifiState);
        /**
         * wifi热点状态变更的回调
         * @param wifiApState see {@link #WIFI_AP_STATE_DISABLED}, {@link #WIFI_AP_STATE_DISABLING}, {@link #WIFI_AP_STATE_ENABLED}
         * {@link #WIFI_AP_STATE_ENABLING}, {@link #WIFI_AP_STATE_FAILED}
         */
        public void onWifiApStateChanged(int wifiApState);
        /**
         * 网络id变更的回调
         */
        public void onNetworkIdsChanged();

        /**
         * 信号强度变更的回调
         * @param rssi 信号强度
         */
        public void onRSSIChanged(int rssi);

        /**
         * 选择一个wifi网络去连接的回调
         */
        public void onPickWifiNetwork();

        /**
         * 连接正在准备
         * @param ssid SSID
         */
        public void onConnectionPreparing(String ssid);
        /**
         * 连接准备完成的回调
         * @param success 连接准备是否成功
         * @param ssid 将要连接的网络id
         */
        public void onConnectionPrepared(boolean success, String ssid);

        /**
         * 网络连接成功
         * @param networkInfo NetworkInfo
         * @param wifiInfo WifiInfo
         */
        public void onConnectNetworkSucceeded(NetworkInfo networkInfo, WifiInfo wifiInfo);

        /**
         * 网络连接失败
         * @param networkInfo NetworkInfo
         */
        public void onConnectNetworkFailed(NetworkInfo networkInfo);
    }
}
