package com.android.device.software;

import android.util.Log;

import com.android.utils.ULog;

import org.json.JSONObject;

import java.lang.reflect.Field;

public class Build {
    /**
     * 获取系统构建信息
     *
     * @return 包含系统构建信息的JSONObject对象
     */
    public static JSONObject getBuildInfo() {
        JSONObject jsonObject = new JSONObject();
        try {
            Field[] fields = android.os.Build.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                jsonObject.put(field.getName(), String.valueOf(field.get(null)));
            }
        } catch (Throwable e) {
            ULog.e(e);
        }
        try {
            Field[] fields = android.os.Build.VERSION.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                jsonObject.put(field.getName(), String.valueOf(field.get(null)));
            }
        } catch (Throwable e) {
            ULog.e(e);
        }
        return jsonObject;
    }
}
