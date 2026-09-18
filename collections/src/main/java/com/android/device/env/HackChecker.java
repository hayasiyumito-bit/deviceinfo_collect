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
import android.util.Log;

import com.android.utils.Cmd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HackChecker {

    private static final String TAG = "HackChecker";

    private HackChecker() {
    }

    public static boolean isEmulator(Context context) {
        int suspectCount = 0;

        if (Build.FINGERPRINT.startsWith("generic")) {
            suspectCount += 10;
        }
        if (Build.FINGERPRINT.toLowerCase(Locale.ROOT).contains("vbox")) {
            suspectCount += 10;
        }
        if (Build.FINGERPRINT.toLowerCase(Locale.ROOT).contains("test-keys")) {
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
            suspectCount++;
        }

        String buildFlavor = Cmd.getProperty("ro.build.flavor");
        if (TextUtils.isEmpty(buildFlavor) || buildFlavor.contains("vbox") || buildFlavor.contains("sdk_gphone")) {
            suspectCount++;
        }

        String productBoard = Cmd.getProperty("ro.product.board");
        if (TextUtils.isEmpty(productBoard)
                || productBoard.contains("android")
                || productBoard.contains("goldfish")) {
            suspectCount++;
        }

        String boardPlatform = Cmd.getProperty("ro.board.platform");
        if (TextUtils.isEmpty(boardPlatform) || boardPlatform.contains("android")) {
            suspectCount++;
        }

        String hardware = Cmd.getProperty("ro.hardware");
        if (TextUtils.isEmpty(hardware)) {
            suspectCount++;
        } else {
            String hardwareLower = hardware.toLowerCase(Locale.ROOT);
            if (hardwareLower.contains("ttvm") || hardwareLower.contains("nox")) {
                suspectCount += 10;
            }
        }

        if (context != null) {
            suspectCount += countEmulatorContextSignals(context);
        }
        return suspectCount > 3;
    }

    private static int countEmulatorContextSignals(Context context) {
        int suspectCount = 0;
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            suspectCount++;
        }

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            return suspectCount + 2;
        }

        if (sensorManager.getSensorList(Sensor.TYPE_ALL).size() <= 7) {
            suspectCount++;
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
            suspectCount++;
        }

        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                suspectCount++;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED
                        && TextUtils.isEmpty(adapter.getName())) {
                    suspectCount++;
                }
            } else if (TextUtils.isEmpty(adapter.getName())) {
                suspectCount++;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Bluetooth check skipped", e);
        }
        return suspectCount;
    }

    public static boolean isVPN(Context context) {
        if (context == null) {
            return false;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }
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
        } catch (RuntimeException e) {
            Log.w(TAG, "VPN check failed", e);
        }
        return false;
    }

    public static boolean isDebug(Context context) {
        if (context == null) {
            return false;
        }
        try {
            boolean debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            return debuggable || Debug.isDebuggerConnected();
        } catch (RuntimeException e) {
            Log.w(TAG, "Debug check failed", e);
            return false;
        }
    }

    public static JSONObject getEnvCheckerInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("isEmulator", isEmulator(context));
            jsonObject.put("isVPN", isVPN(context));
            jsonObject.put("isDebug", isDebug(context));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to build env checker json", e);
        }
        return jsonObject;
    }

    /** 环境检测命中时返回可读原因列表。 */
    public static JSONArray collectDetectionReasons(Context context) {
        JSONArray reasons = new JSONArray();
        if (context == null) {
            return reasons;
        }
        try {
            if (Build.FINGERPRINT.startsWith("generic")) {
                reasons.put("Build.FINGERPRINT 以 generic 开头: " + Build.FINGERPRINT);
            }
            String fingerprintLower = Build.FINGERPRINT.toLowerCase(Locale.ROOT);
            if (fingerprintLower.contains("vbox")) {
                reasons.put("Build.FINGERPRINT 含 vbox: " + Build.FINGERPRINT);
            }
            if (fingerprintLower.contains("test-keys")) {
                reasons.put("Build.FINGERPRINT 含 test-keys: " + Build.FINGERPRINT);
            }
            if (Build.MODEL.contains("google_sdk")) {
                reasons.put("Build.MODEL 含 google_sdk: " + Build.MODEL);
            }
            if (Build.MODEL.contains("Emulator")) {
                reasons.put("Build.MODEL 含 Emulator: " + Build.MODEL);
            }
            if (Build.MODEL.contains("Android SDK built for x86")) {
                reasons.put("Build.MODEL 为 x86 模拟器 SDK: " + Build.MODEL);
            }
            if (Build.MANUFACTURER.contains("Genymotion")) {
                reasons.put("Build.MANUFACTURER 为 Genymotion: " + Build.MANUFACTURER);
            }
            if (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) {
                reasons.put("Build.BRAND/DEVICE 均为 generic");
            }
            if ("google_sdk".equals(Build.PRODUCT)) {
                reasons.put("Build.PRODUCT=google_sdk");
            }

            String baseBandVersion = Cmd.getProperty("gsm.version.baseband");
            if (TextUtils.isEmpty(baseBandVersion) || baseBandVersion.contains("1.0.0.0")) {
                reasons.put("gsm.version.baseband 异常: " + baseBandVersion);
            }

            String buildFlavor = Cmd.getProperty("ro.build.flavor");
            if (TextUtils.isEmpty(buildFlavor) || buildFlavor.contains("vbox") || buildFlavor.contains("sdk_gphone")) {
                reasons.put("ro.build.flavor 疑似模拟器: " + buildFlavor);
            }

            String productBoard = Cmd.getProperty("ro.product.board");
            if (TextUtils.isEmpty(productBoard)
                    || productBoard.contains("android")
                    || productBoard.contains("goldfish")) {
                reasons.put("ro.product.board 疑似模拟器: " + productBoard);
            }

            String boardPlatform = Cmd.getProperty("ro.board.platform");
            if (TextUtils.isEmpty(boardPlatform) || boardPlatform.contains("android")) {
                reasons.put("ro.board.platform 疑似模拟器: " + boardPlatform);
            }

            String hardware = Cmd.getProperty("ro.hardware");
            if (TextUtils.isEmpty(hardware)) {
                reasons.put("ro.hardware 为空");
            } else {
                String hardwareLower = hardware.toLowerCase(Locale.ROOT);
                if (hardwareLower.contains("ttvm") || hardwareLower.contains("nox")) {
                    reasons.put("ro.hardware 含模拟器特征: " + hardware);
                }
            }

            collectEmulatorContextReasons(context, reasons);

            if (isVPN(context)) {
                reasons.put("检测到 VPN 传输层网络");
            }
            if (isDebug(context)) {
                boolean debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                if (debuggable) {
                    reasons.put("应用为可调试构建 (FLAG_DEBUGGABLE)");
                }
                if (Debug.isDebuggerConnected()) {
                    reasons.put("调试器已连接 (Debug.isDebuggerConnected)");
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to collect env reasons", e);
        }
        return reasons;
    }

    private static void collectEmulatorContextReasons(Context context, JSONArray reasons) throws JSONException {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            reasons.put("无相机闪光灯系统特性");
        }

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            reasons.put("无法获取 SensorManager");
            return;
        }

        if (sensorManager.getSensorList(Sensor.TYPE_ALL).size() <= 7) {
            reasons.put("传感器数量 <= 7");
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
            reasons.put("无光线传感器");
        }

        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                reasons.put("BluetoothAdapter 为空");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED
                        && TextUtils.isEmpty(adapter.getName())) {
                    reasons.put("蓝牙名称为空");
                }
            } else if (TextUtils.isEmpty(adapter.getName())) {
                reasons.put("蓝牙名称为空");
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Bluetooth reason skipped", e);
        }
    }
}
