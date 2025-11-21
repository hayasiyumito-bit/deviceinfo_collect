package com.xxxx.sentry;

import android.content.Context;

import com.android.device.DInfo;

public class Sentry {
    public final static void start(Context context, String appID, String sdkversion) {
        new Thread(() -> DInfo.getDInfo(context, appID)).start();
    }
}
