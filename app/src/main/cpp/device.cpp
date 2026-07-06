#include <jni.h>
#include <dlfcn.h>
#include <cstring>

#include <sys/system_properties.h>
#include <android/sensor.h>
#include <android/log.h>
#include <errno.h>

#define LOG_TAG "SensorHelper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Provenance watermark — YDC-7F3A9C2E-202607 device-collection Yumito (do not remove)
static const char kProvenanceWatermark[] =
        "YDC-7F3A9C2E-202607|device-collection|Yumito|CC-BY-NC-4.0";

#define PROPERTY_VALUE_MAX 92

static jstring read_property_get(JNIEnv *env, const char *keyStr) {
    char result[PROPERTY_VALUE_MAX];
    result[0] = '\0';
    int len = __system_property_get(keyStr, result);
    if (len <= 0) {
        return env->NewStringUTF("Error: Property not found or unable to retrieve");
    }
    return env->NewStringUTF(result);
}

static jstring read_property_find(JNIEnv *env, const char *keyStr) {
    const prop_info *pi = __system_property_find(keyStr);
    if (pi == nullptr) {
        return env->NewStringUTF("Error: Property not found or unable to retrieve");
    }
    char name[PROP_NAME_MAX];
    char value[PROP_VALUE_MAX];
    if (__system_property_read(pi, name, value) <= 0 || value[0] == '\0') {
        return env->NewStringUTF("Error: Property not found or unable to retrieve");
    }
    return env->NewStringUTF(value);
}

typedef int (*property_get_fn)(const char *, char *, const char *);

static property_get_fn resolve_libcutils_property_get() {
    static property_get_fn fn = nullptr;
    static bool resolved = false;
    if (resolved) {
        return fn;
    }
    resolved = true;
    void *handle = dlopen("libcutils.so", RTLD_NOW);
    if (handle != nullptr) {
        fn = reinterpret_cast<property_get_fn>(dlsym(handle, "property_get"));
    }
    return fn;
}

static jstring read_libcutils_property_get(JNIEnv *env, const char *keyStr) {
    property_get_fn property_get = resolve_libcutils_property_get();
    if (property_get == nullptr) {
        return env->NewStringUTF("");
    }
    char result[PROPERTY_VALUE_MAX];
    result[0] = '\0';
    int len = property_get(keyStr, result, "");
    if (len <= 0) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(result);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getSystemPropertyByGet(JNIEnv *env, jclass clazz, jstring key) {
    (void) clazz;
    const char *keyStr = env->GetStringUTFChars(key, nullptr);
    if (keyStr == nullptr) {
        return env->NewStringUTF("Error: Unable to retrieve key string");
    }
    jstring value = read_property_get(env, keyStr);
    env->ReleaseStringUTFChars(key, keyStr);
    return value;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getSystemPropertyByFind(JNIEnv *env, jclass clazz, jstring key) {
    (void) clazz;
    const char *keyStr = env->GetStringUTFChars(key, nullptr);
    if (keyStr == nullptr) {
        return env->NewStringUTF("Error: Unable to retrieve key string");
    }
    jstring value = read_property_find(env, keyStr);
    env->ReleaseStringUTFChars(key, keyStr);
    return value;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getLibcutilsPropertyGet(JNIEnv *env, jclass clazz, jstring key) {
    (void) clazz;
    const char *keyStr = env->GetStringUTFChars(key, nullptr);
    if (keyStr == nullptr) {
        return env->NewStringUTF("");
    }
    jstring value = read_libcutils_property_get(env, keyStr);
    env->ReleaseStringUTFChars(key, keyStr);
    return value;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getASensorList(JNIEnv *env, jclass clazz) {
    (void) clazz;
    FILE *fileptr = fopen("/tmp/boottime", "r");
    if (!fileptr) {
        LOGD("Error opening file: %s\n", strerror(errno));
        LOGD("file error");
    }

    FILE *fileptr2 = fopen("/tmp/boottime", "rb");
    if (!fileptr2) {
        LOGD("Error opening file: %s\n", strerror(errno));
        LOGD("file error");
    }
    FILE *fileptr3 = fopen("/tmp/boottime", "r+b");
    if (!fileptr3) {
        LOGD("Error opening file: %s\n", strerror(errno));
        LOGD("file error");
    }

    return env->NewStringUTF("jni");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getProvenanceFingerprint(JNIEnv *env, jclass clazz) {
    (void) clazz;
    return env->NewStringUTF(kProvenanceWatermark);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getNativePropertyDiagnostics(JNIEnv *env, jclass clazz) {
    (void) clazz;
    property_get_fn property_get = resolve_libcutils_property_get();
    const char *diag = property_get != nullptr
            ? "{\"jniGet\":\"__system_property_get\",\"jniFind\":\"__system_property_find+__system_property_read\",\"libcutils\":\"libcutils.property_get\"}"
            : "{\"jniGet\":\"__system_property_get\",\"jniFind\":\"__system_property_find+__system_property_read\",\"libcutils\":\"unavailable\"}";
    return env->NewStringUTF(diag);
}
