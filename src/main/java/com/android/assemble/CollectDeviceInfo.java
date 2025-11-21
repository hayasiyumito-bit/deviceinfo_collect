package com.android.assemble;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;


import com.android.UApplication;
import com.android.utils.Cmd;
import com.android.utils.ULog;

import org.json.JSONObject;

public class CollectDeviceInfo {
    private static final String TAG = "CollectDeviceInfo";
    private static final int REQ_ONE = 10001;
    private static final int REQ_TWO = REQ_ONE + 1;
    private static final int REQ_THR = REQ_TWO + 1;

    public static JSONObject getDeviceInfo() {
        try {
            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("idInfo", SepaFactory.INSTANCE.cd(REQ_TWO));
//            jsonObject.put("deviceInfo",  SepaFactory.INSTANCE.cd(REQ_THR));
            jsonObject.put("cpuFreq", getCPUFreq());
//            jsonObject.put("cpuinfo", Cmd.exe("cat /proc/cpuinfo"));
            jsonObject.put("df", Cmd.exe("df"));
//            jsonObject.put("ro_sf_lcd_density", Cmd.exe("getprop ro.sf.lcd_density"));
            //jsonObject.put("resolution", getScreenWH());
//            jsonObject.put("getprop", Cmd.exe("getprop"));
            jsonObject.put("memInfoList", getMemInfo());
            jsonObject.put("uptime", Cmd.exe("uptime"));
            jsonObject.put("version", Cmd.exe("cat /proc/version"));
            jsonObject.put("wlan0_address", Cmd.exe("cat /sys/class/net/wlan0/address"));
            return jsonObject;
        } catch (Exception e) {
//            e.printStackTrace();
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
//            e.printStackTrace();
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
//            Log.e(TAG, e.toString());
            return null;
        }
    }


    //屏幕宽、高、密度
    public static String getScreenWH(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            return outMetrics.widthPixels + "x" + outMetrics.heightPixels;
        } catch (Throwable e) {
//            e.printStackTrace();
        }
        return "";
    }
}
