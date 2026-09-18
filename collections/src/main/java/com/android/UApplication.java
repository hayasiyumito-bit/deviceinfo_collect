package com.android;

import android.app.Application;
import android.content.Context;

public class UApplication extends Application {

    private static Context S_CONTEXT;

    @Override
    public void onCreate() {
        super.onCreate();
        S_CONTEXT = this;
    }


    public static Context getContext() {
        return S_CONTEXT;
    }
}
