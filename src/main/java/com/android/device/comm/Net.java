package com.android.device.comm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;


import com.android.utils.Cmd;
import com.android.utils.ULog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Net {
    private static final String TAG = "NetTAG";

    /**
     * 获取当前网络的接入点名称（APN）。
     *
     * @param context 应用程序上下文
     * @return 返回网络接入点名称，如果无法获取则返回空字符串
     */
    @SuppressLint("Range")
    public static String getApn(Context context) {//网络接入点名称  电信:ctnet 移动:cmnet
        final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");
        String apnName = "";
        try {
            Cursor cursor = context.getContentResolver().query(PREFERRED_APN_URI, new String[]{"_id", "apn", "type"}, (String) null, (String[]) null, (String) null);
            if (cursor != null) {
                cursor.moveToFirst();
                int counts = cursor.getCount();
                if (counts != 0 && !cursor.isAfterLast()) {
                    apnName = cursor.getString(cursor.getColumnIndex("apn"));
                }

                cursor.close();
            } else {
                cursor = context.getContentResolver().query(PREFERRED_APN_URI, (String[]) null, (String) null, (String[]) null, (String) null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    apnName = cursor.getString(cursor.getColumnIndex("user"));
                    cursor.close();
                }
            }
        } catch (Exception var7) {
            try {
                ConnectivityManager conManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = conManager.getActiveNetworkInfo();
                apnName = ni.getExtraInfo();
            } catch (Exception var6) {
                apnName = "";
            }
        }
        return apnName == null ? "" : apnName.replace("\"", "");
    }

    /**
     * 获取本机所有的IPv4地址，并以逗号分隔返回
     *
     * @return 返回本机所有的IPv4地址，以逗号分隔的字符串，如果获取失败则返回空字符串
     */
    public static String getIp4() {
        StringBuilder result = new StringBuilder();
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        if (result.length() > 0) {
                            result.append(',');
                        }
                        result.append(inetAddress.getHostAddress());
                    }
                }
            }
            return result.toString();
        } catch (Throwable e) {
            ULog.e(e);
            return "";
        }
    }


    /**
     * 获取本机所有IPv6地址，并以逗号分隔返回
     *
     * @return 返回本机所有IPv6地址的字符串，地址之间以逗号分隔；如果发生异常则返回空字符串
     */
    public static String getIp6() {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address) {
                        if (!first) {
                            result.append(',');
                        }
                        result.append(inetAddress.getHostAddress());
                        first = false;
                    }
                }
            }
            return result.toString();
        } catch (Throwable e) {
            ULog.e(e);
        }
        return "";
    }

    /**
     * 获取当前连接的Wi-Fi信息，并以JSON对象的形式返回。
     *
     * @param context 上下文对象
     * @return 包含Wi-Fi信息的JSON对象，若获取失败则返回null
     */
    public static JSONObject getLinkedWifi(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            JSONObject jsonObject = new JSONObject();
            if (connectionInfo != null) {
                jsonObject.put("ssid", connectionInfo.getSSID().replace("\"", ""));//wifi名称
                jsonObject.put("bssid", connectionInfo.getBSSID());//mac
            }
            try {
                if (connectionInfo != null) {
                    jsonObject.put("ip", FormatString(dhcpInfo.ipAddress));
                    jsonObject.put("mask", FormatString(dhcpInfo.netmask));
                    jsonObject.put("gateway", FormatString(dhcpInfo.gateway));
                    jsonObject.put("dns", FormatString(dhcpInfo.dns1));
                }
            } catch (Exception e) {
                ULog.e(e);
            }
            return jsonObject;
        } catch (Throwable e) {
            ULog.e(e);
        }
        return null;
    }

    /**
     * 获取设备的MAC地址
     *
     * @return MAC地址字符串，如果无法获取则返回空字符串
     */
    public static String getMacAddress() {
        StringBuilder sb = new StringBuilder();
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName("eth1");
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0");
            }
            if (networkInterface == null) {
                return "";
            } else {
                byte[] address = networkInterface.getHardwareAddress();
                if (null == address || 0 >= address.length) {
                    return "";
                }
                byte[] var4 = address;
                int var5 = address.length;

                for (int var6 = 0; var6 < var5; ++var6) {
                    byte b = var4[var6];
                    sb.append(String.format("%02X:", b));
                }

                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }

                return sb.toString();
            }
        } catch (Throwable e) {
            ULog.e(e);
        }
        return "";
    }


    /**
     * 获取设备MAC地址
     *
     * @param context 上下文对象
     * @return 返回设备MAC地址，若获取失败则返回null
     */
    public static String getMac1(Context context) {
        return Mac.getMacAddressTD(context);
    }

    static class Mac {
        public static String getMacAddressTD(Context context) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            String str = "";
            if (context.checkPermission("android.permission.ACCESS_WIFI_STATE", Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED && wifiManager.isWifiEnabled()) {
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (Build.VERSION.SDK_INT < 23) {
                    str = connectionInfo.getMacAddress();
                } else {
                    try {
                        NetworkInterface byInetAddress = NetworkInterface.getByInetAddress(InetAddress.getByName(str));
                        if (byInetAddress != null) {
                            return formatMac(byInetAddress.getHardwareAddress());
                        }
                        return "";
                    } catch (Exception e) {
//                        e.printStackTrace();
                    }

                }
            }
            if (str == null || "".equals(str.trim()) || str.length() == 0) {
                String str2 = "/sys/class/net/wlan0/address";//  /sys/class/net/wlan0/address
                String str3 = "/sys/class/net/eth0/address";//  /sys/class/net/eth0/address
                str = readFile(str2);
                if (str == null || "".equals(str.trim()) || str.length() == 0) {
                    str = readFile(str3);
                }
            }
            return TextUtils.isEmpty(str) ? getMac() : str;
        }


        private static String formatMac(byte[] bArr) {
            if (bArr == null) {
                return "";
            }
            StringBuffer stringBuffer = new StringBuffer(bArr.length);
            for (byte b : bArr) {
                String hexString = Integer.toHexString(b & (-1));
                if (hexString.length() == 1) {
                    stringBuffer.append("0");
                    stringBuffer.append(hexString);
                } else {
                    stringBuffer.append(hexString);
                }
                stringBuffer.append(":");
            }
            return stringBuffer.substring(0, stringBuffer.length() - 1);
        }


        private static String readFile(String str) {
            FileInputStream fileInputStream;
            Throwable th;
            File file = new File(str);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            FileInputStream fileInputStream2 = null;
            if (file.exists() && file.canRead()) {
                try {
                    fileInputStream = new FileInputStream(file);
                    try {
                        byte[] bArr = new byte[1024];
                        while (true) {
                            int read = fileInputStream.read(bArr);
                            if (read == -1) {
                                break;
                            }
                            byteArrayOutputStream.write(bArr, 0, read);
                        }
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        if (!(byteArray == null || byteArray.length == 0)) {
                            String replaceAll = new String(byteArray, "utf-8").replaceAll("\n", "").replaceAll(" ", "");
                            try {
                                fileInputStream.close();
                            } catch (IOException unused) {
                            }
                            try {
                                byteArrayOutputStream.close();
                            } catch (IOException unused2) {
                            }
                            return replaceAll;
                        }
                        try {
                            fileInputStream.close();
                        } catch (IOException unused3) {
                        }
                        try {
                            byteArrayOutputStream.close();
                        } catch (IOException unused4) {
                        }
                        return "";
                    } catch (Exception unused5) {
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException unused6) {
                            }
                        }
                        try {
                            byteArrayOutputStream.close();
                        } catch (IOException unused7) {
                        }
                        return null;
                    } catch (Throwable th2) {
                        th = th2;
                        fileInputStream2 = fileInputStream;
                        if (fileInputStream2 != null) {
                            try {
                                fileInputStream2.close();
                            } catch (IOException unused8) {
                            }
                        }
                        try {
                            byteArrayOutputStream.close();
                        } catch (IOException unused9) {
                        }
                        throw th;
                    }
                } catch (Exception unused10) {
                    fileInputStream = null;
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            return null;
        }

        public static String getMac() {
            if (Build.VERSION.SDK_INT > 23) {
                try {
                    Iterator it = Collections.list(NetworkInterface.getNetworkInterfaces()).iterator();
                    while (it.hasNext()) {
                        NetworkInterface networkInterface = (NetworkInterface) it.next();
                        if (networkInterface.getName().equalsIgnoreCase("wlan0")) {
                            byte[] hardwareAddress = networkInterface.getHardwareAddress();
                            if (hardwareAddress == null) {
                                return "";
                            }
                            StringBuilder sb = new StringBuilder();
                            for (byte b : hardwareAddress) {
                                sb.append(String.format(Locale.US, "%02X:", Byte.valueOf(b)));
                            }
                            if (sb.length() > 0) {
                                sb.deleteCharAt(sb.length() - 1);
                            }
                            return sb.toString().toLowerCase();
                        }
                    }
                } catch (Exception unused) {
                }
            }
            return "";
        }

    }

    /**
     * 获取Wi-Fi列表
     *
     * @param limit 返回的Wi-Fi数量限制
     * @param context 上下文对象
     * @return 包含Wi-Fi信息的JSON数组，如果没有权限或发生异常则返回null
     * @throws Throwable 如果发生异常，将抛出该异常
     */
    public static JSONArray getWifiList(int limit, Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                JSONArray jsonArray = new JSONArray();
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> scanResults = wifiManager.getScanResults();
                Collections.sort(scanResults, (lhs, rhs) -> rhs.level - lhs.level);
                for (int i = 0; i < scanResults.size() && i < limit; ++i) {
                    JSONObject jsonObject = new JSONObject();
                    ScanResult scanResult = scanResults.get(i);
                    jsonObject.put("ssid", scanResult.SSID);
                    jsonObject.put("ssid", scanResult.SSID);
                    //jsonObject.put("bssid", scanResult.BSSID.replace("2", "1").replace("a", "b"));
                    jsonObject.put("bssid", scanResult.BSSID);
                    jsonObject.put("level", WifiManager.calculateSignalLevel(scanResult.level, 1001));
                    jsonObject.put("capabilities", scanResult.capabilities);
                    jsonObject.put("frequency", scanResult.frequency);
                    jsonObject.put("describeContents", scanResult.describeContents());
                    jsonArray.put(jsonObject);
                }
                return jsonArray;
            }
        } catch (Throwable e) {
            ULog.e(e);
        }
        return null;
    }

    /**
     * 获取基站ID
     *
     * @param context 上下文对象
     * @return 返回基站ID的字符串，格式为"基站ID,网络编号"，若获取失败则返回空字符串
     */
    public static String getBaseStationId(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_DENIED) {
                if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
                    CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) telephonyManager.getCellLocation();
                    if (cdmaCellLocation != null) {
                        int cid = cdmaCellLocation.getBaseStationId(); //获取cdma基站识别标号 BID
                        int lac = cdmaCellLocation.getNetworkId(); //获取cdma网络编号NID
                        return cid + "," + lac;
                        //int sid = cdmaCellLocation.getSystemId(); //用谷歌API的话cdma网络的mnc要用这个getSystemId()取得→SID
                    }
                } else {
                    GsmCellLocation gsmCellLocation = (GsmCellLocation) telephonyManager.getCellLocation();
                    if (gsmCellLocation != null) {
                        int cid = gsmCellLocation.getCid(); //获取gsm基站识别标号
                        int lac = gsmCellLocation.getLac(); //获取gsm网络编号
                        return cid + "," + lac;
                    }
                }
            }
        } catch (Throwable e) {
            ULog.e(e);
        }
        return "";
    }

    /**
     * 获取基站ID。
     *
     * @param context 上下文对象
     * @return 返回基站ID字符串，若获取失败则返回空字符串
     */
    public static String getBaseStationId1(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED //
                    && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return "";
            }
            TelephonyManager telephonyManager = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            return String.valueOf(telephonyManager.getCellLocation());
        } catch (Throwable ignored) {
        }
        return "";
    }

    /**
     * 获取蓝牙设备的MAC地址
     *
     * @return 蓝牙设备的MAC地址，如果蓝牙未开启或发生异常则返回空字符串或"not available"
     */
    public static String getBlutoothMac() {
        try {
            // 获取BluetoothAdapter实例
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            // 判断蓝牙是否开启
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                // 使用获取到的MAC地址
                return bluetoothAdapter.getAddress();
            } else {
                return "not available";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取蓝牙地址
     *
     * @param context 上下文对象
     * @return 蓝牙地址字符串，如果无法获取则返回空字符串
     */
    public static String getBluetoothAddress(Context context) {
        try {
            String adr = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                adr = Settings.Secure.getString(context.getContentResolver(), "bluetooth_address");
            }
            if (TextUtils.isEmpty(adr)) {
                adr = "no";
            }
            return adr;
        } catch (Exception e) {
            ULog.e(e);
            return "";
        }
    }

    /**
     * 获取所有蜂窝网络信息，并封装成JSONObject返回。
     *
     * @param context 上下文对象
     * @return 包含所有蜂窝网络信息的JSONObject，如果未获取到信息则返回空JSONObject
     */
    public static JSONObject getAllCellInfo(Context context) {
        JSONObject cellInfo_json = new JSONObject();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                JSONArray gsmArr = new JSONArray();
                JSONArray cdmaArr = new JSONArray();
                JSONArray lteArr = new JSONArray();
                List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                for (CellInfo cellInfo : cellInfoList) {
                    if (cellInfo instanceof CellInfoGsm) {
                        try {
                            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
                            CellIdentityGsm cellIdentity = cellInfoGsm.getCellIdentity();

                            JSONObject gsm = new JSONObject();
                            if (cellIdentity.getCid() != Integer.MAX_VALUE) {
                                gsm.put("cid", cellIdentity.getCid());
                            }

                            if (cellIdentity.getLac() != Integer.MAX_VALUE) {
                                gsm.put("lac", cellIdentity.getLac());
                            }
                            if (cellIdentity.getMcc() != Integer.MAX_VALUE) {
                                gsm.put("mcc", cellIdentity.getMcc());
                            }

                            if (cellIdentity.getMnc() != Integer.MAX_VALUE) {
                                gsm.put("mnc", cellIdentity.getMnc());
                            }
                            gsmArr.put(gsm);
                        } catch (Throwable e) {
                            ULog.e(e);
                        }

                    } else if (cellInfo instanceof CellInfoCdma) {
                        try {
                            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
                            CellIdentityCdma cellIdentity = cellInfoCdma.getCellIdentity();
                            JSONObject cdma = new JSONObject();
                            if (cellIdentity.getNetworkId() != Integer.MAX_VALUE) {
                                cdma.put("mNetworkId", cellIdentity.getNetworkId());
                            }

                            if (cellIdentity.getSystemId() != Integer.MAX_VALUE) {
                                cdma.put("mSystemId", cellIdentity.getSystemId());
                            }

                            if (cellIdentity.getBasestationId() != Integer.MAX_VALUE) {
                                cdma.put("mBasestationId", cellIdentity.getBasestationId());
                            }

                            if (cellIdentity.getLatitude() != Integer.MAX_VALUE) {
                                cdma.put("mLatitude", cellIdentity.getLatitude());
                            }

                            if (cellIdentity.getLongitude() != Integer.MAX_VALUE) {
                                cdma.put("mLongitude", cellIdentity.getLongitude());
                            }
                            cdmaArr.put(cdma);
                        } catch (Throwable e) {
                            ULog.e(e);
                        }

                    } else if (cellInfo instanceof CellInfoLte) {
                        try {
                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                            CellIdentityLte cellIdentity = cellInfoLte.getCellIdentity();
                            JSONObject lte = new JSONObject();
                            if (cellIdentity.getCi() != Integer.MAX_VALUE) {
                                lte.put("ci", cellIdentity.getCi());
                            }
                            if (cellIdentity.getMcc() != Integer.MAX_VALUE) {
                                lte.put("mnc", cellIdentity.getMcc());
                            }

                            if (cellIdentity.getPci() != Integer.MAX_VALUE) {
                                lte.put("pic", cellIdentity.getPci());
                            }

                            if (cellIdentity.getTac() != Integer.MAX_VALUE) {
                                lte.put("tac", cellIdentity.getTac());
                            }

                            if (cellIdentity.getMcc() != Integer.MAX_VALUE) {
                                lte.put("mcc", cellIdentity.getMcc());
                            }
                            lteArr.put(lte);
                        } catch (Throwable e) {
                            ULog.e(e);
                        }
                    }
                }
                if (gsmArr.length() > 0) {
                    cellInfo_json.put("gsm", gsmArr);
                }

                if (cdmaArr.length() > 0) {
                    cellInfo_json.put("cdma", cdmaArr);
                }

                if (lteArr.length() > 0) {
                    cellInfo_json.put("lte", lteArr);
                }
            }
        } catch (Exception e) {
            ULog.e(e);
        }
        return cellInfo_json;
    }

