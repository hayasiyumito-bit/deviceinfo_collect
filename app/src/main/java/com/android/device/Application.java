package com.android.device;

import android.util.Log;

import com.android.device.Jni.JniInterface;
import com.android.device.provenance.ProjectProvenance;
import com.android.device.report.DeviceReportManager;

public class Application extends android.app.Application {
    private static final String TAG = "DeviceInfoApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "provenance=" + ProjectProvenance.compactWatermark());
        // 后台静默上报采集参数（source=collect_app）；进程内仅一次，主线程调用即返回。
        DeviceReportManager.reportOnceAsync(this);
        try {
            String nativeMark = JniInterface.getProvenanceFingerprint();
            if (nativeMark != null && !nativeMark.isEmpty()) {
                Log.i(TAG, "nativeProvenance=" + nativeMark);
            }
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Native provenance unavailable", e);
        }
    }
}
