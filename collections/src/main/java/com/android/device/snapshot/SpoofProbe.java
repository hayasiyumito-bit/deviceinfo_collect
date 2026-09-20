package com.android.device.snapshot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.json.JSONObject;

import java.util.List;

/**
 * 伪装核对（Spoof Probe）：把最常被 Hook 伪装的设备标识 / SIM / 位置字段集中读取到一个栏目，
 * 便于人工核对（对照 YumyHook 注入值）。每个字段独立 try/catch，读不到返回 ""，异常返回
 * "&lt;err:...&gt;"，绝不抛出或阻塞（位置读取只取 getLastKnownLocation，不做任何 while 轮询）。
 */
public final class SpoofProbe {

    private SpoofProbe() {
    }

    public static JSONObject get(Context context) {
        JSONObject o = new JSONObject();
        // ---- Build 属性 ----
        JsonPut.put(o, "model", Build.MODEL);
        JsonPut.put(o, "brand", Build.BRAND);
        JsonPut.put(o, "manufacturer", Build.MANUFACTURER);
        JsonPut.put(o, "fingerprint", Build.FINGERPRINT);
        JsonPut.put(o, "buildSerialField", safe(() -> Build.SERIAL));

        // ---- 设备标识 ----
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        JsonPut.put(o, "androidId", safe(() ->
                Settings.Secure.getString(context.getContentResolver(), "android_id")));
        JsonPut.put(o, "imei_getImei", safe(() -> imeiGetImei(tm)));
        JsonPut.put(o, "imei_getDeviceId", safe(() -> deviceId(tm)));
        JsonPut.put(o, "meid_getMeid", safe(() -> meid(tm)));
        JsonPut.put(o, "imsi_getSubscriberId", safe(() -> tm == null ? "" : tm.getSubscriberId()));
        JsonPut.put(o, "phone_getLine1Number", safe(() -> tm == null ? "" : line1(tm)));
        JsonPut.put(o, "serial_BuildGetSerial", safe(SpoofProbe::buildGetSerial));
        JsonPut.put(o, "simSerial_getSimSerialNumber", safe(() -> tm == null ? "" : tm.getSimSerialNumber()));

        // ---- SIM ----
        JsonPut.put(o, "simOperator", safe(() -> tm == null ? "" : tm.getSimOperator()));
        JsonPut.put(o, "simOperatorName", safe(() -> tm == null ? "" : tm.getSimOperatorName()));
        JsonPut.put(o, "simCountryIso", safe(() -> tm == null ? "" : tm.getSimCountryIso()));

        // ---- 位置（AOSP LocationManager：单次 getLastKnownLocation，遍历 provider，不轮询/不阻塞） ----
        JsonPut.put(o, "location_LocationManager", location(context));
        // ---- 位置（GMS FusedLocationProviderClient：Google Maps 等真机 App 实际走的路径） ----
        JsonPut.put(o, "location_GmsFused", fusedLocation(context));
        return o;
    }

    /** GMS Fused 定位快照：getLastLocation() 带超时阻塞，读不到不阻塞主流程。 */
    @SuppressLint("MissingPermission")
    private static JSONObject fusedLocation(Context context) {
        JSONObject loc = new JSONObject();
        try {
            com.google.android.gms.location.FusedLocationProviderClient client =
                    com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context);
            android.location.Location l = com.google.android.gms.tasks.Tasks.await(
                    client.getLastLocation(), 4, java.util.concurrent.TimeUnit.SECONDS);
            if (l != null) {
                JsonPut.put(loc, "provider", l.getProvider());
                JsonPut.put(loc, "latitude", l.getLatitude());
                JsonPut.put(loc, "longitude", l.getLongitude());
                JsonPut.put(loc, "accuracy", l.getAccuracy());
            } else {
                JsonPut.put(loc, "latitude", "");
                JsonPut.put(loc, "longitude", "");
                JsonPut.put(loc, "note", "GMS getLastLocation returned null");
            }
        } catch (Throwable t) {
            JsonPut.put(loc, "error", t.getClass().getSimpleName() + ":" + t.getMessage());
        }
        return loc;
    }

    // ---- 位置：单次快照，最多遍历所有 provider 取一个非空 lastKnownLocation ----
    @SuppressLint("MissingPermission")
    private static JSONObject location(Context context) {
        JSONObject loc = new JSONObject();
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location best = null;
            String from = "";
            List<String> providers = lm.getProviders(true);
            for (String p : providers) {
                try {
                    Location l = lm.getLastKnownLocation(p);
                    if (l != null) {
                        best = l;
                        from = p;
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (best != null) {
                JsonPut.put(loc, "provider", from);
                JsonPut.put(loc, "latitude", best.getLatitude());
                JsonPut.put(loc, "longitude", best.getLongitude());
                JsonPut.put(loc, "altitude", best.getAltitude());
                JsonPut.put(loc, "accuracy", best.getAccuracy());
            } else {
                JsonPut.put(loc, "latitude", "");
                JsonPut.put(loc, "longitude", "");
                JsonPut.put(loc, "note", "no lastKnownLocation from providers " + providers);
            }
        } catch (Throwable t) {
            JsonPut.put(loc, "error", String.valueOf(t));
        }
        return loc;
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private static String imeiGetImei(TelephonyManager tm) {
        if (tm == null) return "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return tm.getImei();
        return "";
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private static String deviceId(TelephonyManager tm) {
        return tm == null ? "" : tm.getDeviceId();
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private static String meid(TelephonyManager tm) {
        if (tm == null) return "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return tm.getMeid();
        return "";
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private static String line1(TelephonyManager tm) {
        return tm.getLine1Number();
    }

    @SuppressLint("HardwareIds")
    private static String buildGetSerial() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return Build.getSerial();
        return Build.SERIAL;
    }

    // ---- 工具：把可能抛异常的读取包成字符串结果 ----
    private interface Reader {
        String read() throws Throwable;
    }

    private static String safe(Reader r) {
        try {
            String v = r.read();
            return v == null ? "" : v;
        } catch (Throwable t) {
            return "<err:" + t.getClass().getSimpleName() + ">";
        }
    }
}