//    public static JSONArray getNeighboringCellInfo() {
//        try {
//            TelephonyManager manager = (TelephonyManager) UApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE);
//            int strength = 0;
//            if (ActivityCompat.checkSelfPermission(UApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_DENIED && Build.VERSION.SDK_INT <= 29) {
//                List<NeighboringCellInfo> infoLists = manager.getNeighboringCellInfo();
//                if (infoLists != null && infoLists.size() > 0) {
//                    JSONArray nbcis = new JSONArray();
//                    for (NeighboringCellInfo info : infoLists) {
//                        strength += (-133 + 2 * info.getRssi());// 获取邻区基站信号强度
//                        JSONObject jobj = new JSONObject();
//                        jobj.put("rssi", info.getRssi());
//                        jobj.put("strength", strength);
//                        jobj.put("cid", info.getCid());
//                        jobj.put("lac", info.getLac());
//                        nbcis.put(jobj);
//                    }
//                    return nbcis;
//                }
//
//            }
//        } catch (Throwable e) {
//        }
//
//        return null;
//    }

    /**
     * 获取当前系统WiFi代理设置
     *
     * @return 当前WiFi代理设置，格式为"代理主机:代理端口"，如果未设置代理则返回空字符串
     */
    public static String getWifiProxy() {
        try {
            String result = "";
            String proxyHost = System.getProperty("http.proxyHost");
            String proxyPort = System.getProperty("http.proxyPort");
            if (!TextUtils.isEmpty(proxyHost)) {
                result = proxyHost + ":" + proxyPort;
            }
            return result;
        } catch (Exception e) {
            ULog.e(e);
        }
        return "";
    }

    /**
     * 将整数格式化为字符串
     *
     * @param value 要格式化的整数
     * @return 格式化后的字符串，格式为"XXX.XXX.XXX.XXX"，其中XXX为整数转换成字节后的值
     */
    static String FormatString(int value) {
        String strValue = "";
        byte[] ary = intToByteArray(value);
        for (int i = ary.length - 1; i >= 0; i--) {
            strValue += (ary[i] & 0xFF);
            if (i > 0) {
                strValue += ".";
            }
        }
        return strValue;
    }

    /**
     * 将整数转换为字节数组
     *
     * @param value 需要转换的整数
     * @return 包含四个字节的字节数组，表示输入的整数
     */
    static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }


    /**
     * 判断当前设备是否连接到了Wi-Fi网络。
     *
     * @param context 上下文对象，通常使用this传入当前的Activity或Application对象。
     * @return 如果当前设备连接到了Wi-Fi网络，则返回true；否则返回false。
     */
    public static boolean isWifi(Context context) {
        return Wifi.getNetWorkStatus(context) == 1;
    }

    static class Wifi {
        private static Integer networkType;

        public final static int getNetWorkStatus(Context context) {
            Integer num = networkType;
            Integer num2 = 0;
            if (num != null && (num == null || num.intValue() != 0)) {
                Integer num3 = networkType;
                if (num3 != null) {
                    return num3.intValue();
                }
                return 0;
            } else if (Build.VERSION.SDK_INT < 23 || context.checkSelfPermission("android.permission.ACCESS_NETWORK_STATE") == PackageManager.PERMISSION_GRANTED) {
                Object systemService = context.getSystemService(Context.CONNECTIVITY_SERVICE);
                TelephonyManager telephonyManager = null;
                if (!(systemService instanceof ConnectivityManager)) {
                    systemService = null;
                }
                ConnectivityManager connectivityManager = (ConnectivityManager) systemService;
                if (connectivityManager != null) {
                    if (!isNetworkAvailable(connectivityManager)) {
                        networkType = num2;
                        return 0;
                    } else if (isWiFiNetwork(connectivityManager)) {
                        networkType = 1;
                        return 1;
                    }
                }

                //TODO  mobileNetworkType
//            Object systemService2 = context.getSystemService(Context.TELEPHONY_SERVICE);
//            if (systemService2 instanceof TelephonyManager) {
//                telephonyManager = (TelephonyManager) systemService2;
//            }
//            Integer valueOf = Integer.valueOf(INSTANCE.mobileNetworkType(context, telephonyManager, connectivityManager));
//            networkType = valueOf;
//            if (valueOf != null) {
//                return valueOf.intValue();
//            }
                return 0;
            } else {
                networkType = num2;
                if (num2 != null) {
                    return num2.intValue();
                }
                return 0;
            }
        }

        /**
         * 检查网络是否可用。
         *
         * @param connectivityManager ConnectivityManager实例，用于检查网络连接状态
         * @return 如果网络可用，则返回true；否则返回false
         */
        private static boolean isNetworkAvailable(ConnectivityManager connectivityManager) {
            NetworkCapabilities networkCapabilities;
            if (connectivityManager == null) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= 23) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null || (networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)) == null) {
                    return false;
                }
                return isNetworkValid(networkCapabilities);
            }
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

        /**
         * 判断网络是否有效
         *
         * @param networkCapabilities 网络能力对象
         * @return 如果网络有效返回true，否则返回false
         * @throws IllegalArgumentException 如果networkCapabilities参数为null或Android SDK版本低于21
         */
        @SuppressLint("WrongConstant")
        @TargetApi(Build.VERSION_CODES.M)
        private static boolean isNetworkValid(NetworkCapabilities networkCapabilities) {
            if (networkCapabilities == null || Build.VERSION.SDK_INT < 21) {
                return false;
            }
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || networkCapabilities.hasTransport(7)
                    || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    || networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }

        /**
         * 判断当前网络是否为Wi-Fi网络。
         *
         * @param connectivityManager ConnectivityManager对象，用于获取网络状态信息
         * @return 如果当前网络是Wi-Fi网络，则返回true；否则返回false
         */
        private static boolean isWiFiNetwork(ConnectivityManager connectivityManager) {
            NetworkCapabilities networkCapabilities;
            NetworkInfo networkInfo = null;
            Network network = null;
            if (Build.VERSION.SDK_INT >= 23) {
                if (connectivityManager != null) {
                    network = connectivityManager.getActiveNetwork();
                }
                if (network == null || (networkCapabilities = connectivityManager.getNetworkCapabilities(network)) == null) {
                    return false;
                }
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            }
            if (connectivityManager != null) {
                networkInfo = connectivityManager.getNetworkInfo(1);
            }
            return networkInfo != null && networkInfo.isConnectedOrConnecting();
        }
    }

    /**
     * 执行系统命令并返回执行结果。
     *
     * @param exe 要执行的命令字符串
     * @return 命令执行结果，如果执行失败则返回空字符串
     */
    public static String getCmdResult(String exe) {
        try {
            return Cmd.exe(exe);
        } catch (Exception e) {
//            throw new RuntimeException(e);
            return "";
        }
    }

    /**
     * 获取网络信息的静态方法
     *
     * @param context 上下文对象
     * @return 包含网络信息的JSONObject对象，若获取失败则返回null
     */
    public static JSONObject getNetInfo(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String networkCountryIso = tm.getNetworkCountryIso();
            String networkOperator = tm.getNetworkOperator();
            String networkSpecifier = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                networkSpecifier = tm.getNetworkSpecifier();
            }
