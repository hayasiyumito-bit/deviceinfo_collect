package com.android.device.software;

import android.content.Context;
import android.content.pm.InstallSourceInfo;
import android.os.Build;

import com.android.utils.CompressString;

import org.json.JSONObject;

public class PackageInfo {
    /**
     * 获取安装包信息
     * @param context 上下文
     * @return  安装包信息
     */
    public static JSONObject getInstallerInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                InstallSourceInfo installSourceInfo = context.getPackageManager().getInstallSourceInfo(context.getPackageName());
                jsonObject.put("initiatingPackageNameNewApi", installSourceInfo.getInitiatingPackageName());
                jsonObject.put("installingPackageNameNewApi", installSourceInfo.getInstallingPackageName());
                jsonObject.put("originatingPackageNameNewApi", installSourceInfo.getOriginatingPackageName());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    jsonObject.put("packageSourceNewApi", installSourceInfo.getPackageSource());
                }
            }
            jsonObject.put("installingPackageName", context.getPackageManager().getInstallerPackageName(context.getPackageName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

}
