package com.java.se.utils;

import java.security.MessageDigest;

public final class MD5 {

    public static String getPMD5(String packageName) {
        return stringToMD5("688158" + packageName + "_ucloud_");
    }

    public static String stringToMD5(String string) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                if ((b & 0xFF) < 0x10)
                    hex.append("0");
                hex.append(Integer.toHexString(b & 0xFF));
            }
            return hex.toString();
        } catch (Throwable e) {
        }
        return "";
    }
}
