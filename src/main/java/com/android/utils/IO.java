package com.android.utils;


import java.io.Closeable;

public class IO {
    public static void close(Closeable x) {
        if (x != null) {
            try {
                x.close();
            } catch (Exception e) {
                ULog.e(e);
            }
        }

    }
}
