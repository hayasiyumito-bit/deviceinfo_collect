package com.java.se;

import com.java.se.utils.Encrypt;

import java.util.Locale;

public class Main {
    public static void main(String[] args) throws Exception {

        byte[] bytes = {-84, 18, 6, 58};
//        System.out.printf("dasdsa");
        System.out.println(byteArray2Int(bytes));

    }

    public static int bytesToInt(byte[] bytes) {
        int addr = bytes[3] & 0xFF;
        addr |= ((bytes[2] << 8) & 0xFF00);
        addr |= ((bytes[1] << 16) & 0xFF0000);
        addr |= ((bytes[0] << 24) & 0xFF000000);
        return addr;
    }

    /**
     * byte数组转换为整型
     * @param b
     * @return
     */
    public static int byteArray2Int(byte[] b) {
        int value = 0;
        for (int i = b.length-1; i >=0; i--) {
            int shift = (b.length-1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }

        return value;
    }
}