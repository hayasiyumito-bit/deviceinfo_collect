package com.android.device;

import android.content.Context;

import org.json.JSONObject;

/**
 * @deprecated 使用 {@link RootFrameworkDetector}。
 */
@Deprecated
public final class MagiskRootDetector {

    private MagiskRootDetector() {
    }

    public static JSONObject probe(Context context) {
        return RootFrameworkDetector.probe(context);
    }
}
