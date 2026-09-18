# ============================================================================
# deviceinfo_collect R8 混淆规则
# release 已开启 minifyEnabled + shrinkResources，以下 keep 规则用于防止
# R8 改名/裁剪破坏「名字绑定 JNI」、追踪指纹、清单组件与检测逻辑。
# ============================================================================

# --- Provenance / 追踪指纹（商用合规追踪，勿改名） ---
-keep class com.android.device.provenance.ProjectProvenance { *; }
-keep class com.android.device.BuildConfig { *; }

# --- JNI（name-based 注册：native 符号 = 包名/类名/方法名，绝不能被 R8 改名） ---
# libdevice.so 用标准命名（Java_com_android_device_Jni_JniInterface_xxx）绑定，
# 一旦类名或方法名被混淆，getSystemPropertyByGet/getMagiskNativeProbe 等会 UnsatisfiedLink。
-keep class com.android.device.Jni.JniInterface {
    public static native <methods>;
    public static <methods>;
}
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# --- 清单组件（Application / Activity 由 R8 依清单自动保留，这里再兜底一层） ---
-keep class com.android.device.Application { *; }
-keep class com.android.device.MainActivity { *; }

# --- JSON（io.github.maven-rep:json，直接 new JSONObject/JSONArray 使用；保留避免误裁） ---
-keep class org.json.** { *; }
-dontwarn org.json.**

# --- 崩溃栈可读（保留行号，源文件名做匿名化） ---
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,Exceptions,InnerClasses
-renamesourcefileattribute SourceFile

# --- 第三方告警抑制 ---
-dontwarn com.google.android.gms.**
