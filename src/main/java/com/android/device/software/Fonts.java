package com.android.device.software;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Fonts {
    /**
     * 获取字体信息
     *
     * @param context 上下文对象
     * @return 包含字体信息的 JSONObject 对象
     * @throws RuntimeException 如果在构建 JSON 对象时发生错误，抛出 RuntimeException 异常
     */
    public static JSONObject getFontsInfo(Context context) {
        JSONObject json = new JSONObject();
        try {
//            json.put("systemFonts", getSystemFonts());
            json.put("fonts", getFonts(context));
            json.put("configuration", getConfiguration(context));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    /**
     * 获取当前上下文环境的配置信息并返回其字符串表示形式。
     *
     * @param context 当前的上下文环境
     * @return 配置信息的字符串表示形式
     */
    public static String getConfiguration(Context context) {
        return context.getResources().getConfiguration().toString();
    }

    /**
     * 获取系统字体列表。
     *
     * @return 包含系统字体信息的 JSONObject 对象。
     */
    public static JSONObject getSystemFonts() {
        ArrayList<String> uArrayList = new ArrayList<>();
        ArrayList<String> nameList = new ArrayList<>();
        JSONObject data = new JSONObject();

        try {
            File uFile = new File("/system/fonts/");
            if (!uFile.exists()) {
                return data;
            } else {
                File[] uFileArray = uFile.listFiles();
                if (uFileArray == null) {
                    return data;
                } else {
                    for (File file : uFileArray) {
                        String name = file.getName();
                        if (!TextUtils.isEmpty(name) && name.indexOf(".") > 0) {
                            String fileName = name.substring(0, name.indexOf("."));
                            if (!uArrayList.contains(name)) {
                                uArrayList.add(name);
                            }

                            if (!nameList.contains(fileName)) {
                                nameList.add(fileName);
                            }
                        }
                    }

                    Collections.sort(nameList);
                    StringBuilder stringBuilder = new StringBuilder();

                    for (int i = 0; i < nameList.size(); ++i) {
                        stringBuilder.append(nameList.get(i));
                    }

                    ArrayList<String> addList = new ArrayList<>();
                    for (String str : uArrayList) {
                        if (!Fonts_sdk_static.getFonts(Build.VERSION.SDK_INT).contains(str)) {
                            addList.add(str);
                        }
                    }
                    ArrayList<String> deleteList = new ArrayList<>();
                    for (String str : Fonts_sdk_static.getFonts(Build.VERSION.SDK_INT)) {
                        if (!uArrayList.contains(str)) {
                            deleteList.add(str);
                        }
                    }

                    data.put("filesNameMd5", calculateMD5(stringBuilder.toString()));
                    data.put("files", addList);
                    data.put("deleteFiles", deleteList);
                }
            }
        } catch (Exception var9) {
            //dont do anything
        }
        return data;
    }

    /**
     * 计算给定字符串的MD5哈希值。
     *
     * @param message 需要计算MD5哈希值的字符串
     * @return 计算得到的MD5哈希值（以十六进制字符串表示），如果发生异常则返回null
     */
    private static String calculateMD5(String message) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(message.getBytes());
            byte[] bytes = md5.digest();
            StringBuilder hexString = new StringBuilder(32);
            byte[] var4 = bytes;
            int var5 = bytes.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                byte b = var4[var6];
                String hex = Integer.toHexString(255 & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException var9) {
            return null;
        }
    }

    /**
     * 获取系统字体列表
     *
     * @param context 上下文对象
     * @return 系统字体列表
     */
    public static List<String> getFonts(Context context) {
        List<String> fonts = new ArrayList();
        try {
            Field declaredFiel = Typeface.class.getDeclaredField("sSystemFontMap");
            declaredFiel.setAccessible(true);
            Map map = (Map) declaredFiel.get(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            StringBuilder sb = new StringBuilder();
            Iterator var5 = map.keySet().iterator();

            while (var5.hasNext()) {
                Object key = var5.next();
                sb.append(key).append(",");
                fonts.add(key + "");
            }

            Log.d("fonts", " = " + sb);
        } catch (Exception var7) {
            Exception e = var7;
            Log.e("fonts", " = " + e);
        }

        return fonts;
    }
}
