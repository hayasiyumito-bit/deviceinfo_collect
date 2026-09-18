package com.android.device.hardware;

import static android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY;
import static android.os.BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER;
import static android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE;
import static android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW;
import static android.os.BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER;
import static android.os.BatteryManager.BATTERY_PROPERTY_STATUS;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class Battery {
    /**
     * 获取电池信息
     *
     * @param context 上下文对象
     * @return 包含电池信息的JSON对象，如果获取失败则返回null
     */
    public static JSONObject getBatteryInfo(Context context) {
        BatteryManager manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        JSONObject jsonObject = new JSONObject();
        if (manager != null) {
            try {
                jsonObject.put("charge_counter", manager.getLongProperty(BATTERY_PROPERTY_CHARGE_COUNTER));// 1
                jsonObject.put("current_now", manager.getLongProperty(BATTERY_PROPERTY_CURRENT_NOW));// 2
                jsonObject.put("current_average", manager.getLongProperty(BATTERY_PROPERTY_CURRENT_AVERAGE));// 3
                jsonObject.put("battery_capacity", manager.getLongProperty(BATTERY_PROPERTY_CAPACITY));// 4
                jsonObject.put("energy_counter", manager.getLongProperty(BATTERY_PROPERTY_ENERGY_COUNTER));// 5
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//api >= 26
                    /**
                     *    // values for "status" field in the ACTION_BATTERY_CHANGED Intent
                     *     public static final int BATTERY_STATUS_UNKNOWN = Constants.BATTERY_STATUS_UNKNOWN; 1
                     *     public static final int BATTERY_STATUS_CHARGING = Constants.BATTERY_STATUS_CHARGING; 2
                     *     public static final int BATTERY_STATUS_DISCHARGING = Constants.BATTERY_STATUS_DISCHARGING; 3
                     *     public static final int BATTERY_STATUS_NOT_CHARGING = Constants.BATTERY_STATUS_NOT_CHARGING; 4
                     *     public static final int BATTERY_STATUS_FULL = Constants.BATTERY_STATUS_FULL; 5
                     */

                    jsonObject.put("battery_status", manager.getLongProperty(BATTERY_PROPERTY_STATUS));// 6
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    jsonObject.put("isCharging", manager.isCharging());
                }

                jsonObject.put("powerProfile", getPowerProfile(context));
            } catch (Throwable e) {
//                e.printStackTrace();
                return null;
            }
        }
        return jsonObject;
    }

    /**
     * 获取设备电源配置文件
     *
     * @param context 上下文对象
     * @return 包含电源配置信息的JSONObject对象
     */
    public static JSONObject getPowerProfile(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            Class<?> powerProfileClass = Class.forName("com.android.internal.os.PowerProfile");
            Field[] fields = powerProfileClass.getDeclaredFields();
            for (Field f : fields) {
                String value = String.valueOf(((Double) Class.forName("com.android.internal.os.PowerProfile").getMethod("getAveragePower", String.class).invoke(Class.forName("com.android.internal.os.PowerProfile").getConstructor(Context.class).newInstance(context), f.getName())).doubleValue());
                jsonObject.put(f.getName(), value);
            }
            jsonObject.put("battery_capacity", getBatteryCapacity(context));
            jsonObject.put("num_cpu_clusters", getNumCpuClusters(context));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException | InstantiationException | JSONException e) {
//            throw new RuntimeException(e);
        }
        return jsonObject;
    }

    /**
     * 获取设备电池容量
     *
     * @param context 上下文对象
     * @return 设备的电池容量（单位：mAh）
     * @throws ClassNotFoundException 如果找不到指定的类
     * @throws NoSuchMethodException 如果找不到指定的方法
     * @throws InvocationTargetException 如果方法调用过程中抛出异常
     * @throws IllegalAccessException 如果无法访问指定的方法
     * @throws InstantiationException 如果无法实例化指定的类
     */
    public static String getBatteryCapacity(Context context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return String.valueOf(((Double) Class.forName("com.android.internal.os.PowerProfile").getMethod("getBatteryCapacity", null).invoke(Class.forName("com.android.internal.os.PowerProfile").getConstructor(Context.class).newInstance(context), null)).doubleValue());
    }

    /**
     * 获取CPU集群的数量
     *
     * @param context 上下文对象
     * @return 返回CPU集群数量的字符串表示
     * @throws ClassNotFoundException 当类路径中找不到指定的类时抛出
     * @throws NoSuchMethodException 当找不到指定的方法时抛出
     * @throws InvocationTargetException 当被调用的方法抛出异常时抛出
     * @throws IllegalAccessException 当无法访问指定的方法时抛出
     * @throws InstantiationException 当无法实例化指定的类时抛出
     */
    private static String getNumCpuClusters(Context context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        return String.valueOf(((Integer) Class.forName("com.android.internal.os.PowerProfile").getMethod("getNumCpuClusters", null).invoke(Class.forName("com.android.internal.os.PowerProfile").getConstructor(Context.class).newInstance(context), null)).intValue());
    }


    /**
     * 获取电池信息并返回为JSON对象
     *
     * @param context 上下文对象
     * @return 包含电池信息的JSON对象
     */
    public static JSONObject getAfBatteryInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        IntentFilter AFInAppEventType = new IntentFilter("android.intent.action.BATTERY_CHANGED");
        try {
            Intent registerReceiver = context.registerReceiver(null, AFInAppEventType);
            if (registerReceiver != null) {
                String str;
                if (2 == registerReceiver.getIntExtra("status", -1)) {
                    int plugged = registerReceiver.getIntExtra("plugged", -1);
                    if (plugged != 1) {
                        if (plugged != 2) {
                            if (plugged != 4) {
                                str = "other";
                            } else {
                                str = "wireless";
                            }
                        } else {
                            str = "usb";
                        }
                    } else {
                        str = "ac";
                    }
                } else {
                    str = "no";
                }
                jsonObject.put("btch", str);

                int batteryLevel = registerReceiver.getIntExtra("level", -1);
                int batteryScale = registerReceiver.getIntExtra("scale", -1);
                int batteryTemperature = registerReceiver.getIntExtra("temperature", -2700);

                jsonObject.put("batteryLevel", batteryLevel);
                jsonObject.put("batteryScale", batteryScale);
                jsonObject.put("batteryTemp", batteryTemperature);

                int voltage = registerReceiver.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                jsonObject.put("batteryVoltage", voltage);

                int health = registerReceiver.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                jsonObject.put("batteryHealth", health);

                String technology = registerReceiver.getStringExtra("technology");
                jsonObject.put("batteryTechnology", technology);

                boolean present = registerReceiver.getBooleanExtra("present", false);
                jsonObject.put("batteryPresent", present);
            }
        } catch (Throwable th) {
            Log.e("deviceinfo", "getAfBatteryInfo error");
            return null;
        }
        return jsonObject;
    }
}
