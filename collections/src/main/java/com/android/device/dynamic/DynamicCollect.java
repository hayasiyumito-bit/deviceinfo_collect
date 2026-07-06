package com.android.device.dynamic;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import org.json.JSONObject;
import java.util.List;
import android.hardware.Sensor;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.BatteryManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import com.android.device.hardware.Storage;
import com.android.utils.Http;

import java.util.ArrayList;
public class DynamicCollect implements SensorEventListener{

    private Context context;
    private boolean running = false;
    private static long collectFreq = 86400; // 3 seconds interval

    private static long riseFreq = 86400;

    private static JSONObject dynamicUpload = new JSONObject();

    private static List<JSONObject> sensorList = new ArrayList<>();

    private static List<JSONObject> othersList = new ArrayList<>();

    private static Object lock = new Object();

    //private SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

    /**
     * 获取动态上传的JSON对象
     *
     * @return 返回动态上传的JSON对象
     */
    public static JSONObject getDynamicJson() {
        return dynamicUpload;
    }

    public DynamicCollect(Context context) {
        this.context = context;
    }

    public static void uploadParameter(int cf, int rf) {
        collectFreq = cf;
        riseFreq = rf;
    }

    /**
     * 计算JSON对象列表中所有对象的字符总数
     *
     * @param arrayList 包含JSONObject的列表
     * @return 所有JSONObject对象的字符总数
     */
    private static int getTotalCharacters(List<JSONObject> arrayList) {
        int totalCharacters = 0;
        for (JSONObject jsonObject : arrayList) {
            totalCharacters += jsonObject.length();
        }
        return totalCharacters;
    }
    /**
     * 注册传感器数据采集功能。
     *
     * <p>通过调用该方法，程序将注册并监听加速度计、磁场传感器和陀螺仪这三种传感器。每当这些传感器的数据发生变化时，
     * 都会触发传感器事件监听器（即实现了{@link SensorEventListener}接口的对象）中的相应方法，例如
     * {@link SensorEventListener#onSensorChanged(SensorEvent)}。
     */
    private void registerSensorCollection() {
        // Collect sensor data
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            // You can add sensor data to the JSON object here
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    /**
     * 取消注册传感器数据收集
     *
     * 该方法用于取消注册传感器监听器，停止收集传感器数据。
     *
     * 在循环遍历所有传感器类型时，仅针对加速度计（TYPE_ACCELEROMETER）、磁力计（TYPE_MAGNETIC_FIELD）和陀螺仪（TYPE_GYROSCOPE）
     * 这三种传感器取消注册监听器。
     */
    private void unregisterSensorCollection() {
        // Collect sensor data
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            // You can add sensor data to the JSON object here
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER || sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD || sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                sm.unregisterListener(this);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // 处理传感器数据变化事件
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ||
                event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD ||
                event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            JSONObject data = new JSONObject();
            try {
                data.put("time", System.currentTimeMillis());
                data.put("type", event.sensor.getType());
                data.put("x", x);
                data.put("y", y);
                data.put("z", z);
                int total = 1;
                synchronized (lock) {
                    //Log.e("xufuhaixufuhai", "onSensorChanged sensorList:" + data);
                    sensorList.add(data);
                    //total = getTotalCharacters(sensorList) + getTotalCharacters(othersList);
                }
                //Log.e("xufuhai", "DataUploadTask:sensorList getTotalCharacters:" + total);
                if (false) {
                    //Log.e("xufuhai", "DataUploadTask:sensorList");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Log.e("xufuhaixufuhai", "onSensorChanged uploadAndReportData");
                            uploadAndReportData();
                        }
                    }).start();
                }
                //Log.e("xufuhai", "sensor_dynamic_data:" + dynamicUpload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 传感器精度变化时的处理
        //Log.d("xufuhai", "onAccuracyChanged: x=");
    }

    public void start() {
        if (!running) {
            running = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doDataCollectionTask();
                }
            }).start();
            registerSensorCollection();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doDataUploadTask();
                }
            }).start();
        }
    }

    public void stop() {
        running = false;
        unregisterSensorCollection();
        try {
            //Log.e("xufuhaixufuhai", "stop uploadAndReportData");
            uploadAndReportData();
            //Log.e("xufuhai", "dynamic_data:" + dynamicUpload);
        } catch(Exception e) {
        }
    }

    private void collectAndReportData() {
        JSONObject data = new JSONObject();
        try {
            // Collect battery information
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int temp  = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -2700);
            float batteryPct = (level * 100.0f) / scale;
            String str = null;
            if (2 == batteryStatus.getIntExtra("status", -1)) {
                int intExtra = batteryStatus.getIntExtra("plugged", -1);
                if (intExtra != 1) {
                    if (intExtra != 2) {
                        if (intExtra != 4) {
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
            data.put("time", System.currentTimeMillis());
            data.put("battery_level", batteryPct);
            data.put("battery_temp", temp);
            data.put("battery_btch", str);
            // Collect device memory and storage information
            StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
            data.put("memory", Storage.getAvailMemory(context));
            data.put("af_disk", Storage.getDiskSize2AF());
            int total = 1;
            synchronized (lock) {
                othersList.add(data);
                //total = getTotalCharacters(sensorList) + getTotalCharacters(othersList);
            }
            //Log.e("xufuhai", "DataUploadTask:othersList getTotalCharacters:" + total);
            if (false) {
                //Log.e("xufuhai", "DataUploadTask:othersList");
                //Log.e("xufuhaixufuhai", "collectAndReportData uploadAndReportData");
                uploadAndReportData();
            }
            //Log.e("xufuhai", "others_dynamic_data:" + dynamicUpload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void uploadAndReportData() {
        try {
            int retry = 0;
            synchronized (lock) {
                dynamicUpload.put("model", Build.MODEL);
                dynamicUpload.put("sdk", String.valueOf(Build.VERSION.SDK_INT));
                dynamicUpload.put("build_id", Build.ID);
                ArrayList<JSONObject> othersListCopy = new ArrayList<>(othersList);
                ArrayList<JSONObject> sensorListCopy = new ArrayList<>(sensorList);
                dynamicUpload.put("others_dynamic_data", othersListCopy);
                dynamicUpload.put("sensor_dynamic_data", sensorListCopy);
            }
            JSONObject jsonObject = DynamicCollect.getDynamicJson();
            while (retry < 5) {
                String result;
                //Log.e("xufuhaixufuhai", "uploadAndReportData jsonObject" + jsonObject.toString().length() + " " + jsonObject.toString());
                result = Http.uploadData(jsonObject.toString(), "https://iboot.site/dio/rdd",null);
                //Log.e("xufuhai", "result: " + result);
                JSONObject jsonObjectResult = new JSONObject(result);
                //Log.e("xufuhai", "jsonObjectResult: " + jsonObjectResult);
                int code = jsonObjectResult.getInt("code");
                if (result != "" && result != "-1" && code == 0) {
                    //Log.e("xufuhai", "uploadAndReportData finish uploading dynamic date");
                    synchronized (lock) {
                        sensorList.clear();
                        othersList.clear();
                    }
                    //Log.e("xufuhaixufuhai", "uploadAndReportData unregisterSensorCollection");
                    unregisterSensorCollection();
                    return;
                } else {
                    SystemClock.sleep(1000);
                    retry++;
                    unregisterSensorCollection();
                }
            }
        } catch (Exception e) {
            //Log.e("xufuhaixufuhai", "Exception uploadAndReportData unregisterSensorCollection");
            unregisterSensorCollection();
//            e.printStackTrace();
        }
    }
    protected Void doDataUploadTask() {
        //Log.e("xufuhai", "DataUploadTask:doInBackground1");
        while (running) {
            //Log.e("xufuhai", "DataUploadTask:doInBackground");
            //uploadAndReportData();
            try {
                Thread.sleep(1*1000);
                //Log.e("xufuhaixufuhai", "doDataUploadTask uploadAndReportData");
                uploadAndReportData();
                Thread.sleep(riseFreq*1000);
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
        return null;
    }

    protected Void doDataCollectionTask() {
        //Log.e("xufuhai", "DataCollectionTask:doInBackground1");
        while (running) {
            //SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            //Log.e("xufuhai", "DataCollectionTask:doInBackground");
            collectAndReportData();
            try {
                Thread.sleep(collectFreq*1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
