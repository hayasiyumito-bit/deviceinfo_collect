package com.android.device.ids;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaDrm;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.webkit.WebSettings;


import com.android.utils.Cmd;
import com.android.utils.MD5;
import com.android.utils.ULog;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class IDs {
    private static final String TAG = "IDsTAG";

    /**
     * 获取设备的IMEI号
     *
     * @param context 上下文对象
     * @return 设备的IMEI号，如果获取失败则返回空字符串
     */
    public static String getImei(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        return telephonyManager.getDeviceId();
                    }
                }
            }
        } catch (Exception e) {
            // 权限不足时返回空字符串
        }
        return "";
    }


    /**
     * 获取设备ID的方法
     *
     * @param context 上下文对象
     * @return 返回设备ID的字符串，如果无法获取则返回空字符串
     */
    public static String getDeviceIds(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    && Build.VERSION.SDK_INT < 29) {
                StringBuilder buffer = new StringBuilder();
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
                if (tm != null) {
                    if (!TextUtils.isEmpty(tm.getDeviceSoftwareVersion())) {
                        buffer.append(tm.getDeviceSoftwareVersion());
                    }
                    int count = tm.getPhoneCount();
                    for (int i = 0; i < count; i++) {
                        if (buffer.length() > 0) {
                            buffer.append("*");
                        }
                        buffer.append(tm.getDeviceId(i));
                    }
                }
                return buffer.toString();
            }
        } catch (Exception e) {
            // 权限不足时返回空字符串
        }
        return "";
    }

    /**
     * MEID for GSM phones
     */
    /**
     * 获取设备的IMEI码
     *
     * @param context 上下文对象
     * @return 返回设备的IMEI码，如果没有获取到则返回空字符串
     */
    public static String getIMEIs(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    && Build.VERSION.SDK_INT < 29) {
                StringBuffer buffer = new StringBuffer();
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
                if (tm != null) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        int count = tm.getPhoneCount();
                        for (int i = 0; i < count; i++) {
                            if (i != 0) {
                                buffer.append("*");
                            }
                            String imei = tm.getImei(i);
                            if (!TextUtils.isEmpty(imei)) {
                                buffer.append(imei);
                            }
                        }
                        return buffer.toString();
                    }
                }
            }
        } catch (Exception e) {
            // 权限不足时返回空字符串
        }
        return "";
    }

    /**
     * MEID for CDMA phones
     */
    /**
     * 获取设备的MEID信息
     *
     * @param context 上下文对象
     * @return 设备MEID信息的字符串表示，若无法获取则返回空字符串
     */
    public static String getMEIDs(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    && Build.VERSION.SDK_INT < 29) {
                StringBuffer buffer = new StringBuffer();
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
                if (tm != null) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        int count = tm.getPhoneCount();
                        for (int i = 0; i < count; i++) {
                            if (i != 0) {
                                buffer.append("*");
                            }
                            String meid = tm.getMeid(i);
                            if (!TextUtils.isEmpty(meid)) {
                                buffer.append(meid);
                            }
                        }
                        return buffer.toString();
                    }
                }
            }
        } catch (Exception e) {
            // 权限不足时返回空字符串
        }
        return "";
    }

    /**
     * International Mobile Subscriber Identity
     */
    /**
     * 获取国际移动用户识别码（IMSI）
     *
     * @param context 上下文对象
     * @return IMSI码，如果无法获取则返回空字符串
     */
    public static String getIMSI(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    && Build.VERSION.SDK_INT < 29) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    return telephonyManager.getSubscriberId();
                }
            }
        } catch (Exception e) {
            // 权限不足时返回空字符串
        }
        return "";
    }

    /**
     * 获取设备的序列号
     *
     * @param context 上下文对象
     * @return 返回设备的序列号，如果获取失败则返回空字符串
     */
    public static String getSerialNo(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    && Build.VERSION.SDK_INT < 29) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return Build.getSerial();
                }
                return Build.SERIAL;
            }
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return "";
    }

    /**
     * 获取Android设备的Android ID。
     *
     * @param context 应用上下文
     * @return Android ID字符串，如果获取失败或ID为空则返回空字符串
     */
    public static String getAndroidId(Context context) {
        try {
            @SuppressLint("HardwareIds")
            String adid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (!TextUtils.isEmpty(adid)) {
                return adid;
            }
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return "";
    }

    /**
     * 获取SIM卡的序列号
     *
     * @param context 上下文对象
     * @return 如果获取成功，返回SIM卡的序列号；如果获取失败或没有权限，返回空字符串
     */
    public static String getSimSerialNumber(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    return telephonyManager.getSimSerialNumber();
                }
            }
        } catch (Exception e) {
            // 权限不足时返回空字符串
        }
        return "";
    }

    /**
     * 获取设备的电话号码
     *
     * @param context 上下文对象
     * @return 返回设备的电话号码，如果没有权限或出现异常，则返回空字符串
     */
    public static String getPhoneNumber(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                return manager.getLine1Number();
            }
        } catch (Exception e) {
            // 权限不足时返回空字符串
        }
        return "";
    }

    /**
     * 获取用户代理字符串
     *
     * @param context 上下文对象
     * @return 返回用户代理字符串
     */
    public static String getUserAgent(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                return WebSettings.getDefaultUserAgent(context);
            }
            Locale locale = Locale.getDefault();
            StringBuilder buffer = new StringBuilder();
            final String version = Build.VERSION.RELEASE;
            if (version.length() > 0) {
                buffer.append(version);
            } else {
                buffer.append("1.0");
            }
            buffer.append("; ");
            final String language = locale.getLanguage();
            if (language != null) {
                buffer.append(language.toLowerCase());
                final String country = locale.getCountry();
                if (country != null) {
                    buffer.append("-");
                    buffer.append(country.toLowerCase());
                }
            } else {
                buffer.append("en");
            }
            // add the model for the release build
            if ("REL".equals(Build.VERSION.CODENAME)) {
                final String model = Build.MODEL;
                if (model.length() > 0) {
                    buffer.append("; ");
                    buffer.append(model);
                }
            }
            final String id = Build.ID;
            if (id.length() > 0) {
                buffer.append(" Build/");
                buffer.append(id);
            }
            Resources resources = context.getResources();
            int uaid = resources.getIdentifier("android:string/web_user_agent", "string", "android");
            final String base = resources.getText(uaid).toString();
            return String.format(base, buffer, "Mobile ");
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return "";
    }

    /**
     * 获取Google广告ID
     *
     * @param context 上下文对象
     * @return Google广告ID，若获取失败则返回null
     */
    public static String getGoogleADID(Context context) {
        try {
            return AdvertisingIdClient.getGoogleAdId(context);
        } catch (Exception e) {
//            throw new RuntimeException(e);
            return "";
        }
    }

    /**
     * 获取DRM的唯一标识符。
     *
     * @return 返回DRM的唯一标识符，如果获取失败则返回空字符串。
     */
    public static String getDrmId() {
        String res = "";
        try {
            UUID wideVineUuid = new UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L);
            MediaDrm wvDrm = new MediaDrm(wideVineUuid);
            byte[] wideVineId = wvDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID);
