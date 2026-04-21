package com.android.device.snapshot;

import android.util.Log;

import org.json.JSONObject;

/**
 * 快照 JSON 写入：全模块统一用此处做 null / 异常安全写入，避免各处重复 try/catch。
 */
public final class JsonPut {

    private static final String TAG = "JsonPut";

    private JsonPut() {
    }

    /**
     * 将 value 写入 json；null 写入 {@link JSONObject#NULL}；异常时写入可读错误串。
     */
    public static void put(JSONObject json, String key, Object value) {
        try {
            if (value != null) {
                json.put(key, value);
            } else {
                json.put(key, JSONObject.NULL);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to put key: " + key, e);
            try {
                json.put(key, "Error: " + e.getMessage());
            } catch (Exception ignored) {
            }
        }
    }
}
