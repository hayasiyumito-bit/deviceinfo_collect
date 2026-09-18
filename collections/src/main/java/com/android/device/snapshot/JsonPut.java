package com.android.device.snapshot;

import android.util.Log;

import org.json.JSONObject;

/**
 * 快照 JSON 读写：统一 null / 异常安全处理。
 */
public final class JsonPut {

    private static final String TAG = "JsonPut";

    private JsonPut() {
    }

    public static String optString(JSONObject json, String key, String defaultValue) {
        return json != null ? json.optString(key, defaultValue) : defaultValue;
    }

    public static int optInt(JSONObject json, String key, int defaultValue) {
        return json != null ? json.optInt(key, defaultValue) : defaultValue;
    }

    public static long optLong(JSONObject json, String key, long defaultValue) {
        return json != null ? json.optLong(key, defaultValue) : defaultValue;
    }

    public static boolean optBoolean(JSONObject json, String key, boolean defaultValue) {
        return json != null ? json.optBoolean(key, defaultValue) : defaultValue;
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
