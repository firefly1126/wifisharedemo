package com.lxm.wifiShareDemo;

/**
 * 数据传输的接口
 * @author simon.L
 * @version 1.0.0
 */
public interface DataTransferListener {
    /**
     * 开始传输的回调
     * @param mediaItem MediaITem
     */
    void onBegin(MediaItem mediaItem);

    /**
     * 正在传输的回调
     * @param mediaItem MediaItem
     * @param transferSize 传输的大小
     * @param totalSize 传输文件总大小
     */
    void onProgress(MediaItem mediaItem, long transferSize, long totalSize);

    /**
     * 传输结束的回调
     * @param mediaItem MediaItem
     */
    void onEnd(MediaItem mediaItem);
}
