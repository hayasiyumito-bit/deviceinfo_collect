package com.android.device.env;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Debug;
import android.text.TextUtils;


import com.android.UApplication;
import com.android.utils.Cmd;
import com.android.utils.ULog;

import org.json.JSONException;
import org.json.JSONObject;


public class HackChecker {

    public static boolean isEmulator(Context context) {
        int suspectCount = 0;

        if (Build.FINGERPRINT.startsWith("generic")) {
            suspectCount += 10;
        }
        if (Build.FINGERPRINT.toLowerCase().contains("vbox")) {
            suspectCount += 10;
        }
        if (Build.FINGERPRINT.toLowerCase().contains("test-keys")) {
            suspectCount += 10;
        }
        if (Build.MODEL.contains("google_sdk")) {
            suspectCount += 10;
        }
        if (Build.MODEL.contains("Emulator")) {
            suspectCount += 10;
        }
        if (Build.MODEL.contains("Android SDK built for x86")) {
            suspectCount += 10;
        }
        if (Build.MANUFACTURER.contains("Genymotion")) {
            suspectCount += 10;
        }
        if (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) {
            suspectCount += 10;
        }
        if ("google_sdk".equals(Build.PRODUCT)) {
            suspectCount += 10;
        }

        String baseBandVersion = Cmd.getProperty("gsm.version.baseband");
        if (TextUtils.isEmpty(baseBandVersion) || baseBandVersion.contains("1.0.0.0")) {
            ++suspectCount;
        }

        String buildFlavor = Cmd.getProperty("ro.build.flavor");
        if (TextUtils.isEmpty(buildFlavor) || buildFlavor.contains("vbox") || buildFlavor.contains("sdk_gphone")) {
            ++suspectCount;
        }

        String productBoard = Cmd.getProperty("ro.product.board");
        if (TextUtils.isEmpty(productBoard) || productBoard.contains("android") | productBoard.contains("goldfish")) {
            ++suspectCount;
        }

        String boardPlatform = Cmd.getProperty("ro.board.platform");
        if (TextUtils.isEmpty(boardPlatform) || boardPlatform.contains("android")) {
            ++suspectCount;
        }

        String hardware = Cmd.getProperty("ro.hardware");
        if (TextUtils.isEmpty(hardware)) {
            ++suspectCount;
        } else if (hardware.toLowerCase().contains("ttvm")) {
            suspectCount += 10;
        } else if (hardware.toLowerCase().contains("nox")) {
            suspectCount += 10;
        }
        if (context != null) {
            boolean isSupportCameraFlash = context.getPackageManager().hasSystemFeature("android.hardware.camera.flash");
            if (!isSupportCameraFlash) ++suspectCount;

            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            int sensorSize = sm.getSensorList(Sensor.TYPE_ALL).size();
            if (sensorSize <= 7) {
                ++suspectCount;
            }
            //光传感器判断
            Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (sensor == null) {
                suspectCount++;
            }

            try {//蓝牙判断
                BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
                if (ba == null) {
                    suspectCount++;
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            String name = ba.getName();
                            if (TextUtils.isEmpty(name)) {
                                suspectCount++;
                            }
                        }
                    }

                }
            } catch (Throwable e) {

            }
        }
        return suspectCount > 3;
    }

    public static boolean isVPN(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network[] networks = cm.getAllNetworks();
            if (networks == null || networks.length == 0) {
                return false;
            }
            for (Network network : networks) {
                NetworkInfo networkInfo = cm.getNetworkInfo(network);
                if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isDebug(Context context) {
        try {
            return (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE) != 0 || Debug.isDebuggerConnected();
        } catch (Exception e) {
            ULog.e(e);
        }
        return false;
    }

    public static JSONObject getEnvCheckerInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("isEmulator", isEmulator(context));
            jsonObject.put("isVPN", isVPN(context));
            jsonObject.put("isDebug", isDebug(context));
        } catch (JSONException e) {
            ULog.e(e);
        }
        return jsonObject;
    }

}