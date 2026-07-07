package com.android.device.Jni;

public class JniInterface {
    static {
       System.loadLibrary("device");
    }

    /** Native __system_property_get */
    public static native String getSystemPropertyByGet(String key);

    /** Native __system_property_find + __system_property_read */
    public static native String getSystemPropertyByFind(String key);

    /** Native libcutils property_get */
    public static native String getLibcutilsPropertyGet(String key);

    public static native String getASensorList();

    /** Native 溯源水印，供产物追踪。Fingerprint: YDC-7F3A9C2E-202607 */
    public static native String getProvenanceFingerprint();

    /** 返回各 JNI 通道实际调用的 native API 说明（JSON 字符串）。 */
    public static native String getNativePropertyDiagnostics();

    /** Native 层 Magisk/Root 路径与 maps/mount 探测（JSON）。 */
    public static native String getMagiskNativeProbe();
}
