package com.android.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppUtils {
    /**
     * 获取应用程序信息
     *
     * @param context 上下文
     * @return 应用程序信息列表
     */
    public static JSONObject getAppsInfoJson(Context context) {
        JSONObject jsonObject = new JSONObject();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) != PackageManager.PERMISSION_GRANTED) {
                return jsonObject;
            }
        }
        try {
            JSONArray sys_apps = new JSONArray();
            JSONArray t_p_apps = new JSONArray();
            for (AppInfo appInfo : getInstalledSystemApps(context)) {
                sys_apps.put(appInfo);
            }
            for (AppInfo appInfo : getInstalledApps(context)) {
                t_p_apps.put(appInfo);
            }

            jsonObject.put("systemApps", sys_apps);
            jsonObject.put("thirdPartyApps", t_p_apps);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject;
    }

    /**
     * 获取应用程序信息
     *
     * @param context 上下文
     * @return 应用程序信息列表
     */
    public static JSONArray getAppstempInfoJson(Context context) {
        JSONArray t_p_apps = new JSONArray();
        for (AppInfo appInfo : getInstalledApps(context)) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("appName", appInfo.appName);
                jsonObject.put("packageName", appInfo.packageName);
                jsonObject.put("versionCode", appInfo.versionCode);
                jsonObject.put("versionName", appInfo.versionName);
                jsonObject.put("isSystemApp", false);
                jsonObject.put("firstInstallTime", appInfo.firstInstallTime);
                jsonObject.put("lastUpdateTime", appInfo.lastUpdateTime);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            t_p_apps.put(jsonObject);
        }
        return t_p_apps;
    }

    /**
     * 获取已安装应用程序的列表。
     *
     * @param context Android 上下文。
     * @return 包含已安装应用程序信息的 AppInfo 列表。  该列表已按应用程序名称排序。
     * 如果出现错误或没有找到应用程序，则返回一个空的列表（非null）。
     */
    public static List<AppInfo> getInstalledApps(Context context) {
        if (context == null) {
            return Collections.emptyList(); // 避免空指针异常
        }

        PackageManager packageManager = context.getPackageManager();
        List<AppInfo> appList = new ArrayList<>();

        try {
            List<PackageInfo> packages = packageManager.getInstalledPackages(0);  // 获取所有已安装的包
            for (PackageInfo packageInfo : packages) {
                //过滤掉系统应用 (可选)
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    continue;
                }

                AppInfo appInfo = new AppInfo();
                appInfo.appName = packageInfo.applicationInfo.loadLabel(packageManager).toString();
                appInfo.packageName = packageInfo.packageName;
                appInfo.versionName = packageInfo.versionName;
                appInfo.versionCode = packageInfo.versionCode;
                appInfo.firstInstallTime = packageInfo.firstInstallTime;
                appInfo.lastUpdateTime = packageInfo.lastUpdateTime;
                // 获取图标，处理可能的异常
                try {
                    appInfo.icon = packageInfo.applicationInfo.loadIcon(packageManager);
                } catch (OutOfMemoryError e) {
                    // 处理图标加载内存溢出, 可以设置默认图标
                    // appInfo.icon = context.getResources().getDrawable(R.drawable.default_icon); //需要自己添加default_icon
                    e.printStackTrace();
                }

                // 获取启动的 Intent (可选，用于启动应用)
                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageInfo.packageName);
                if (launchIntent != null) {
                    appInfo.launchIntent = launchIntent;
                }

                appList.add(appInfo);
            }

            // 按应用名称排序
            Collections.sort(appList, (a1, a2) -> a1.appName.compareToIgnoreCase(a2.appName));

        } catch (Exception e) {
            e.printStackTrace(); // 记录异常信息
            // 可以在这里添加用户友好的错误处理，例如 Toast 提示
        }

        return appList;
    }

    /**
     * 获取已安装的系统应用列表
     *
     * @param context
     * @return
     */
    public static List<AppInfo> getInstalledSystemApps(Context context) {
        if (context == null) return Collections.emptyList();
        PackageManager pm = context.getPackageManager();
        List<AppInfo> appList = new ArrayList<>();
        try {
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo app : apps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    AppInfo appInfo = new AppInfo();
                    appInfo.appName = app.loadLabel(pm).toString();
                    appInfo.packageName = app.packageName;
                    // 可以获取其他信息，例如版本号，但需要从 PackageManager 中单独获取 PackageInfo
                    try {
                        PackageInfo packageInfo = pm.getPackageInfo(app.packageName, 0);
                        appInfo.versionName = packageInfo.versionName;
                        appInfo.versionCode = packageInfo.versionCode;
                        appInfo.firstInstallTime = packageInfo.firstInstallTime;
                        appInfo.lastUpdateTime = packageInfo.lastUpdateTime;
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.d("Apputiles", app.packageName + "|NameNotFoundException");
                    }

                    try {
                        appInfo.icon = app.loadIcon(pm);
                    } catch (OutOfMemoryError e) {
                    }
                    appList.add(appInfo);
                }
            }
            Collections.sort(appList, (a1, a2) -> a1.appName.compareToIgnoreCase(a2.appName));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appList;
    }

    /**
     * 将 PackageInfo 序列化到文件
     *
     * @param packageInfo 要序列化的 PackageInfo 对象
     * @param filePath    文件路径
     * @return true 如果成功，false 如果失败
     */
    public static boolean serializePackageInfo(PackageInfo packageInfo, String filePath) {
        Parcel parcel = Parcel.obtain();
        try {
            packageInfo.writeToParcel(parcel, 0);
            byte[] data = parcel.marshall();
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(data);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } finally {
            parcel.recycle();
        }
    }

    /**
     * 将多个 PackageInfo 序列化到文件
     *
     * @param packageInfos 要序列化的 PackageInfo 对象列表
     * @param filePath     文件路径
     * @return true 如果成功，false 如果失败
     */
    public static boolean serializePackageInfos(List<PackageInfo> packageInfos, String filePath) {

        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Parcel parcel = Parcel.obtain();
        try {
            for (PackageInfo packageInfo : packageInfos) {
                packageInfo.writeToParcel(parcel, 0);
            }
            byte[] data = parcel.marshall();
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(data);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } finally {
            parcel.recycle();
        }
    }

    /**
     * 获取系统所有已安装的应用并序列化到文件
     *
     * @param context Android 上下文
     * @return true 如果成功，false 如果失败
     */
    public static boolean getAllInstalledAppsAndSerialize(Context context) {
        File filesDir = context.getApplicationContext().getExternalFilesDir(null);
        Log.d("AppUtils", "Files dir: " + filesDir);
        if (context == null) {
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos;

        try {
            packageInfos = packageManager.getInstalledPackages(0);  // 获取所有已安装的包
            String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "Android" + File.separator + "data" + File.separator + "com.permissionx.app" + File.separator + "packageInfo";
            return serializePackageInfos(packageInfos, filePath);
        } catch (Exception e) {
            e.printStackTrace(); // 记录异常信息
            return false;
        }
    }

    /**
     * 反序列化文件中的 PackageInfo 列表并打印日志
     *
     * @param filePath 文件路径
     * @return true 如果成功，false 如果失败
     */
    public static boolean deserializePackageInfosAndLog(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);

            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);

            List<PackageInfo> packageInfos = new ArrayList<>();
            while (parcel.dataAvail() > 0) {
                PackageInfo packageInfo = PackageInfo.CREATOR.createFromParcel(parcel);
                packageInfos.add(packageInfo);
            }

            for (PackageInfo packageInfo : packageInfos) {
                Log.d("deserialize_tt", "PackageInfo: " + packageInfo.packageName + ", Version: " + packageInfo.versionName);
            }

            parcel.recycle();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 应用程序信息类。
     */
    public static class AppInfo {
        public String appName;
        public String packageName;
        public String versionName;
        public long firstInstallTime;
        public long lastUpdateTime;

        public int versionCode;
        public Drawable icon;
        public Intent launchIntent; // 可选：启动 Intent

        @Override
        public String toString() {
            return "AppInfo{" +
                    "appName='" + appName + '\'' +
                    ", packageName='" + packageName + '\'' +
                    ", versionName='" + versionName + '\'' +
                    ", firstInstallTime=" + firstInstallTime +
                    ", lastUpdateTime=" + lastUpdateTime +
                    ", versionCode=" + versionCode +
                    '}';
        }
    }

    /**
     * 判断应用是否安装
     *
     * @param context
     * @param packageName
     * @return true:已安装 false:未安装
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) return false;
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 获取Android系统编码器和解码器的信息，并打印详细日志。
     */
    public static void getMediaCodecList() {
        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            String codecName = codecInfo.getName();
            boolean isEncoder = codecInfo.isEncoder();
            String[] supportedTypes = codecInfo.getSupportedTypes();

            Log.d("MediaCodecInfo", "Codec Name: " + codecName);
            Log.d("MediaCodecInfo", "Is Encoder: " + isEncoder);
            Log.d("MediaCodecInfo", "Supported Types: " + Arrays.toString(supportedTypes));

            for (String type : supportedTypes) {
                MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(type);
                Log.d("MediaCodecInfo", "Capabilities for type " + type + ": " + capabilities);
            }
        }
    }
}
