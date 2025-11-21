package com.android.device.software;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SettingsInfo {
    /**
     * 获取系统设置信息
     * @param context 上下文对象
     * @return JSONObject 返回系统设置信息
     */
    public static JSONObject getSettingsInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            ContentResolver contentResolver = context.getContentResolver();
            // 获取System和Secure的所有字段
//            Field[] fields = Settings.System.class.getFields();
            Field[] fields = Settings.System.class.getDeclaredFields();
            JSONArray Settings_System_Arr = new JSONArray();
            for (Field field : fields) {
                try {
                    String name = field.getName();
                    String filed = (String) field.get(null);
                    String value = Settings.System.getString(contentResolver, filed);
                    Settings_System_Arr.put(name + ":" + value);
                } catch (Exception e) {
                    continue;
                }
            }

            fields = Settings.Secure.class.getDeclaredFields();
            JSONArray Settings_Secure_Arr = new JSONArray();
            for (Field field : fields) {
                try {
                    String name = field.getName();
                    String filed = (String) field.get(null);
                    String value = Settings.Secure.getString(contentResolver, filed);
                    Settings_Secure_Arr.put(name + ":" + value);
                } catch (Exception e) {
                    continue;
                }
            }
            fields = Settings.Global.class.getDeclaredFields();
            JSONArray Settings_Global_Arr = new JSONArray();
            for (Field field : fields) {
                try {
                    String name = field.getName();
                    String filed = (String) field.get(null);
                    String value = Settings.Secure.getString(contentResolver, filed);
                    Settings_Global_Arr.put(name + ":" + value);
                } catch (Exception e) {
                    continue;
                }
            }
            jsonObject.put("settingSystem", Settings_System_Arr);
            jsonObject.put("settingSecure", Settings_Secure_Arr);
            jsonObject.put("settingGlobal", Settings_Global_Arr);
        } catch (Exception e) {
            e.printStackTrace();

        }
        return jsonObject;
    }
}
