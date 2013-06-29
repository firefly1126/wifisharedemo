package com.lxm.wifiShareDemo;

/**
 * 数据传输的接口
 * @author simon.L
 * @version 1.0.0
 */
public interface DataTransferListener {
    void onBegin(MediaItem mediaItem);

    void onProgress(MediaItem mediaItem, long transferSize, long totalSize);

    void onEnd(MediaItem mediaItem);
}
