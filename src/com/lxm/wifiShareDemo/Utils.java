package com.lxm.wifiShareDemo;

import java.io.File;

/**
 * 网络工具
 *
 * @author simon.L
 * @version 1.0.0
 */
public class Utils {

    public static final int DEFAULT_PORT = 49153;
    public static final int SO_TIME_OUT = 1000 * 60 * 60;
    public static final int DEFAULT_BACKLOG = 100;
    public static final int RECEIVE_BUFFER_SIZE = 1024 * 32;
    public static final int SEND_BUFFER_SIZE = 1024 * 32;

    public static final int MASK_0XFF = 0xFF;
    public static final int SHIFT_8 = 8;
    public static final int SHIFT_16 = 16;
    public static final int SHIFT_24 = 24;

    public static String intToIpString(int i) {
        return (i & MASK_0XFF)
                + "." + ((i >> SHIFT_8) & MASK_0XFF)
                + "." + ((i >> SHIFT_16) & MASK_0XFF)
                + "." + ((i >> SHIFT_24) & MASK_0XFF);
    }

    public static String genUniqueFilePath(String path) {
        String dir = getFilePathDir(path);
        String shortName = getFileShortName(path);
        String suffix = "." + getFileExtension(path);

        String frontPortion = dir.endsWith(File.separator) ? dir + shortName : dir + File.separator + shortName;

        String uniqueFilePath = frontPortion + suffix;
        File file = new File(uniqueFilePath);

        int number = 0;
        while (file.exists()) {
            uniqueFilePath = frontPortion + "_" + (++number) + suffix;
            file = new File(uniqueFilePath);
        }

        return uniqueFilePath;
    }

    public static String getFilePathDir(String path) {
        int separatorIndex = -1;

        if (path != null && path.startsWith(File.separator)) {
            separatorIndex = path.lastIndexOf(File.separatorChar);
        }

        return (separatorIndex == -1) ? File.separator : path.substring(0, separatorIndex);
    }

    public static String getFileName(String path) {
        if (path == null || path.length() == 0) {
            return "";
        }

        int query = path.lastIndexOf('?');
        if (query > 0) {
            path = path.substring(0, query);
        }

        int filenamePos = path.lastIndexOf(File.separatorChar);
        return (filenamePos >= 0) ? path.substring(filenamePos + 1) : path;
    }

    public static String getFileShortName(String path) {
        String fileName = getFileName(path);
        int separatorIndex = fileName.lastIndexOf('.');
        return (separatorIndex == -1) ? fileName : fileName.substring(0, separatorIndex);
    }

    public static String getFileExtension(String path) {
        String fileName = getFileName(path);

        if (fileName.length() > 0) {
            int dotPos = fileName.lastIndexOf('.');
            if (0 <= dotPos) {
                return fileName.substring(dotPos + 1);
            }
        }

        return "";
    }

    public static String genValidateFileName(String fileName, String replacement) {
        // {} \ / : * ? " < > |
        return fileName == null ? null : fileName.replaceAll("([{/\\\\:*?\"<>|}\\u0000-\\u001f\\uD7B0-\\uFFFF]+)", replacement);
    }

    public static int locationAfterCRLFCRLF(byte[] buf, int offset) {
        int pos = offset;
        boolean found = false;
        while (pos < (buf.length - 4)) {
            if (buf[pos] == '\r' && buf[++pos] == '\n' && buf[++pos] == '\r' && buf[++pos] == '\n') {
                found = true;
                break;
            }
            pos++;
        }
        pos++;

        return found ? pos : -1;
    }
}
