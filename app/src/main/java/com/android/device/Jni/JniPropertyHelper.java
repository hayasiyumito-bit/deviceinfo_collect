package com.android.device.Jni;

import android.util.Log;

/**
 * JNI 属性读取的安全封装；native 异常或未实现时返回空串，不导致进程崩溃。
 */
public final class JniPropertyHelper {

    private static final String TAG = "JniPropertyHelper";
    private static volatile boolean jniUsable = true;

    private JniPropertyHelper() {
    }

    public static String getSystemPropertyByFind(String key) {
        return readProperty(key, JniInterface::getSystemPropertyByFind);
    }

    public static String getLibcutilsPropertyGet(String key) {
        return readProperty(key, JniInterface::getLibcutilsPropertyGet);
    }

    private static String readProperty(String key, PropertyReader reader) {
        if (!jniUsable || key == null || key.isEmpty()) {
            return "";
        }
        try {
            String value = reader.read(key);
            return value != null ? value.trim() : "";
        } catch (UnsatisfiedLinkError e) {
            jniUsable = false;
            Log.w(TAG, "JNI library unavailable, skip native property read", e);
            return "";
        } catch (Throwable t) {
            Log.w(TAG, "Native property read failed for " + key, t);
            return "";
        }
    }

    @FunctionalInterface
    private interface PropertyReader {
        String read(String key);
    }
}
