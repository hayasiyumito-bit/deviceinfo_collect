package com.android.device.software;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.utils.ULog;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class Library {

    /**
     * 获取系统共享库列表
     *
     * @param context 上下文对象
     * @return 系统共享库列表字符串，如果获取失败则返回空字符串
     */
    public static String getLibraries(Context context) {
        try {
            StringBuffer buffer = new StringBuffer();
            final List<String> list = new ArrayList<String>();
            PackageManager pm = context.getPackageManager();
            final String[] rawList = pm.getSystemSharedLibraryNames();
            for (int i = 0; i < rawList.length; i++) {
                list.add(rawList[i]);
            }
            // sort by name
            Collections.sort(list, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    if (o1 == o2) return 0;
                    if (o1 == null) return -1;
                    if (o2 == null) return 1;
                    return o1.compareTo(o2);
                }
            });

            final int count = (list != null) ? list.size() : 0;
            for (int p = 0; p < count; p++) {
                String lib = list.get(p);
                buffer.append("library:");
                buffer.append(lib).append("\n");
            }
            return buffer.toString();
        } catch (Throwable e) {
//            ULog.e(e);
        }
        return "";
    }

    /**
     * 获取当前Java系统属性，并将其转换为JSONObject对象返回。
     *
     * @return 包含所有系统属性的JSONObject对象
     */
    public static JSONObject getJavaProperties() {
        JSONObject jsonObject = new JSONObject();
        try {
            Properties prop = System.getProperties();
            Set<String> keySets = prop.stringPropertyNames();
            for (String key : keySets) {
                jsonObject.put(key, prop.getProperty(key));
            }
        } catch (Throwable e) {
//            ULog.e(e);
        }
        return jsonObject;
    }


    /**
     * 获取库信息
     *
     * @param context 上下文对象
     * @return 包含库信息和Java属性的JSONObject对象
     * @throws JSONException 如果JSON操作发生异常
     */
    public static JSONObject getLibraryInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("libraries", getLibraries(context));
            jsonObject.put("javaProperties", getJavaProperties());
        } catch (Throwable e) {
            ULog.e(e);
        }
        return jsonObject;
    }

}
