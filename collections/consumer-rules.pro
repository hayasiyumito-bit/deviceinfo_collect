# 随 AAR 传递给消费方(如 YumyHook)的 R8 保留规则。
# JNI 通过类名/方法名与 libdevice.so 绑定，被 R8 改名即 UnsatisfiedLinkError / NoClassDefFound。
-keep class com.android.device.Jni.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
# 风控入口经反射/JNI 交叉引用，保守保留其公开入口。
-keep class com.android.device.sdk.** { *; }
-keep class com.android.device.SecurityReportComposer { *; }
-keep class com.android.device.snapshot.DeviceSnapshot { *; }
