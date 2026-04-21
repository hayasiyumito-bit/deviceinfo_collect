package com.android.device.snapshot;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

/**
 * 设备信息快照的唯一入口：返回聚合后的 {@link JSONObject}。
 * 具体字段由各域类提供，组装逻辑见 {@link SnapshotFields}。
 */
public final class DeviceSnapshot {

    private static final String TAG = "DeviceSnapshot";

    private DeviceSnapshot() {
    }

    /**
     * 采集当前设备快照（全量字段）。
     *
     * @return 根 JSON；context 为空或发生不可恢复错误时返回 null
     */
    public static JSONObject collect(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return null;
        }
        try {
            JSONObject root = new JSONObject();
            SnapshotFields.addTimestamp(root);
            SnapshotFields.fillAll(context, root);
            SnapshotDebugSink.maybeExportDebugArtifacts(context, root);
            Log.d(TAG, "Snapshot collection completed successfully");
            return root;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting snapshot", e);
            return null;
        }
    }
}
