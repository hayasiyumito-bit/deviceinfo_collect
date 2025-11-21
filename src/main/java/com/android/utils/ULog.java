package com.android.utils;


import android.util.Log;

public class ULog {
    public static void e(Throwable e) {
        Log.e(ULog.class.getSimpleName(), "exception", e);
    }

    public static void d(String s) {
        if (s == null) return;
        Log.d(ULog.class.getSimpleName(), s);
    }

    public static void e(String s) {
        if (s == null) return;
        Log.e(ULog.class.getSimpleName(), s);
    }

    public static void v(String s) {
        if (s == null) return;
        Log.v(ULog.class.getSimpleName(), s);
    }

    public static void i(String s) {
        if (s == null) return;
        Log.i(ULog.class.getSimpleName(), s);
    }

}
