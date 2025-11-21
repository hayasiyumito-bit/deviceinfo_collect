package com.android.device.env;

import com.android.utils.Cmd;

public class ServiceChecker {

    public static String getServiceList() {
        return Cmd.exe("service list");
    }

    public static String checkService(String serviceName) {
        return Cmd.exe("service check " + serviceName);
    }

}
