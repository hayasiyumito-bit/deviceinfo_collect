package com.android.device;

import android.util.Log;

public class Application extends android.app.Application {
    private static final String TAG = "DeviceInfoApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application started");
    }
}
