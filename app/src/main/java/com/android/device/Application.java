package com.android.device;

import android.util.Log;

import com.android.device.Jni.JniInterface;
import com.android.device.provenance.ProjectProvenance;

public class Application extends android.app.Application {
    private static final String TAG = "DeviceInfoApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "provenance=" + ProjectProvenance.compactWatermark());
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
