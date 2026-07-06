package com.android.device.appsflyer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.hardware.Sensor;
import android.net.Uri;
import android.os.Build;

import com.android.utils.Cmd;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.hardware.SensorManager;

import com.android.device.comm.SimCard;
import com.android.device.hardware.Battery;
import com.android.device.hardware.Storage;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import android.view.WindowManager;

public class AppsflyerInfo {
    /**
     * 获取Google Play服务的版本名称。
     *
     * @param context 上下文对象
     * @return Google Play服务的版本名称，如果未找到则返回空字符串
     * @throws SecurityException 如果调用方没有访问权限
     */
    private static String get_gp_api_ver_name(Context context) {
        try {
            return context.getPackageManager().getPackageInfo("com.android.vending", 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /**
     * 获取Google Play服务的API版本号
     *
     * @param context 上下文对象
     * @return 返回Google Play服务的API版本号，如果获取失败则返回0
     */
    private static long get_gp_api_ver(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo("com.android.vending", 0);
            if (Build.VERSION.SDK_INT >= 28) {
                return packageInfo.getLongVersionCode();
            }
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0L;
        }
    }

    /**
     * 获取启动当前应用的来源信息
     *
     * @param context 上下文环境
     * @return 返回启动当前应用的来源信息，如果无法获取则返回空字符串
     */
    private static String get_open_referrer(Context context) {
        Uri uri;
        Activity activity = (Activity) context;
        if (activity == null || activity.getIntent() == null) {
            uri = null;
        } else if (Build.VERSION.SDK_INT >= 22) {
            uri = activity.getReferrer();
            String obj = uri != null ? uri.toString() : null;
            return obj == null ? "" : obj;
        }
        return "";
    }

    /**
     * 获取启动该应用的包名。
     *
     * @param context 上下文对象
     * @return 启动该应用的包名，若无法获取则返回空字符串
     * @throws PackageManager.NameNotFoundException 若包名无法找到，则抛出此异常
     */
    private static String get_initiating_package(Context context) throws PackageManager.NameNotFoundException {
        InstallSourceInfo installSourceInfo;
        PackageManager packageManager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (packageManager != null && (installSourceInfo = packageManager.getInstallSourceInfo(context.getPackageName())) != null) {
                return installSourceInfo.getInitiatingPackageName();
            }
        }
        return "";
    }

    /**
     * 获取安装包的名称。
     *
     * @param context 上下文对象
     * @return 安装包的名称，如果无法获取则返回空字符串
     * @throws PackageManager.NameNotFoundException 如果包名不存在则抛出此异常
     */
    private static String get_installing_package(Context context) throws PackageManager.NameNotFoundException {
        InstallSourceInfo installSourceInfo;
        PackageManager packageManager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (packageManager != null && (installSourceInfo = packageManager.getInstallSourceInfo(context.getPackageName())) != null) {
                return installSourceInfo.getInstallingPackageName();
            }
        }
        return "";
    }