//            Arrays.toString(wideVineId)
//            return android.util.Base64.encodeToString(wideVineId, Base64.NO_WRAP);
            res = MD5.stringToMD5(new String(wideVineId));
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return res;
    }

    static class AdvertisingIdClient {
        /**
         * 这个方法是耗时的，不能在主线程调用
         */
        public static String getGoogleAdId(Context context) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                return "Cannot call in the main thread, You must call in the other thread";
            }
            try {
                PackageManager pm = context.getPackageManager();
                pm.getPackageInfo("com.android.vending", 0);
                AdvertisingConnection connection = new AdvertisingConnection();
                Intent intent = new Intent(
                        "com.google.android.gms.ads.identifier.service.START");
                intent.setPackage("com.google.android.gms");
                if (context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                    try {
                        AdvertisingInterface adInterface = new AdvertisingIdClient.AdvertisingInterface(
                                connection.getBinder());
                        return adInterface.getId();
                    } finally {
                        context.unbindService(connection);
                    }
                }
            } catch (Exception e) {
//                ULog.e(e);
            }
            return "";
        }

        private static final class AdvertisingConnection implements ServiceConnection {
            boolean retrieved = false;
            private final LinkedBlockingQueue<IBinder> queue = new LinkedBlockingQueue<>(1);

            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    this.queue.put(service);
                } catch (InterruptedException localInterruptedException) {
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }

            public IBinder getBinder() throws InterruptedException {
                if (this.retrieved)
                    throw new IllegalStateException();
                this.retrieved = true;
                return this.queue.take();
            }
        }

        private static final class AdvertisingInterface implements IInterface {
            private IBinder binder;

            public AdvertisingInterface(IBinder pBinder) {
                binder = pBinder;
            }

            public IBinder asBinder() {
                return binder;
            }

            public String getId() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                String id;
                try {
                    data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                    binder.transact(1, data, reply, 0);
                    reply.readException();
                    id = reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return id;
            }

            public boolean isLimitAdTrackingEnabled(boolean paramBoolean)
                    throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                boolean limitAdTracking;
                try {
                    data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                    data.writeInt(paramBoolean ? 1 : 0);
                    binder.transact(2, data, reply, 0);
                    reply.readException();
                    limitAdTracking = 0 != reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return limitAdTracking;
            }
        }
    }

    /**
     * 获取Android系统属性
     *
     * @param str 需要获取的属性名称
     * @return 获取到的属性值，如果获取失败则返回"Access denied finding property 属性名称"
     * @throws NullPointerException 如果获取到的属性值为null，则抛出空指针异常
     */
    public static String AFInAppEventType(String str) {
        String value = Cmd.getPropertyViaShell(str);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = Cmd.getPropertyViaJavaApi(str);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return "Access denied finding property " + str;
    }

    /**
     * 获取设备ID信息
     *
     * @param context 上下文对象
     * @return 包含设备ID信息的JSONObject对象
     */
    public static JSONObject getIDsInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("imei", getImei(context));
            jsonObject.put("imeis", getIMEIs(context));
            jsonObject.put("deviceIds", getDeviceIds(context));
            jsonObject.put("meids", getMEIDs(context));
            jsonObject.put("imsi", getIMSI(context));
            jsonObject.put("serialNo", getSerialNo(context));
            jsonObject.put("androidId", getAndroidId(context));
            jsonObject.put("iccid", getSimSerialNumber(context));
            jsonObject.put("phoneNo", getPhoneNumber(context));
            jsonObject.put("userAgent", getUserAgent(context));
            jsonObject.put("googleADID", getGoogleADID(context));
            jsonObject.put("drmId", getDrmId());
            jsonObject.put("description", AFInAppEventType("ro.build.description"));
            jsonObject.put("bootloader", AFInAppEventType("ro.build.bootloader"));
            jsonObject.put("bootimage_utc", AFInAppEventType("ro.bootimage.build.date.utc"));
            jsonObject.put("getprop", Cmd.exe("getprop"));
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return jsonObject;
    }
}
