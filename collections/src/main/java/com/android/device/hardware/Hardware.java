package com.android.device.hardware;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Build;
import android.util.Log;

import com.android.utils.Cmd;
import com.android.utils.ULog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Hardware {

    /**
     * 获取CPU名称
     *
     * @return 返回CPU名称，如果无法获取则返回空字符串
     */
    public static String getCpuName() {
        String valueStr;
        FileReader fr;
        String cpuName = null;
        BufferedReader bufferedReader = null;

        try {
            fr = new FileReader("/proc/cpuinfo");
            bufferedReader = new BufferedReader(fr);
            while ((valueStr = bufferedReader.readLine()) != null) {
                if (valueStr.contains("Hardware")) {
                    cpuName = valueStr.split(":")[1];
                    break;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ignored) {

                }
            }
        }
        return cpuName != null ? cpuName : "";
    }

    /**
     * 获取CPU信息
     *
     * @return CPU信息字符串，如果发生异常则返回空字符串
     */
    public static String getCpuInfo() { //IO操作
        try {
            StringBuilder cpuInfo = new StringBuilder();
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader reader = new BufferedReader(fr);
            String str;
            while ((str = reader.readLine()) != null) {
                cpuInfo.append(str);
                cpuInfo.append("\n");
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
//                    ULog.e(e);
                }
            }
            if (fr != null) {
                try {
                    fr.close();
                } catch (Exception e) {
//                    ULog.e(e);
                }
            }
            return cpuInfo.toString();
        } catch (Exception e) {
//            ULog.e(e);
        }
        return "";
    }

    /**
     * 通过执行shell命令获取CPU信息并返回。
     *
     * @return CPU信息字符串，如果执行命令出错则返回null。
     */
    public static String getCpuInfoByString() {
        try {
            return Cmd.exe("cat /proc/cpuinfo");
        } catch (Exception e) {
//            Log.e("hardware_cpuinfo", e.getMessage());
        }
        return null;
    }

    /**
     * 获取系统特性信息
     *
     * @param context 应用上下文
     * @return 系统特性信息字符串
     */
    public static String getFeatures(Context context) {
        try {
            StringBuffer buffer = new StringBuffer();
            if (Build.VERSION.SDK_INT >= 24) {
                PackageManager pm = context.getPackageManager();
                List<FeatureInfo> list = Arrays.asList(pm.getSystemAvailableFeatures());
                // sort by name
                Collections.sort(list, (o1, o2) -> {
                    if (o1.name == o2.name) return 0;
                    if (o1.name == null) return -1;
                    if (o2.name == null) return 1;
                    return o1.name.compareTo(o2.name);
                });

                int count = list.size();
                for (int p = 0; p < count; p++) {
                    FeatureInfo fi = list.get(p);
                    if (fi.name != null) {
                        buffer.append(fi.name).append("#").append(fi.version);
                    } else {
                        buffer.append("reqGlEsVersion=0x").append(fi.reqGlEsVersion);
                    }
                    buffer.append("#").append(fi.flags).append("\n");
                }
            }
            return buffer.toString();
        } catch (Throwable e) {
//            ULog.e(e);
        }
        return "";
    }

    /**
     * 判断设备是否支持NFC功能
     *
     * @param context 上下文对象
     * @return 如果设备支持NFC功能，则返回true；否则返回false
     */
    public static boolean isSupportNFC(Context context) {
        NfcManager nfcManager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = nfcManager.getDefaultAdapter();
        return adapter != null;
    }

    /**
     * 获取设备硬件信息
     *
     * @param context 上下文对象
     * @return 包含硬件信息的 JSONObject 对象
     */
    public static JSONObject getHardwareInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
//            jsonObject.put("cpuName", getCpuName());
            jsonObject.put("cpuInfo", getCpuInfo());
//            jsonObject.put("cpuInfoCmd", getCpuInfoByString());
            jsonObject.put("avi_features", getFeatures(context));
//            jsonObject.put("supportNFC", isSupportNFC(context));
            jsonObject.put("camera", Camera.getCameraInfo(context));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

}
