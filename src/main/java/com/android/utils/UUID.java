package com.android.utils;

public final class UUID {
    public static String randomUUID() {
        try {
            java.util.UUID uuid = java.util.UUID.randomUUID();
            return MD5.stringToMD5(uuid.toString() + System.currentTimeMillis());
        } catch (Exception e) {
            ULog.e(e);
        }
        return "";
    }
}
