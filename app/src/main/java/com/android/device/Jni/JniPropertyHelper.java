package com.android.device.Jni;

import android.util.Log;

/**
 * JNI 属性读取的安全封装；失败时返回 {@code Error: ...}，便于 debug_output 区分异常与空值。
 */
public final class JniPropertyHelper {

    private static final String TAG = "JniPropertyHelper";
    private static final String ERROR_PREFIX = "Error:";
    private static final String JNI_UNAVAILABLE = "Error: JNI library unavailable";

    private static volatile boolean jniUsable = true;

    private JniPropertyHelper() {
    }

    public static boolean isErrorResult(String value) {
        return value != null && value.startsWith(ERROR_PREFIX);
    }

    public static String getSystemPropertyByGet(String key) {
        return readProperty(key, JniInterface::getSystemPropertyByGet);
    }

    public static String getSystemPropertyByFind(String key) {
        return readProperty(key, JniInterface::getSystemPropertyByFind);
    }

    public static String getLibcutilsPropertyGet(String key) {
        return readProperty(key, JniInterface::getLibcutilsPropertyGet);
    }

    public static String getNativePropertyDiagnostics() {
        if (!jniUsable) {
            return "{\"error\":\"Error: JNI library unavailable\"}";
        }
        try {
            String value = JniInterface.getNativePropertyDiagnostics();
            if (value == null || value.trim().isEmpty()) {
                return "{\"error\":\"Error: native diagnostics returned empty\"}";
            }
            return value.trim();
        } catch (UnsatisfiedLinkError e) {
            jniUsable = false;
            Log.w(TAG, "Native diagnostics read failed", e);
            return "{\"error\":\"" + JNI_UNAVAILABLE + "\"}";
        } catch (Throwable t) {
            Log.w(TAG, "Native diagnostics read failed", t);
            return "{\"error\":\"" + toErrorMessage(t) + "\"}";
        }
    }

    public static String getMagiskNativeProbe() {
        if (!jniUsable) {
            return "{\"error\":\"" + JNI_UNAVAILABLE + "\"}";
        }
        try {
            String value = JniInterface.getMagiskNativeProbe();
            if (value == null || value.trim().isEmpty()) {
                return "{\"error\":\"Error: native magisk probe returned empty\"}";
            }
            return value.trim();
        } catch (UnsatisfiedLinkError e) {
            jniUsable = false;
            Log.w(TAG, "Native magisk probe failed", e);
            return "{\"error\":\"" + JNI_UNAVAILABLE + "\"}";
        } catch (Throwable t) {
            Log.w(TAG, "Native magisk probe failed", t);
            return "{\"error\":\"" + toErrorMessage(t) + "\"}";
        }
    }

    private static String readProperty(String key, PropertyReader reader) {
        if (key == null || key.isEmpty()) {
            return "Error: invalid property key";
        }
        if (!jniUsable) {
            return JNI_UNAVAILABLE;
        }
        try {
            String value = reader.read(key);
            if (value == null) {
                return "Error: native returned null";
            }
            return value.trim();
        } catch (UnsatisfiedLinkError e) {
            jniUsable = false;
            Log.w(TAG, "JNI library unavailable, skip native property read", e);
            return JNI_UNAVAILABLE;
        } catch (Throwable t) {
            Log.w(TAG, "Native property read failed for " + key, t);
            return toErrorMessage(t);
        }
    }

    private static String toErrorMessage(Throwable t) {
        if (t == null) {
            return "Error: unknown failure";
        }
        String message = t.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return ERROR_PREFIX + " " + message.trim();
        }
        return ERROR_PREFIX + " " + t.getClass().getSimpleName();
    }

    @FunctionalInterface
    private interface PropertyReader {
        String read(String key);
    }
}
