package com.android.device.Jni;

public class JniInterface {
    static {
       System.loadLibrary("device");
    }

    public static native String getSystemPropertyByFind(String key);

    public static native String getLibcutilsPropertyGet(String key);

    public static native String getASensorList();

    /** Native 溯源水印，供产物追踪。Fingerprint: YDC-7F3A9C2E-202607 */
    public static native String getProvenanceFingerprint();
}
