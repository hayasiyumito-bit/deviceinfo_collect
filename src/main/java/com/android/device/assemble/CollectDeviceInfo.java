package com.android.device.assemble;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.android.utils.Cmd;

import org.json.JSONObject;

/**
 * 与整机快照中的「deviceInfo」块对应：CPU/内存/磁盘等系统可读字段。
 */
public class CollectDeviceInfo {
    private static final String TAG = "CollectDeviceInfo";

    public static JSONObject getDeviceInfo() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cpuFreq", getCPUFreq());
            jsonObject.put("df", Cmd.exe("df"));
            jsonObject.put("memInfoList", getMemInfo());
            jsonObject.put("uptime", Cmd.exe("uptime"));
            jsonObject.put("version", Cmd.exe("cat /proc/version"));
            jsonObject.put("wlan0_address", Cmd.exe("cat /sys/class/net/wlan0/address"));
            return jsonObject;
        } catch (Exception e) {
            return null;
        }
    }

    static JSONObject getCPUFreq() {
        try {
            int processors = Runtime.getRuntime().availableProcessors();
            JSONObject jsonObject = new JSONObject();
            for (int i = 0; i < processors; i++) {
                JSONObject cpu = new JSONObject();
                cpu.put("cpuinfo_max_freq", Cmd.exe("cat /sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq"));
                cpu.put("cpuinfo_min_freq", Cmd.exe("cat /sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_min_freq"));
                cpu.put("time_in_state", Cmd.exe("cat /sys/devices/system/cpu/cpu" + i + "/cpufreq/stats/time_in_state"));
                jsonObject.put("cpu" + i, cpu);
            }
            return jsonObject;
        } catch (Exception e) {
            return null;
        }
    }

    static JSONObject getMemInfo() {
        try {
            int processors = Runtime.getRuntime().availableProcessors();
            JSONObject jsonObject = new JSONObject();
            for (int i = 0; i < processors; i++) {
                JSONObject memInfo = new JSONObject();
                memInfo.put("meminfo0", Cmd.exe("cat /proc/meminfo"));
                memInfo.put("meminfo1", Cmd.exe("cat /proc/meminfo"));
                memInfo.put("meminfo2", Cmd.exe("cat /proc/meminfo"));
                jsonObject.put("memInfo" + i, memInfo);
            }
            return jsonObject;
        } catch (Exception e) {
            return null;
        }
    }

    /** 屏幕宽、高（像素） */
    public static String getScreenWH(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            return outMetrics.widthPixels + "x" + outMetrics.heightPixels;
        } catch (Throwable e) {
            Log.w(TAG, "getScreenWH", e);
        }
        return "";
    }
}