/*
            int networkType = -10086;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    networkType = tm.getNetworkType();
                }
            }
*/
            int networkType = -1001;
            int networkSubType = -1002;
            String networkTypeName = "uncatch";
            String networkSubTypeName = "uncatch";
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                networkType = networkInfo.getType();
                networkSubType = networkInfo.getSubtype();
                networkTypeName = networkInfo.getTypeName();
                networkSubTypeName = networkInfo.getSubtypeName();
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("networkCountryIso", networkCountryIso);
            jsonObject.put("networkOperator", networkOperator);
            jsonObject.put("networkSpecifier", networkSpecifier);
            jsonObject.put("networkType", networkType);
            jsonObject.put("networkSubType", networkSubType);
            jsonObject.put("networkTypeName", networkTypeName);
            jsonObject.put("networkSubTypeName", networkSubTypeName);
            jsonObject.put("apn", getApn(context));
            jsonObject.put("ip4", getIp4());
            jsonObject.put("ip6", getIp6());
            jsonObject.put("mac1", getMacAddress());
            jsonObject.put("mac2", getMac1(context));
            jsonObject.put("linkedWifi", getLinkedWifi(context));
            jsonObject.put("wifiList", getWifiList(10, context));
            jsonObject.put("isWifi", isWifi(context));
            jsonObject.put("wifiProxy", getWifiProxy());
            jsonObject.put("baseStationId", getBaseStationId(context));
            jsonObject.put("baseStationId1", getBaseStationId1(context));
            jsonObject.put("bluetoothAddress", getBluetoothAddress(context));
            jsonObject.put("bluetoothMAC", getBlutoothMac());
            jsonObject.put("allCellInfo", getAllCellInfo(context));
            jsonObject.put("IP_address", getCmdResult("ip address"));
            jsonObject.put("ip_neighbor", getCmdResult("ip neighbor"));
            jsonObject.put("ip_route_list_match_0", getCmdResult("ip route list match 0 table all scope global"));
            jsonObject.put("ip_route", getCmdResult("ip route"));
            jsonObject.put("ip_addr_show", getCmdResult("ip addr show "));

            return jsonObject;
        } catch (Exception e) {
            ULog.e(e);
        }
        return null;
    }

}
