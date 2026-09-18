package com.android.device.hardware;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;


import org.json.JSONException;
import org.json.JSONObject;

public class Keyguard {
    /**
     * 检查设备是否处于锁定状态。
     *
     * @param context 上下文对象，通常使用getApplicationContext()或者this。
     * @return 如果设备处于锁定状态，则返回true；否则返回false。
     */
    public static boolean isKeyguardLocked(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardLocked(); // 检查设备是否锁定
    }

    /**
     * 判断设备是否设置了安全的锁屏。
     *
     * @param context 上下文对象，用于获取系统服务。
     * @return 如果设备设置了安全的锁屏，则返回true；否则返回false。
     */
    public static boolean isKeyguardSecure(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure(); // 检查设备是否锁定
    }

    /**
     * 检查设备是否锁定。
     *
     * @param context 上下文对象
     * @return 如果设备已锁定，则返回true；否则返回false
     */
    public static boolean isDeviceLocked(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return keyguardManager.isDeviceLocked(); // 检查设备是否锁定
        }
        return false;
    }

    /**
     * 判断设备是否安全（锁定）
     *
     * @param context 上下文环境
     * @return 如果设备已锁定，则返回true；否则返回false
     */
    public static boolean isDeviceSecure(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceSecure(); // 检查设备是否锁定
        }
        return false;
    }

    /**
     * 检查设备是否处于锁定状态。
     *
     * @param context 上下文对象
     * @return 如果设备处于锁定状态，则返回true；否则返回false
     */
    public static boolean inKeyguardRestrictedInputMode(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.inKeyguardRestrictedInputMode(); // 检查设备是否锁定
    }

    /**
     * 获取设备锁屏信息
     *
     * @param context 上下文
     * @return 包含设备锁屏信息的 JSONObject 对象
     */
    public static JSONObject getKeyguardInfo(Context context){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("isKeyguardLocked", isKeyguardLocked(context));
            jsonObject.put("isKeyguardSecure", isKeyguardSecure(context));
            jsonObject.put("isDeviceLocked", isDeviceLocked(context));
            jsonObject.put("isDeviceSecure", isDeviceSecure(context));
            jsonObject.put("inKeyguardRestrictedInputMode", inKeyguardRestrictedInputMode(context));
        } catch (JSONException e) {
            // 删除ULog，静默处理异常
        }
        return jsonObject;
    }
}
