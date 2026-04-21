package com.android.device.hardware;

import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;

import com.android.UApplication;
import com.android.utils.ULog;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class USB {

    /**
     * 获取USB连接状态
     *
     * @param context 上下文对象
     * @return 如果USB设备已连接，则返回true；否则返回false
     */
    public static boolean getUsbStatus(Context context) {
        try {
            Intent intent = context.registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
            return intent != null && intent.getBooleanExtra("connected", false);
        } catch (Throwable e) {
        }
        return false;
    }

    /**
     * 判断ADB调试是否开启
     *
     * @param context 上下文对象
     * @return 如果ADB调试已开启，则返回true；否则返回false
     */
    public static boolean adbEnable(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ADB_ENABLED, 0) > 0;
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return false;
    }

    /**
     * 获取USB设备信息
     *
     * @param context 上下文对象
     * @return 包含USB设备信息的JSONObject对象
     */
    public static JSONObject getUsbInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("usb", getUsbStatus(context));
            jsonObject.put("adbEnable", adbEnable(context));
        } catch (JSONException e) {
            // 删除ULog，静默处理异常
        }
        return jsonObject;
    }
}
