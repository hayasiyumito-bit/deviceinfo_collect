package com.android.device;

import android.content.Context;
import android.util.Log;

import com.android.device.snapshot.DeviceSnapshot;

import org.json.JSONObject;

/**
 * 聚合 {@link DeviceSnapshot} 与 App 侧安全/Hook/Root 检测，供列表展示。
 * 每次调用均实时采集，不读取 SP/内存快照缓存。
 */
public final class DeviceSnapshotMerger {

    private static final String TAG = "DeviceSnapshotMerger";
    private static final long ROOT_PROBE_WAIT_MS = 3500L;

    private DeviceSnapshotMerger() {
    }

    /** 全量实时采集：Root 探测与设备快照并行，均不使用历史缓存。 */
    public static JSONObject collectFull(Context context) {
        RootAccessHelper.beginFreshAttempt();

        JSONObject root = DeviceSnapshot.collect(context);
        if (root == null) {
            root = new JSONObject();
        }

        try {
            root.put("collectedAt", System.currentTimeMillis());
        } catch (Exception e) {
            Log.w(TAG, "Failed to write collection metadata", e);
        }

        RootAccessHelper.awaitAttempt(ROOT_PROBE_WAIT_MS);

        try {
            JSONObject security = SecurityReportComposer.build(context);
            root.put("security", security);
            root.put("anyRisk", security.optBoolean("anyRisk", false));
            root.put("anyRiskReasons", security.optJSONArray("anyRiskReasons"));
        } catch (Exception e) {
            Log.w(TAG, "security report skipped", e);
        }
        DeviceSnapshot.exportDebugArtifacts(context, root);
        return root;
    }
}
