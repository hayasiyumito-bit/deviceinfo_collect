package com.android.device.time;

import android.os.SystemClock;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Time {

    /**
     * 获取系统时间信息
     * @return JSONObject 返回系统时间信息
     */
    private static String getBootTime() {//获取系统开机时间
        return String.valueOf(System.currentTimeMillis() - SystemClock.elapsedRealtime());
    }

    /**
     * 获取当前时间
     * @return String 返回当前时间
     */
    private static String getDate() {//获取当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    /**
     * 获取时间信息
     * @return JSONObject 返回时间信息
     */
    public static JSONObject getTimeInfo() {
        try {
            JSONObject object = new JSONObject();
            object.put("date", getDate());
            object.put("bootTime", getBootTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
