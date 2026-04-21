package com.android.device.hardware;


import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.android.utils.ULog;

import org.json.JSONObject;

import java.io.File;

public final class Storage {

    /**
     * 获取SD卡容量信息
     *
     * @return SD卡容量信息，格式为"blockSize*blockCount"，若获取失败则返回空字符串
     */
    private static String getSDCardSize() {
        try {
            StringBuffer buffer = new StringBuffer();
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File sdcardDir = Environment.getExternalStorageDirectory();
                StatFs sf = new StatFs(sdcardDir.getPath());
                long blockSize = sf.getBlockSizeLong();
                long blockCount = sf.getBlockCountLong();
                buffer.append(blockSize).append("*");
                buffer.append(blockCount);
            }
            return buffer.toString();
        } catch (Throwable e) {
//            ULog.e(e);
        }
        return "";
    }

    /**
     * 获取系统存储大小
     *
     * @return 系统存储大小，格式为"blockSize*blockCount"，如果获取失败则返回空字符串
     */
    private static String getSystemSize() {
        try {
            StringBuffer buffer = new StringBuffer();
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File root = Environment.getRootDirectory();

                StatFs sf = new StatFs(root.getPath());
                long blockSize = sf.getBlockSizeLong();
                long blockCount = sf.getBlockCountLong();

                buffer.append(blockSize).append("*");
                buffer.append(blockCount);
            }
            return buffer.toString();
        } catch (Throwable ignored) {
        }
        return "";
    }


    /**
     * 获取设备数据大小
     *
     * @return 设备数据大小，格式为"块大小*块数量"，若获取失败则返回空字符串
     */
    private static String getDataSize() {
        try {
            StringBuffer buffer = new StringBuffer();

            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File data = Environment.getDataDirectory();
                StatFs sf = new StatFs(data.getPath());
                long blockSize = sf.getBlockSizeLong();
                long blockCount = sf.getBlockCountLong();

                buffer.append(blockSize).append("*");
                buffer.append(blockCount);
            }
            return buffer.toString();
        } catch (Throwable ignored) {
        }
        return "";
    }

    /**
     * 获取可用内存和总内存
     *
     * @param context 上下文对象
     * @return 可用内存和总内存字符串，格式为"可用内存*总内存"。如果获取失败，则返回空字符串。
     * @throws Throwable 如果在获取内存信息过程中发生异常，将捕获该异常并返回空字符串
     */
    public static String getAvailMemory(Context context) {
        try {
            StringBuffer buffer = new StringBuffer();
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                buffer.append(mi.availMem).append("*");
                buffer.append(mi.totalMem);
            }
            return buffer.toString();
        } catch (Throwable ignored) {
        }
        return "";
    }

    /**
     * 获取内存阈值
     *
     * @param context 上下文对象
     * @return 内存阈值，如果获取失败则返回空字符串
     */
    public static String getMemoryThreshold(Context context) {
        try {
            StringBuffer buffer = new StringBuffer();
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                return String.valueOf(mi.threshold);
            }
            return buffer.toString();
        } catch (Throwable ignored) {
        }
        return "";
    }


    /**
     * 获取设备磁盘的总大小和可用大小（以块为单位）。
     *
     * @return 返回磁盘的总块数和可用块数，格式为“可用块数/总块数”。
     */
    public static String getDiskSize2AF() {
        long availableBlocks = 0;
        long blockCount = 0;
        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        if (Build.VERSION.SDK_INT >= 18) {
            long blockSizeLong = statFs.getBlockSizeLong();
            availableBlocks = statFs.getAvailableBlocksLong() * blockSizeLong;
            blockCount = statFs.getBlockCountLong() * blockSizeLong;
        } else {
            int blockSize = statFs.getBlockSize();
            availableBlocks = statFs.getAvailableBlocks() * blockSize;
            blockCount = statFs.getBlockCount() * blockSize;
        }
        double pow = Math.pow(2.0d, 20.0d);
        //return (long) (availableBlocks / pow) + "/" + (long) (blockCount / pow);
        return statFs.getAvailableBlocks() + "/" + statFs.getBlockCount();
    }

    /**
     * 获取存储大小信息
     *
     * @param context 上下文对象
     * @return 包含存储大小信息的 JSONObject 对象
     */
    public static JSONObject getStorageSizeInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File storage = Environment.getExternalStorageDirectory();

                StatFs sf = new StatFs(storage.getPath());
                int blockCount = sf.getBlockCount();
                long freeBytes = sf.getFreeBytes();
                long availableBytes = sf.getAvailableBytes();
                long totalBytes = sf.getTotalBytes();

                jsonObject.put("blockCount", blockCount);
                jsonObject.put("freeBytes", freeBytes);
                jsonObject.put("availableBytes", availableBytes);
                jsonObject.put("totalBytes", totalBytes);
            }
        } catch (Exception e) {
//            ULog.e(e);
        }
        return jsonObject;
    }

    /**
     * 获取根目录的大小信息
     *
     * @param context 上下文对象
     * @return 包含根目录大小信息的 JSONObject 对象
     */
    public static JSONObject getRootSizeInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File storage = Environment.getRootDirectory();

                StatFs sf = new StatFs(storage.getPath());
                int blockCount = sf.getBlockCount();
                long freeBytes = sf.getFreeBytes();
                long availableBytes = sf.getAvailableBytes();
                long totalBytes = sf.getTotalBytes();

                jsonObject.put("blockCount", blockCount);
                jsonObject.put("freeBytes", freeBytes);
                jsonObject.put("availableBytes", availableBytes);
                jsonObject.put("totalBytes", totalBytes);
            }
        } catch (Exception e) {
//            ULog.e(e);
        }
        return jsonObject;
    }

    /**
     * 获取设备存储空间信息
     *
     * @param context 上下文对象
     * @return 包含存储空间信息的 JSONObject 对象
     */
    public static JSONObject getDataSizeInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File storage = Environment.getDataDirectory();

                StatFs sf = new StatFs(storage.getAbsolutePath());
                long blockCount = sf.getBlockCountLong();
                long freeBytes = sf.getFreeBytes();
                long availableBytes = sf.getAvailableBytes();
                long totalBytes = sf.getTotalBytes();

                jsonObject.put("blockCount", blockCount);
                jsonObject.put("freeBytes", freeBytes);
                jsonObject.put("availableBytes", availableBytes);
                jsonObject.put("totalBytes", totalBytes);
            }
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return jsonObject;
    }

    /**
     * 获取设备的存储信息
     *
     * @param context 上下文对象
     * @return 包含存储信息的 JSONObject 对象
     */
    public static JSONObject getStorageInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("storageInfo", getStorageSizeInfo(context));
            jsonObject.put("rootInfo", getRootSizeInfo(context));
            jsonObject.put("dataInfo", getDataSizeInfo(context));
        } catch (Throwable e) {
            // 删除ULog，静默处理异常
        }
        return jsonObject;
    }
}
