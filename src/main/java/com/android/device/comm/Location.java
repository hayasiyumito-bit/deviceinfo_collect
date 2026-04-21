package com.android.device.comm;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import com.android.UApplication;
import com.android.utils.ULog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.TimeZone;

import android.os.SystemClock;
import android.provider.Settings;

public class Location {

    private static android.location.Location bestLocation;


    /**
     * 获取设备位置信息
     *
     * @param context 上下文环境
     * @return 包含设备位置信息的 JSONObject 对象
     */
    public static JSONObject getLocationInfo(Context context) {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("gps", checkGPSIsOpen(context));
            jsonObject.put("location", getLocation(context));
            jsonObject.put("country", Locale.getDefault().getCountry());
            jsonObject.put("displayCountry", Locale.getDefault().getDisplayCountry());
            jsonObject.put("language", Locale.getDefault().getLanguage());
            jsonObject.put("displayLanguage", Locale.getDefault().getDisplayLanguage());
            jsonObject.put("ISO3Language", Locale.getDefault().getISO3Language());
            jsonObject.put("timezone-DisplayName", TimeZone.getDefault().getDisplayName());
            jsonObject.put("timezone-ID", TimeZone.getDefault().getID());
            long currentTimeMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime();
            jsonObject.put("currentTimeMillis", System.currentTimeMillis());
            jsonObject.put("uptimeMillis", SystemClock.uptimeMillis());
            jsonObject.put("elapsedRealtime", SystemClock.elapsedRealtime());
            jsonObject.put("bootTime", Long.valueOf(currentTimeMillis));
            jsonObject.put("boot_count", getBootCount(context));
            return jsonObject;
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取设备当前的位置信息，并以JSONObject的形式返回。
     *
     * @param context 应用的上下文
     * @return 包含纬度和经度的JSONObject，如果获取失败，则返回null
     */
    private static JSONObject getLocation(Context context) {

        try {
            JSONObject jsonObject = new JSONObject();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_COARSE);//低精度，如果设置为高精度，依然获取不了location。
                    criteria.setAltitudeRequired(false);//不要求海拔
                    criteria.setBearingRequired(false);//不要求方位
                    criteria.setCostAllowed(true);// 允许有花费
                    criteria.setPowerRequirement(Criteria.POWER_LOW);//低功耗
                    String locationProvider = locationManager.getBestProvider(criteria, true);
                    bestLocation = locationManager.getLastKnownLocation(locationProvider);
                    while (bestLocation == null) {
                        locationManager.requestLocationUpdates(locationProvider, 0, 0, new LocationListener() {
                            @Override
                            public void onLocationChanged(android.location.Location location) {
                                bestLocation = location;
                                locationManager.removeUpdates(this);
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {

                            }

                            @Override
                            public void onProviderEnabled(String provider) {

                            }

                            @Override
                            public void onProviderDisabled(String provider) {

                            }
                        });
                    }

                    if (bestLocation != null) {
                        try {
                            jsonObject.put("latitude", bestLocation.getLatitude());
                            jsonObject.put("longitude", bestLocation.getLongitude());
                        } catch (JSONException e) {
                            // 删除ULog，静默处理异常
                        }
                    }


                }
            }
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
        }


//        List providers = locationManager.getProviders(true);
//        String locationProvider = "";
//        if (providers.contains(LocationManager.GPS_PROVIDER)) {// 如果是GPS
//            locationProvider = LocationManager.GPS_PROVIDER;
//        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {// 如果是Network
//            locationProvider = LocationManager.NETWORK_PROVIDER;
//        } else {
//            Toast.makeText(UApplication.getContext(), "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
//            return jsonObject;
//        }

//        String locationProvider = locationManager.getBestProvider(criteria, true);
//        if (locationProvider == null) {
//            Toast.makeText(UApplication.getContext(), "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
//            return jsonObject;
//        }
        return null;
    }

    /**
     * 检查是否打开手机的gps
     *
     * @param context
     * @return
     */
    public static boolean checkGPSIsOpen(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    public static int getBootCount(Context context) {
//        XposedHelpers.findAndHookMethod("android.provider.Settings$Global", classLoader, "getString",android.content.ContentResolver.class, String.class, new XC_MethodHook() {
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                String settingName = (String) param.args[1];
//                if ("boot_count".equals(settingName)) {
//                    // 修改返回值
//                    param.setResult(PhoneBootCount);
//                }
//            }
//        });
        try {
            return Settings.Global.getInt(context.getContentResolver(), "boot_count");
        } catch (Settings.SettingNotFoundException e) {
//            throw new RuntimeException(e);
            return -1;
        }
    }

}