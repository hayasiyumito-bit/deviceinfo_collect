#include <jni.h>
#include <dlfcn.h>
#include <cstring>
#include <cstdio>
#include <unistd.h>

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
        return env->NewStringUTF("Error: libcutils property_get unavailable");
    }
    char result[PROPERTY_VALUE_MAX];
    result[0] = '\0';
    int len = property_get(keyStr, result, "");
    if (len <= 0) {
        return env->NewStringUTF("Error: Property not found or unable to retrieve");
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
        return env->NewStringUTF("Error: Unable to retrieve key string");
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

static const char *kNativeMagiskPaths[] = {
        "/sbin/magisk",
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/data/adb/magisk.db",
        "/data/adb/modules",
        "/data/adb/ksu",
        "/data/adb/kernelsu",
        "/data/adb/ap",
        "/data/adb/apd",
        "/debug_ramdisk/magisk",
        nullptr
};

static const char *kNativeMagiskKeywords[] = {
        "magisk", "zygisk", "magiskpolicy", "kernelsu", "ksud",
        "apatch", "/data/adb/ap", "/data/adb/apd", "supersu", "daemonsu", nullptr
};

static bool line_contains_keyword(const char *line, const char *keyword) {
    if (line == nullptr || keyword == nullptr) {
        return false;
    }
    if (keyword[0] == '/') {
        return strstr(line, keyword) != nullptr;
    }
    if (strlen(keyword) < 5) {
        return false;
    }
    return strstr(line, keyword) != nullptr;
}

static void append_json_string(char *buf, size_t cap, const char *value, bool *first) {
    if (value == nullptr || buf == nullptr || first == nullptr) {
        return;
    }
    size_t len = strnlen(buf, cap);
    int written = snprintf(buf + len, cap - len, "%s\"%s\"", *first ? "" : ",", value);
    if (written > 0 && static_cast<size_t>(written) < cap - len) {
        *first = false;
    }
}

static void append_keyword_hits(const char *path, const char **keywords, char *out, size_t cap) {
    strncat(out, "[", cap - strlen(out) - 1);
    FILE *fp = fopen(path, "r");
    if (fp == nullptr) {
        strncat(out, "]", cap - strlen(out) - 1);
        return;
    }
    char line[512];
    bool first = true;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        for (int i = 0; keywords[i] != nullptr; ++i) {
            if (line_contains_keyword(line, keywords[i])) {
                append_json_string(out, cap, keywords[i], &first);
                break;
            }
        }
    }
    fclose(fp);
    strncat(out, "]", cap - strlen(out) - 1);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getMagiskNativeProbe(JNIEnv *env, jclass clazz) {
    (void) clazz;
    char json[4096];
    snprintf(json, sizeof(json), "{\"accessiblePaths\":[");
    bool first = true;
    for (int i = 0; kNativeMagiskPaths[i] != nullptr; ++i) {
        if (access(kNativeMagiskPaths[i], F_OK) == 0) {
            append_json_string(json, sizeof(json), kNativeMagiskPaths[i], &first);
        }
    }
    strncat(json, "],\"mapsHits\":", sizeof(json) - strlen(json) - 1);
    size_t offset = strlen(json);
    append_keyword_hits("/proc/self/maps", kNativeMagiskKeywords, json + offset,
                        sizeof(json) - offset);
    strncat(json, ",\"mountHits\":", sizeof(json) - strlen(json) - 1);
    offset = strlen(json);
    append_keyword_hits("/proc/self/mountinfo", kNativeMagiskKeywords, json + offset,
                        sizeof(json) - offset);
    strncat(json, "}", sizeof(json) - strlen(json) - 1);
    return env->NewStringUTF(json);
}
