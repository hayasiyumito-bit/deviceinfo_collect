package com.android.device.hardware;

import static android.content.Context.SENSOR_SERVICE;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;

import com.android.utils.ULog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Sensor {
    /**
     * 获取设备传感器信息
     *
     * @param context 上下文对象
     * @return 包含传感器信息的 JSONObject 对象
     */
    public static JSONObject getSensorInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            SensorManager sm = (SensorManager) context.getSystemService(SENSOR_SERVICE);
            List<android.hardware.Sensor> allSensors = sm.getSensorList(android.hardware.Sensor.TYPE_ALL);
            for (android.hardware.Sensor sensor : allSensors) {
                jsonObject.put(sensor.getName(), sensor.toString());
            }
        } catch (Exception e) {
//            ULog.e(e);
        }
        return jsonObject;
    }

    /**
     * 获取设备支持的传感器信息
     *
     * @param context 上下文对象
     * @return 包含设备支持的传感器信息的JSONObject对象
     * @throws RuntimeException 如果JSON操作抛出异常，则抛出此异常
     */
    public static JSONObject getSensorSuppord(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("FEATURE_CONSUMER_IR", getSuppordSensor(context, PackageManager.FEATURE_CONSUMER_IR));
            jsonObject.put("FEATURE_USB_ACCESSORY", getSuppordSensor(context, PackageManager.FEATURE_USB_ACCESSORY));
            jsonObject.put("FEATURE_USB_HOST", getSuppordSensor(context, PackageManager.FEATURE_USB_HOST));
            jsonObject.put("FEATURE_IRIS", getSuppordSensor(context, PackageManager.FEATURE_IRIS));
            jsonObject.put("FEATURE_FACE", getSuppordSensor(context, PackageManager.FEATURE_FACE));
            jsonObject.put("FEATURE_FINGERPRINT", getSuppordSensor(context, PackageManager.FEATURE_FINGERPRINT));
            jsonObject.put("FEATURE_SCREEN_LANDSCAPE", getSuppordSensor(context, PackageManager.FEATURE_SCREEN_LANDSCAPE));
            jsonObject.put("FEATURE_SCREEN_PORTRAIT", getSuppordSensor(context, PackageManager.FEATURE_SCREEN_PORTRAIT));
            jsonObject.put("FEATURE_MICROPHONE", getSuppordSensor(context, PackageManager.FEATURE_MICROPHONE));
            jsonObject.put("FEATURE_LOCATION_GPS", getSuppordSensor(context, PackageManager.FEATURE_LOCATION_GPS));
            jsonObject.put("FEATURE_NFC", getSuppordSensor(context, PackageManager.FEATURE_NFC));
            jsonObject.put("FEATURE_CAMERA", getSuppordSensor(context, PackageManager.FEATURE_CAMERA));
            jsonObject.put("FEATURE_FAKETOUCH", getSuppordSensor(context, PackageManager.FEATURE_FAKETOUCH));
            jsonObject.put("FEATURE_TOUCHSCREEN", getSuppordSensor(context, PackageManager.FEATURE_TOUCHSCREEN));
            jsonObject.put("FEATURE_TELEPHONY", getSuppordSensor(context, PackageManager.FEATURE_TELEPHONY));
            jsonObject.put("FEATURE_BLUETOOTH_LE", getSuppordSensor(context, PackageManager.FEATURE_BLUETOOTH_LE));
            jsonObject.put("FEATURE_BLUETOOTH", getSuppordSensor(context, PackageManager.FEATURE_BLUETOOTH));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject;
    }

    /**
     * 获取加速度传感器信息
     *
     * @param context 上下文对象
     * @return 包含加速度传感器信息的 JSONObject 对象
     */
    public static JSONObject getAfSensorInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            SensorManager sm = (SensorManager) context.getSystemService(SENSOR_SERVICE);
            List<android.hardware.Sensor> allSensors = sm.getSensorList(android.hardware.Sensor.TYPE_ALL);
            for (android.hardware.Sensor sensor : allSensors) {
                if (sensor.getType() == 1 || sensor.getType() == 2 || sensor.getType() == 4) {
                    JSONObject sensorJson = new JSONObject();
                    sensorJson.put("sN", sensor.getName());
                    sensorJson.put("sV", sensor.getVendor());
                    sensorJson.put("sT", sensor.getType());
                    sensorJson.put("sM", sensor.getMaxDelay());
                    jsonObject.put(String.valueOf(sensor.getType()), sensorJson);
                }
            }
        } catch (Exception e) {
//            ULog.e(e);
        }
        return jsonObject;
    }

    /**
     * 判断设备是否支持指定的传感器
     *
     * @param context 上下文对象
     * @param type    传感器类型，如 "android.hardware.sensor.accelerometer"
     * @return 如果设备支持指定的传感器，则返回 true；否则返回 false
     */
    public static boolean getSuppordSensor(Context context, String type) {
        return context.getPackageManager().hasSystemFeature(type);
    }
}
