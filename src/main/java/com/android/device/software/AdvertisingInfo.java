package com.android.device.software;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AdvertisingInfo {
    public static JSONObject getAdvertisingInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            // 动态加载 AdvertisingIdClient 类
            Class<?> advertisingIdClientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");

            // 获取 getAdvertisingIdInfo 方法的 Method 对象
            Method getAdvertisingIdInfoMethod = advertisingIdClientClass.getMethod("getAdvertisingIdInfo", Context.class);

            // 调用 getAdvertisingIdInfo 方法获取 AdvertisingIdClient.Info 对象
            Object adInfo = getAdvertisingIdInfoMethod.invoke(null, context);

            // 获取 AdvertisingIdClient.Info 类的 Class 对象
            Class<?> adInfoClass = adInfo.getClass();

            // 获取 isLimitAdTrackingEnabled 方法的 Method 对象并调用
            Method isLimitAdTrackingEnabledMethod = adInfoClass.getMethod("isLimitAdTrackingEnabled");
            boolean deviceTrackingDisabled = (boolean) isLimitAdTrackingEnabledMethod.invoke(adInfo);

            // 获取 getId 方法的 Method 对象并调用
            Method getIdMethod = adInfoClass.getMethod("getId");
            String advertiserId = (String) getIdMethod.invoke(adInfo);

            jsonObject.put("deviceTrackingDisabled", deviceTrackingDisabled);
            jsonObject.put("advertiserId", advertiserId);
        } catch (ClassNotFoundException e) {
            Log.e("AdInfo", "类未找到: " + e);
        } catch (NoSuchMethodException e) {
            Log.e("AdInfo", "方法未找到: " + e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getCause();
            if (targetException instanceof IOException) {
                Log.e("AdInfo", "网络连接异常: " + targetException.getMessage());
            } else if (targetException != null) {
                Log.e("AdInfo", "Google Play服务不可用");
            } else {
                Log.e("AdInfo", "获取失败: " + targetException.toString());
            }
        } catch (IllegalAccessException e) {
            Log.e("AdInfo", "非法访问: " + e);
        } catch (Exception e) {
            Log.e("AdInfo", "获取失败: " + e);
        }

        return jsonObject;
    }
}