    /**
     * 获取应用安装源包名
     *
     * @param context 上下文对象
     * @return 安装源包名，若无法获取则返回空字符串
     * @throws PackageManager.NameNotFoundException 若包名不存在则抛出此异常
     */
    private static String get_originating_package(Context context) throws PackageManager.NameNotFoundException {
        InstallSourceInfo installSourceInfo;
        PackageManager packageManager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (packageManager != null && (installSourceInfo = packageManager.getInstallSourceInfo(context.getPackageName())) != null) {
                return installSourceInfo.getOriginatingPackageName();
            }
        }
        return "";
    }

    /**
     * 获取Google Play商店的相关信息
     *
     * @param context 上下文对象
     * @return 包含Google Play商店信息的JSONObject对象，若获取失败则返回空JSONObject对象
     */
    public static JSONObject getGooglePlayInfo(Context context) {
        try {
            JSONObject jsonObject = new JSONObject();
            android.content.pm.PackageInfo packageInfo = context.getPackageManager().getPackageInfo("com.android.vending", 0);
            if (packageInfo == null) return null;
            if (Build.VERSION.SDK_INT >= 28) {
                jsonObject.put("googlePlay_versionCode", packageInfo.getLongVersionCode());
            } else {
                jsonObject.put("googlePlay_versionCode", packageInfo.versionCode);
            }
            jsonObject.put("googlePlay_versionName", packageInfo.versionName);
            jsonObject.put("googlePlay_firstInstallTime", packageInfo.firstInstallTime);
            jsonObject.put("googlePlay_lastUpdateTime", packageInfo.lastUpdateTime);
            jsonObject.put("googlePlay_InstallerPackageName", context.getPackageManager().getInstallerPackageName("com.android.vending"));
            Signature[] signatureArr = context.getPackageManager().getPackageInfo("com.android.vending", 64).signatures;
            if (signatureArr == null) {
                return null;
            }
            X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(signatureArr[0].toByteArray()));
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            messageDigest.update(x509Certificate.getEncoded());
            jsonObject.put("googlePlay_signature", String.format("%032X", new BigInteger(1, messageDigest.digest())));
            return jsonObject;
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取安装包的安装程序包名
     *
     * @param context 上下文对象
     * @return 安装包的安装程序包名，如果获取失败则返回空字符串
     */
    private static String getInstallerPackageName(Context context) {
        try {
            return context.getPackageManager().getInstallerPackageName(context.getPackageName());
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * 获取启动器包的名称
     *
     * @param context 上下文对象
     * @return 返回启动器包的名称，如果无法获取则返回空字符串
     */
    public static String getLauncherPackageName(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Uri uri = ((Activity) context).getReferrer();
                if (uri != null) {
                    return uri.toString().split("android-app://")[1];
                }
/*                else {
                    PackageManager localPackageManager = context.getPackageManager();
                    Intent intent = new Intent("android.intent.action.MAIN");
                    intent.addCategory(Intent.CATEGORY_HOME);
                    return localPackageManager
                            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                            .activityInfo
                            .packageName;
                }*/
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /**
     * 获取当前设备上已安装的应用市场列表
     *
     * @param context 上下文对象
     * @return 返回包含应用市场包名的列表
     */
    private static List<String> getAppMarketList(Context context) {
        List<String> appMarketList = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_MARKET);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        for (ResolveInfo resolveInfo : resolveInfos) {
            appMarketList.add(resolveInfo.activityInfo.packageName);
        }
        return appMarketList;
    }

    /**
     * 根据传入的字符串获取Android系统属性
     *
     * @param str 需要查询的系统属性名
     * @return 查询到的系统属性值，如果查询失败或属性值为null，则返回null
     */
    public static String AFInAppEventType(String str) {
        String value = Cmd.getPropertyViaShell(str);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = Cmd.getPropertyViaJavaApi(str);
        return (value != null && !value.isEmpty()) ? value : null;
    }

    /**
     * 获取设备的屏幕旋转状态
     *
     * @param context 上下文对象
     * @return 屏幕旋转状态，返回值为"p"（竖屏）、"l"（横屏左侧）、"pr"（竖屏反向）、"lr"（横屏右侧）或null（获取失败）
     */
    public static String getRotationState(Context context) {
        String str = new String();
        WindowManager windowManager;
        Object systemService = context.getSystemService(Context.WINDOW_SERVICE);
        switch (systemService instanceof WindowManager ? 'Q' : (char) 28) {
            case 28:
                windowManager = null;
                break;
            default:
                windowManager = (WindowManager) systemService;
                break;
        }
        switch (windowManager == null ? 'Y' : '5') {
            case 'Y':
                return null;
            default:
                switch (windowManager.getDefaultDisplay().getRotation()) {
                    case 0:
                        int i3 = 59;
                        switch (i3 % 2 == 0 ? ':' : ';') {
                            case ':':
                            default:
                                str = "p";
                                break;
                        }
                        break;
                    case 1:
                        str = "l";
                        break;
                    case 2:
                        str = "pr";
                        break;
                    case 3:
                        str = "lr";
                        break;
                }
                return str;
        }
    }

    /**
     * 获取应用程序信息并返回为JSON格式。
     *
     * @param context 上下文对象
     * @return 包含应用程序信息的JSONObject对象，如果发生异常则返回null
     * @throws Exception 如果发生异常，则抛出Exception
     */
    private static JSONObject getAppInfo(Context context) {
        try {
            JSONObject object = new JSONObject();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= 28) {
                object.put("app_version_code", String.valueOf(packageInfo.getLongVersionCode()));
            } else {
                object.put("app_version_code", String.valueOf(packageInfo.versionCode));
            }
            object.put("app_package_name", context.getPackageName());
            object.put("app_version_name", packageInfo.versionName);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmssZ", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            object.put("date1", sdf.format(new Date(packageInfo.firstInstallTime)));
            object.put("date2", sdf.format(new Date(packageInfo.lastUpdateTime)));
            object.put("targetSDKver", Integer.valueOf(context.getApplicationInfo().targetSdkVersion));
            return object;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取Appsflyer相关信息
     *
     * @param context 上下文对象
     * @return 包含Appsflyer相关信息的JSONObject对象，如果发生异常则返回null
     */
    public static JSONObject getAppsflyerInfo(Context context) {
        try {
            JSONObject object = new JSONObject();
            object.put("app_info", getAppInfo(context));
            object.put("simInfo", SimCard.getSimCardInfo(context));
            object.put("model", Build.MODEL);
            object.put("brand", Build.BRAND);
            object.put("device", Build.DEVICE);
            object.put("product", Build.PRODUCT);
            object.put("sdk", String.valueOf(Build.VERSION.SDK_INT));
//            object.put("deviceType", Build.TYPE);
//            object.put("api_ver", get_gp_api_ver(context));
//            object.put("api_ver_name", get_gp_api_ver_name(context));
            //object.put("open_referrer", get_open_referrer(context));
            object.put("initiating_package", get_initiating_package(context));
            object.put("installing_package", get_installing_package(context));
            object.put("originating_package", get_originating_package(context));
            object.put("GooglePlayInfo", getGooglePlayInfo(context));
            object.put("installer_package", getInstallerPackageName(context));
            object.put("LauncherPackageName", getLauncherPackageName(context));
//            object.put("gp_referrer", Gpreferrer.getGpReferrer(context));
//            object.put("AppMarketList", getAppMarketList(context));
            object.put("cpu_abi", AFInAppEventType("ro.product.cpu.abi"));
            object.put("cpu_abi2", AFInAppEventType("ro.product.cpu.abi2"));
            object.put("arch", AFInAppEventType("os.arch"));//都是空
            object.put("build_display_id", AFInAppEventType("ro.build.display.id"));
            object.put("aa", AFInAppEventType("ro.arch"));
            object.put("ab", AFInAppEventType("ro.chipname"));//少数几个机型有值 MSM8976 exynos7870 MT6757 MSM8917其他为空
            object.put("ac", AFInAppEventType("ro.dalvik.vm.native.bridge"));//0
            object.put("ad", AFInAppEventType("persist.sys.nativebridge"));//空
            object.put("ae", AFInAppEventType("ro.enable.native.bridge.exec"));//空
            object.put("af", AFInAppEventType("dalvik.vm.isa.x86.features"));//空
            object.put("ag", AFInAppEventType("dalvik.vm.isa.x86.variant"));//空
            object.put("ah", AFInAppEventType("ro.zygote"));//zygote64_32、zygote32、zygote64、空
            object.put("ai", AFInAppEventType("ro.allow.mock.location"));//0
            object.put("aj", AFInAppEventType("ro.dalvik.vm.isa.arm"));//空
            object.put("ak", AFInAppEventType("dalvik.vm.isa.arm.features"));
            object.put("al", AFInAppEventType("dalvik.vm.isa.arm.variant"));
            object.put("am", AFInAppEventType("dalvik.vm.isa.arm64.features"));
            object.put("an", AFInAppEventType("dalvik.vm.isa.arm64.variant"));
            object.put("ao", AFInAppEventType("vzw.os.rooted"));//空或者false SM-J410F SM-A910F SM-N960N SC-02H SM-C710F SM-J415F
            object.put("ap", AFInAppEventType("ro.build.user"));
            object.put("aq", AFInAppEventType("ro.kernel.qemu"));// 空或者0
            object.put("ar", AFInAppEventType("ro.hardware"));
            object.put("as", AFInAppEventType("ro.product.cpu.abi"));
            object.put("at", AFInAppEventType("ro.product.cpu.abilist"));
            object.put("au", AFInAppEventType("ro.product.cpu.abilist32"));
            object.put("av", AFInAppEventType("ro.product.cpu.abilist64"));
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            int sensorSize = sm.getSensorList(Sensor.TYPE_ALL).size();
            object.put("sensor_num", sensorSize);

//            object.put("sensors", com.android.device.hardware.Sensor.getAfSensorInfo(context));
            object.put("af_battery", Battery.getAfBatteryInfo(context));//温度290-420大概率区间
            object.put("disk", Storage.getDiskSize2AF());
            object.put("country", Locale.getDefault().getCountry());
            object.put("lang_code", Locale.getDefault().getLanguage());
            object.put("lang", Locale.getDefault().getDisplayLanguage());
//            object.put("last_boot_time", String.valueOf(System.currentTimeMillis() - SystemClock.elapsedRealtime()));
            object.put("sc_o", getRotationState(context));//是否收集翻转,都是p？
            object.put("dim", com.android.device.software.Screen.getScreenDetailInfo(context));

            return object;
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return null;
    }
}
