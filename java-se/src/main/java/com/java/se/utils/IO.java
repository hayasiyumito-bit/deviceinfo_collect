package com.java.se.utils;


import java.io.Closeable;

public class IO {
    public static void close(Closeable x) {
        if (x != null) {
            try {
                x.close();
            } catch (Exception e) {
                // 删除ULog，静默处理异常
            }
        }

    }
}
