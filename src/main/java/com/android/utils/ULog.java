package com.android.utils;

import android.util.Log;

/**
 * Thin logging wrapper for the collections module.
 */
public final class ULog {

    private static final String TAG = "deviceinfo";

    private ULog() {
    }

    public static void e(Throwable e) {
        if (e != null) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static void d(String s) {
        if (s != null) {
            Log.d(TAG, s);
        }
    }

    public static void e(String s) {
        if (s != null) {
            Log.e(TAG, s);
        }
    }

    public static void v(String s) {
        if (s != null) {
            Log.v(TAG, s);
        }
    }

    public static void i(String s) {
        if (s != null) {
            Log.i(TAG, s);
        }
    }
}
