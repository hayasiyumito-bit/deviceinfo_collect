package com.android.utils;

import android.util.Base64;

public class CompressString {

    public static String compressString(String input) {
        if (input.isEmpty())return input;
        byte[] inputBytes = input.getBytes();
        byte[] encodedBytes = Base64.encode(inputBytes, Base64.DEFAULT);
        String res = new String(encodedBytes);
        return res;
    }

    public static String decompressString(String input) {
        if (input.isEmpty())return input;
        byte[] decodedBytes = Base64.decode(input, Base64.DEFAULT);
        return new String(decodedBytes);
    }
}
