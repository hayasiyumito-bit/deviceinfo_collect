package com.android.device.comm;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;

import com.android.utils.ULog;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

public class SimCard {

    /**
     * 判断设备中是否装有SIM卡
     *
     * @param context 上下文对象
     * @return 如果设备中装有SIM卡，则返回true；否则返回false
     */
    public static boolean hasIccCard(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return telephonyManager.hasIccCard();
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return false;
    }

    /**
     * 判断设备是否插入了SIM卡
     *
     * @param context 上下文对象
     * @return 如果设备插入了SIM卡，则返回true；否则返回false
     */
    public static boolean hasSimCard(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                int simState = telephonyManager.getSimState();
                switch (simState) {
                    case TelephonyManager.SIM_STATE_UNKNOWN:
                    case TelephonyManager.SIM_STATE_ABSENT:
                        return false;
                }
            }
        } catch (Throwable e) {
            // 删除ULog，静默处理异常
        }
        return true;
    }

    /**
     * 获取SIM卡的运营商名称。
     *
     * @param context 上下文对象
     * @return 返回SIM卡的运营商名称，如果无法获取则返回空字符串
     */
    public static String getSimOperator(Context context) {

        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                return telephonyManager.getSimOperator();
            }
        } catch (Throwable e) {
//            ULog.e(e);
        }
        return "";
    }

    /**
     * 获取当前设备使用的数据卡的运营商名称。
     *
     * @param context 上下文对象
     * @return 当前设备使用的数据卡的运营商名称，如“中国联通”、“China Unicom”、“中国电信”、“China Telecom”或“中国移动”、“China Mobile”，具体返回结果取决于当前设备使用的数据卡
     */
    public static String getSimOperatorName(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //返回 中国联通或China Unicom，中国电信或China Telecom，中国移动或China Mobile 返回什么根据当前设备使用的数据卡而定
        return tm.getSimOperatorName();
    }

    /**
     * int NO_PHONE = 0;
     * int GSM_PHONE = 1;
     * int CDMA_PHONE = 2;
     * int SIP_PHONE  = 3;
     */
    /**
     * 获取手机类型
     *
     * @param context 上下文对象
     * @return 手机类型，返回值为 {@link TelephonyManager#PHONE_TYPE_NONE}、
     * {@link TelephonyManager#PHONE_TYPE_GSM}、
     * {@link TelephonyManager#PHONE_TYPE_CDMA}、
     * {@link TelephonyManager#PHONE_TYPE_SIP}之一
     */
    public static int getPhoneType(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getPhoneType();
    }

    /**
     * 获取SIM卡的运营商名称
     *
     * @param context 上下文对象
     * @return SIM卡的运营商名称，若API级别低于P，则返回空字符串
     */
    public static CharSequence getSimCarrierIdName(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return tm.getSimCarrierIdName();
        }
        return "";
    }

    /**
     * 获取网络运营商名称
     *
     * @param context 上下文对象
     * @return 返回网络运营商的名称
     */
    public static String getNetworkOperatorName(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getNetworkOperatorName();
    }


    /**
     * 获取SIM卡的运营商ID。
     *
     * @param context 上下文对象
     * @return 如果设备运行的Android版本大于等于9.0（API级别28），则返回SIM卡的运营商ID；否则返回0。
     */
    public static int getSimCarrierId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return tm.getSimCarrierId();
        }
        return 0;
    }


    /**
     * 获取GSM信息
     *
     * @param context 上下文对象
     * @return 包含MCC、MNC、LAC、CID等信息的JSONObject对象
     */
    public static JSONObject getGSMInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_DENIED
                        && context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_DENIED) {
                    TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (mTelephonyManager != null) {
                        String operator = mTelephonyManager.getNetworkOperator();
                        if (!TextUtils.isEmpty(operator) && operator.length() >= 5) {
                            String mcc = operator.substring(0, 3);
                            String mnc = operator.substring(3, 5);
                            CellLocation location = mTelephonyManager.getCellLocation();
                            if (location instanceof GsmCellLocation) {
                                GsmCellLocation gsmCellLocation = (GsmCellLocation) location;
                                int lac = gsmCellLocation.getLac();
                                int cellId = gsmCellLocation.getCid();
                                jsonObject.put("mcc", mcc);
                                jsonObject.put("mnc", mnc);
                                jsonObject.put("lac", lac);
                                jsonObject.put("cid", cellId);

                            } else if (location instanceof CdmaCellLocation) {
                                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) location;
                                int lac = cdmaCellLocation.getNetworkId();
                                int cellId = cdmaCellLocation.getBaseStationId();
                                jsonObject.put("mcc", mcc);
                                jsonObject.put("mnc", mnc);
                                jsonObject.put("lac", lac);
                                jsonObject.put("cid", cellId);
                            }
                        }
                        return jsonObject;
                    }
                }
            }

        } catch (Throwable e) {
            // 删除ULog，静默处理异常
        }
        return jsonObject;
    }

    /**
     * 获取设备的订阅者ID（IMSI）。
     *
     * @param context 上下文对象
     * @return 设备的订阅者ID（IMSI），如果无法获取则返回空字符串
     */
    public static String getSubscriberId(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_DENIED
                        && Build.VERSION.SDK_INT < 29) {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        return telephonyManager.getSubscriberId();
                    }
                }
            }
        } catch (Throwable e) {

        }
        return "";
    }

    /**
     * 获取SIM卡的序列号
     *
     * @param context 上下文对象
     * @return SIM卡的序列号，如果获取失败或没有权限则返回空字符串
     */
    public static String getSimSerialNumber(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        return telephonyManager.getSimSerialNumber();
                    }
                }
            }
        } catch (Exception e) {
            // 权限不足时返回空字符串
        }
        return "";
    }

    /**
     * 获取手机号码
     *
     * @param context 上下文对象
     * @return 手机号码，如果获取失败则返回空字符串
     */
    public static String getPhoneNumber(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_DENIED) {
                    TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    return manager.getLine1Number();
                }
            }
        } catch (Throwable e) {

        }
        return "";
    }

    /**
     * 查询电话SIM卡信息
     *
     * @param context 上下文对象
     * @return 返回包含SIM卡信息的JSONObject对象
     */
    public static JSONObject queryTelephonySimInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            // 检查权限，避免SecurityException
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return jsonObject;
                }
            }
            Uri uri = Uri.parse("content://telephony/siminfo"); //访问raw_contacts表
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(uri, new String[]{"_id", "icc_id", "sim_id", "display_name", "carrier_name", "name_source", "color", "number", "display_number_format", "data_roaming", "mcc", "mnc"}, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int count = cursor.getColumnCount();
                    for (int i = 0; i < count; i++) {
                        String key = cursor.getColumnName(i);
                        String value = cursor.getString(i);
                        try {
                            jsonObject.put(key, value);
                        } catch (JSONException e) {
                            // 删除ULog，静默处理异常
                        }
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return jsonObject;
    }


    /**
     * 获取移动网络国家代码（MNC）。
     *
     * @param context 上下文对象，用于获取资源
     * @return 返回移动网络国家代码（MNC）
     */
    public static int getMNC(Context context) {
        return context.getResources().getConfiguration().mnc;
    }

    /**
     * 获取移动国家代码（MCC）。
     *
     * @param context 上下文对象
     * @return 返回移动国家代码（MCC）
     */
    public static int getMCC(Context context) {
        return context.getResources().getConfiguration().mcc;
    }

    /**
     * 判断给定的网络信息对象是否不为空且处于连接或正在连接状态。
     *
     * @param networkInfo 网络信息对象
     * @return 如果网络信息对象不为空且处于连接或正在连接状态，则返回true；否则返回false
     */
    private static boolean valueOf(NetworkInfo networkInfo) {
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    /**
     * 获取当前网络连接类型（WIFI、MOBILE或其他）
     *
     * @param context 上下文对象
     * @return 当前网络类型，WIFI、MOBILE 或 OTHERS
     */
    public static String getWifiorMobile(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                int i = 0;
                Network[] allNetworks = connectivityManager.getAllNetworks();
                int length = allNetworks.length;
                while (i < length) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(allNetworks[i]);
                    if (!valueOf(networkInfo)) {
                        i++;
                    } else if (1 == networkInfo.getType()) {
                        return "WIFI";
                    } else if (networkInfo.getType() == 0) {
                        return "MOBILE";
                    } else
                        //different from af logic which no existence for instance vpn
                        return "OTHERS";
                }
            }
        } catch (Throwable th2) {
            return null;
        }
        return null;
    }

    /**
     * 获取SIM卡信息
     *
     * @param context 上下文对象
     * @return 包含SIM卡信息的JSONObject对象
     * @throws Exception 如果获取SIM卡信息时发生异常，则抛出异常
     */
    public static JSONObject getSimCardInfo(Context context) {
        JSONObject info = new JSONObject();
        try {
            info.put("hasIccCard", hasIccCard(context));
            info.put("hasSimCard", hasSimCard(context));
            info.put("phoneType", getPhoneType(context));

            info.put("simOperator", getSimOperator(context));
            info.put("mcc", getMCC(context));
            info.put("mnc", getMNC(context));
            info.put("simSerialNumber", getSimSerialNumber(context));
            info.put("subscriberId", getSubscriberId(context) + "");
            info.put("phoneNumber", getPhoneNumber(context));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.put("simCarrierId", getSimCarrierId(context));
            }
            info.put("gsmInfo", getGSMInfo(context));
            info.put("simOperatorName", getSimOperatorName(context));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.put("simCarrierIdName", getSimCarrierIdName(context));
            }
            info.put("simCarrierName", getNetworkOperatorName(context));
            info.put("queryTelephonySimInfo", queryTelephonySimInfo(context));
            info.put("network", getWifiorMobile(context));
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }

        return info;
    }
}
