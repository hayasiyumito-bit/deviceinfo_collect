package com.android.device.snapshot;

import android.content.Context;
import android.media.RingtoneManager;
import android.provider.Settings;
import android.util.Log;

import com.android.device.appsflyer.AppsflyerInfo;
import com.android.device.assemble.CollectDeviceInfo;
import com.android.device.comm.Location;
import com.android.device.comm.Net;
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

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 将各域采集结果写入根 JSON；按「标识 / 输入与媒体 / 硬件 / 存储 / 网络 / 软件 / 系统杂项」分段，便于维护。
 */
final class SnapshotFields {

    private static final String TAG = "SnapshotFields";
    private static final String DATE_FORMAT = "yyyy-MM-dd_HHmmssZ";
    private static final String TIMEZONE_UTC = "UTC";

    private SnapshotFields() {
    }

    static void addTimestamp(JSONObject jsonObject) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone(TIMEZONE_UTC));
        jsonObject.put("time", sdf.format(new Date(System.currentTimeMillis())));
    }

    static void fillAll(Context context, JSONObject root) {
        try {
            putAppsAndIds(context, root);
            putInputAndMedia(context, root);
            putHardware(context, root);
            putStorage(context, root);
            putNetAndLocation(context, root);
            putSoftwareStack(context, root);
            putSystemMisc(context, root);
            putRingtone(context, root);
        } catch (Exception e) {
            Log.e(TAG, "Error during snapshot assembly", e);
        }
    }

    private static void putAppsAndIds(Context context, JSONObject root) {
        JsonPut.put(root, "appsflyerdebuginfo", AppsflyerInfo.getAppsflyerInfo(context));
        JsonPut.put(root, "ids", IDs.getIDsInfo(context));
        JsonPut.put(root, "build", com.android.device.software.Build.getBuildInfo());
        JsonPut.put(root, "deviceInfo", CollectDeviceInfo.getDeviceInfo());
    }

    private static void putInputAndMedia(Context context, JSONObject root) {
        JsonPut.put(root, "input", Input.getInputInfo(context));
        JsonPut.put(root, "library", Library.getLibraryInfo(context));
        JsonPut.put(root, "media", Media.getMediaInfo(context));
        JsonPut.put(root, "fonts", Fonts.getFonts(context));
        JsonPut.put(root, "systemFonts", Fonts.getSystemFonts());
        JsonPut.put(root, "InputLanguageList", InputLanguage.getInputLanguageList(context));
        JsonPut.put(root, "inputMethods", Settings.Secure.getString(context.getContentResolver(), "enabled_input_methods"));
    }

    private static void putHardware(Context context, JSONObject root) {
        JsonPut.put(root, "hardware", Hardware.getHardwareInfo(context));
        JsonPut.put(root, "batteryInfo", Battery.getBatteryInfo(context));
        JsonPut.put(root, "sensor", Sensor.getSensorInfo(context));
        JsonPut.put(root, "gpuInfo", Gpu.getGpuInfo(context));
        JsonPut.put(root, "inputDevices", InputDevices.getInputDevices(context));
        JsonPut.put(root, "usb", USB.getUsbInfo(context));
    }

    private static void putStorage(Context context, JSONObject root) {
        JsonPut.put(root, "storage", Storage.getStorageInfo(context));
        JsonPut.put(root, "memThreshold", Storage.getMemoryThreshold(context));
    }

    private static void putNetAndLocation(Context context, JSONObject root) {
        JsonPut.put(root, "net", Net.getNetInfo(context));
        JsonPut.put(root, "location", Location.getLocationInfo(context));
    }

    private static void putSoftwareStack(Context context, JSONObject root) {
        JsonPut.put(root, "packageInfo", PackageInfo.getInstallerInfo(context));
        JsonPut.put(root, "installedApps", AppUtils.getAppsInfoJson(context));
        JsonPut.put(root, "service_list", ServiceList.getServiceListInfo());
    }

    private static void putSystemMisc(Context context, JSONObject root) {
        JsonPut.put(root, "fileStat", FileStat.getFileStat());
        JsonPut.put(root, "uname", Cmd.exe("uname -a"));
    }

    private static void putRingtone(Context context, JSONObject root) {
        try {
            String ringTitle = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_RINGTONE_URI)
                    .getTitle(context);
            JsonPut.put(root, "ringTitle", ringTitle);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get ring title", e);
            JsonPut.put(root, "ringTitle", "Unknown");
        }
    }
}
