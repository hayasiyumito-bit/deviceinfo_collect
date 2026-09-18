package com.android.device.software;

import android.content.Context;
import android.provider.Settings;

import com.android.utils.Cmd;

import org.json.JSONObject;

public class Input {
    /**
     * 用于检测自动化模拟点击
     */
    /**
     * 获取默认输入法
     *
     * @param context 上下文
     * @return 默认输入法的字符串表示，如果没有找到则返回空字符串
     */
    public static String getDefaultInputMethod(Context context) {
        String string = Settings.Secure.getString(context.getContentResolver(), "default_input_method");
        return string == null ? "" : string;
    }


    /**
     * 获取输入法的相关信息
     *
     * @param context 上下文对象
     * @return 包含输入法信息的 JSONObject 对象
     */
    public static JSONObject getInputInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("defaultInputMethod", getDefaultInputMethod(context));
            jsonObject.put("imeList", Cmd.exe("ime list -s"));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

}
