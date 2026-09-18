package com.android.device.ids;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import android.util.Log;
import android.content.Context;

import org.json.JSONObject;

public class Gpreferrer {
    private static final String TAG = "deviceinfo";
    /**
     * 获取安装引荐信息
     *
     * @param context 上下文对象
     * @return 包含安装引荐信息的JSONObject对象
     */
    public static JSONObject getGpReferrer(Context context) {
        InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(context).build();
        JSONObject jsonObject = new JSONObject();
        // 开始连接
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        try {
                            // 获取安装引荐信息
                            ReferrerDetails response = referrerClient.getInstallReferrer();
                            String referrer = response.getInstallReferrer();
                            long click_ts = response.getReferrerClickTimestampSeconds();
                            long install_begin_ts = response.getInstallBeginTimestampSeconds();
                            boolean instant = response.getGooglePlayInstantParam();
                            String install_version = response.getInstallVersion();
                            long install_begin_server_ts = response.getInstallBeginTimestampServerSeconds();
                            long click_server_ts = response.getReferrerClickTimestampServerSeconds();
                            jsonObject.put("referrer", referrer);
                            jsonObject.put("instant", instant);
                            jsonObject.put("install_version", install_version);
                            jsonObject.put("install_begin_server_ts", install_begin_server_ts);
                            jsonObject.put("install_begin_ts", install_begin_ts);
                            jsonObject.put("click_server_ts", click_server_ts);
                            jsonObject.put("click_ts", click_ts);

                        } catch (Exception e) {
//                            e.printStackTrace();
                        }
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        Log.e(TAG, "Install Referrer not supported");
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        Log.e(TAG, "Install Referrer service unavailable");
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                // 尝试重新连接
                Log.d(TAG, "onInstallReferrerServiceDisconnected: trying to reconnect");
                //referrerClient.startConnection(this);
            }
        });
        return jsonObject;
    }
}
