package com.android.device.sdk;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.android.device.RootAccessHelper;
import com.android.device.SecurityReportComposer;
import com.android.device.snapshot.DeviceSnapshot;

import org.json.JSONObject;

import java.security.MessageDigest;

/**
 * 设备采集 SDK 的唯一公开入口。
 *
 * <p>把 {@code deviceinfo_collect} 的「设备快照 + 风控检测」封装成一个可被任意 App
 * (如 YumyHook 管理器)调用的库：{@link #collect} 采集全量参数,{@link #report} 上报,
 * {@link #collectAndReport} 一步到位。全部为同步阻塞方法,调用方需放到工作线程执行。
 */
public final class DeviceCollectSdk {

    public static final String SDK_VERSION = "1.0.0";
    public static final int SCHEMA_VERSION = 1;
    private static final String TAG = "DeviceCollectSdk";

    private DeviceCollectSdk() {}

    /**
     * 全量实时采集：设备快照(build/硬件/网络/传感器/存储…)+ 风控(root/hook/模拟器)。
     * 复用 {@link DeviceSnapshot#collect} 与 {@link SecurityReportComposer#build},
     * 采集与 root 探测并行,均不读历史缓存。
     *
     * @return 合并后的根 JSON;失败返回一个仅含错误标记的对象,不抛异常
     */
    public static JSONObject collect(Context context) {
        return collect(context, 2_000L, true);
    }

    public static JSONObject collect(Context context, long rootProbeWaitMs) {
        return collect(context, rootProbeWaitMs, true);
    }

    /**
     * @param includeRisk 是否执行风控检测(root/hook/模拟器)。
     *   <p><b>警告</b>：风控检测会密集 fork 子进程(app_process/su/shell)并可能强停其它检测 App,
     *   这是「前台主动诊断」的用法。在<b>宿主 App 启动时的后台线程</b>里静默跑它,会触发 Android 12+
     *   的 phantom process killer 把宿主进程 SIGKILL(表现为闪退)。因此宿主的自动上报应传 {@code false},
     *   只采集设备快照;需要风控时应由用户主动触发、或在关闭 phantom 监控的受控环境下运行。
     */
    public static JSONObject collect(Context context, long rootProbeWaitMs, boolean includeRisk) {
        if (includeRisk) {
            RootAccessHelper.beginFreshAttempt();
        }

        JSONObject root = DeviceSnapshot.collect(context);
        if (root == null) {
            root = new JSONObject();
        }
        try {
            root.put("collectedAt", System.currentTimeMillis());
        } catch (Exception ignored) {
        }

        if (includeRisk) {
            RootAccessHelper.awaitAttempt(rootProbeWaitMs);
            try {
                JSONObject security = SecurityReportComposer.build(context);
                root.put("security", security);
                root.put("anyRisk", security.optBoolean("anyRisk", false));
                root.put("anyRiskReasons", security.optJSONArray("anyRiskReasons"));
                root.put("anyRiskFixHints", security.optJSONArray("anyRiskFixHints"));
                root.put("remediation", security.optJSONObject("remediation"));
            } catch (Exception e) {
                Log.w(TAG, "security report skipped", e);
            }
        }
        return root;
    }

    /**
     * 采集并上报（同步）。调用方应在工作线程执行。
     *
     * @return 上报结果
     */
    public static ReportClient.Result collectAndReport(Context context, ReportConfig config) {
        JSONObject payload = collect(context, config.rootProbeWaitMs(), config.includeRisk());
        return report(context, payload, config);
    }

    /**
     * 上报一份已采集好的快照（同步）。会自动套上上报信封（设备号/包名/版本/风险摘要）。
     */
    public static ReportClient.Result report(Context context, JSONObject payload,
                                             ReportConfig config) {
        JSONObject envelope = new JSONObject();
        try {
            envelope.put("schema", SCHEMA_VERSION);
            envelope.put("sdk_version", SDK_VERSION);
            envelope.put("device_id", deviceId(context));
            envelope.put("app_package", config.appPackage());
            envelope.put("app_version", config.appVersion());
            // 上报来源；为空则不写该键，服务端按 app_package 回退推断（兼容旧包）。
            if (config.source() != null && !config.source().isEmpty()) {
                envelope.put("source", config.source());
            }
            envelope.put("collected_at", payload.optLong("collectedAt", System.currentTimeMillis()));
            envelope.put("any_risk", payload.optBoolean("anyRisk", false));
            envelope.put("risk_reasons", payload.optJSONArray("anyRiskReasons"));
            envelope.put("payload", payload);
        } catch (Exception e) {
            return new ReportClient.Result(false, -1, "envelope build failed: " + e.getMessage());
        }
        return ReportClient.post(config, envelope);
    }

    /**
     * 稳定设备标识：ANDROID_ID 的 SHA-256(十六进制,前 32 位)。ANDROID_ID 不可读时退化为 "unknown"。
     */
    public static String deviceId(Context context) {
        String raw;
        try {
            raw = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            raw = null;
        }
        if (raw == null || raw.isEmpty()) {
            return "unknown";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16 && i < d.length; i++) {
                sb.append(Character.forDigit((d[i] >> 4) & 0xF, 16));
                sb.append(Character.forDigit(d[i] & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
