#include <jni.h>
#include <dlfcn.h>
#include <cstring>
#include <cstdio>
#include <unistd.h>

#include <sys/system_properties.h>
#include <android/sensor.h>
#include <android/log.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <cstdlib>

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
        "/debug_ramdisk/.magisk",
        "/data/adb/modules/zygisk",
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

// 判定 readlink 结果是否仍指向 procfs 的 maps（/proc/self/maps 会被内核解析为 /proc/<pid>/maps）。
static bool is_proc_maps_path(const char *p) {
    if (!p || strncmp(p, "/proc/", 6) != 0) {
        return false;
    }
    size_t len = strlen(p);
    return len >= 5 && strcmp(p + len - 5, "/maps") == 0;
}

// 拷贝路径到 JSON 安全字符串（转义反斜杠与双引号）。
static void copy_json_escaped(char *dst, size_t cap, const char *src) {
    size_t j = 0;
    if (cap == 0) return;
    for (size_t i = 0; src && src[i] != '\0' && j + 2 < cap; ++i) {
        char c = src[i];
        if (c == '\\' || c == '"') {
            dst[j++] = '\\';
        }
        dst[j++] = c;
    }
    dst[j] = '\0';
}

// 反检测对抗：探测 /proc 读取是否被重定向到伪造文件。
// 干净设备上 open("/proc/self/maps") 后 readlink(/proc/self/fd/N)=="/proc/self/maps"，
// 且该 fd 与 /proc 处于同一 st_dev；被 Hook 重定向到临时文件时二者均不成立。
extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getProcRedirectProbe(JNIEnv *env, jclass clazz) {
    (void) clazz;
    char json[2048];
    const char *target = "/proc/self/maps";
    char resolved[512];
    resolved[0] = '\0';
    char resolvedEsc[1024];
    resolvedEsc[0] = '\0';
    bool opened = false;
    bool redirected = false;
    bool fdOffProcFs = false;
    bool readlinkHookDetected = false;
    bool fstatHookDetected = false;
    long readBytes = -1;
    long statSize = -1;
    char rawResolved[512];
    rawResolved[0] = '\0';
    char rawResolvedEsc[1024];
    rawResolvedEsc[0] = '\0';

    int fd = open(target, O_RDONLY | O_CLOEXEC);
    if (fd >= 0) {
        opened = true;
        char linkPath[64];
        snprintf(linkPath, sizeof(linkPath), "/proc/self/fd/%d", fd);
        ssize_t n = readlink(linkPath, resolved, sizeof(resolved) - 1);
        if (n >= 0) {
            resolved[n] = '\0';
            // /proc/self/maps 打开后 fd 解析为 /proc/<pid>/maps 属正常；
            // 只有指向非 procfs 路径（临时文件）才是重定向。
            if (!is_proc_maps_path(resolved)) {
                redirected = true;
            }
        }
        struct stat fdst;
        struct stat procst;
        memset(&fdst, 0, sizeof(fdst));
        memset(&procst, 0, sizeof(procst));
        if (fstat(fd, &fdst) == 0 && stat("/proc", &procst) == 0) {
            if (fdst.st_dev != procst.st_dev) {
                fdOffProcFs = true;
            }
        }

        // 反制反检测：用 raw syscall 绕过对 libc readlink/fstat 的 Hook，与 libc 结果交叉核对。
        // 反检测模块通常只 Hook libc 符号，未拦截原始 syscall，故二者不一致即暴露伪造。
#if defined(SYS_readlinkat)
        {
            long rn = syscall(SYS_readlinkat, AT_FDCWD, linkPath,
                              rawResolved, (long) (sizeof(rawResolved) - 1));
            if (rn >= 0) {
                rawResolved[rn] = '\0';
                if (!is_proc_maps_path(rawResolved)) {
                    redirected = true;  // 原始 syscall 看到非 procfs 目标（临时文件）
                }
                if (strcmp(rawResolved, resolved) != 0) {
                    readlinkHookDetected = true;  // libc 与 raw 不一致 → readlink 被篡改
                }
            }
        }
#endif
#if defined(__aarch64__) && defined(SYS_fstat)
        {
            struct stat rawst;
            struct stat libst;
            memset(&rawst, 0, sizeof(rawst));
            memset(&libst, 0, sizeof(libst));
            if (syscall(SYS_fstat, fd, &rawst) == 0) {
                if (fstat(fd, &libst) == 0) {
                    if (libst.st_dev != rawst.st_dev || libst.st_size != rawst.st_size) {
                        fstatHookDetected = true;  // libc fstat 被伪造为 procfs/size0
                    }
                }
                if (rawst.st_size > 0) {
                    fdOffProcFs = true;  // 句柄背后是普通文件，/proc 虚拟文件恒为 0
                }
            }
        }
#endif

        char buf[256];
        ssize_t r = read(fd, buf, sizeof(buf));
        readBytes = (r >= 0) ? (long) r : -1;
        close(fd);
    }

    struct stat mst;
    memset(&mst, 0, sizeof(mst));
    if (stat(target, &mst) == 0) {
        statSize = (long) mst.st_size;
    }

    copy_json_escaped(resolvedEsc, sizeof(resolvedEsc), resolved);
    copy_json_escaped(rawResolvedEsc, sizeof(rawResolvedEsc), rawResolved);
    // 真实 /proc/self/maps 的 st_size 恒为 0；非 0 说明 stat 被重定向到普通文件。
    bool statSizeAnomaly = (statSize > 0);
    bool anomaly = redirected || fdOffProcFs || statSizeAnomaly
                   || readlinkHookDetected || fstatHookDetected;

    snprintf(json, sizeof(json),
             "{\"opened\":%s,\"mapsFdTarget\":\"%s\",\"rawFdTarget\":\"%s\","
             "\"mapsRedirected\":%s,\"mapsFdOffProcFs\":%s,"
             "\"readlinkHookDetected\":%s,\"fstatHookDetected\":%s,"
             "\"statSize\":%ld,\"statSizeAnomaly\":%s,"
             "\"readBytes\":%ld,\"anomaly\":%s}",
             opened ? "true" : "false",
             resolvedEsc,
             rawResolvedEsc,
             redirected ? "true" : "false",
             fdOffProcFs ? "true" : "false",
             readlinkHookDetected ? "true" : "false",
             fstatHookDetected ? "true" : "false",
             statSize,
             statSizeAnomaly ? "true" : "false",
             readBytes,
             anomaly ? "true" : "false");
    return env->NewStringUTF(json);
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

// ============================================================================
// 反 Hook 终极探针：用 inline svc 系统调用直接读取 /proc/self/maps 原文，
// 绕过反检测模块对 libc open/openat/read 的符号级 Hook（如 YumyHook 的
// shadowhook + 读过滤/临时文件重定向），把被隐藏的注入库暴露出来。
// 只 Hook libc 符号的模块无法拦截真正的 svc #0，故 inline-svc 读到的是内核真相。
// ============================================================================
#if defined(__aarch64__)
static inline long yh_svc(long nr, long a0, long a1, long a2, long a3) {
    register long x8 asm("x8") = nr;
    register long x0 asm("x0") = a0;
    register long x1 asm("x1") = a1;
    register long x2 asm("x2") = a2;
    register long x3 asm("x3") = a3;
    asm volatile("svc #0" : "+r"(x0) : "r"(x8), "r"(x1), "r"(x2), "r"(x3)
                 : "memory", "cc");
    return x0;
}
#define YH_HAVE_SVC 1
#define YH_NR_OPENAT 56
#define YH_NR_READ   63
#define YH_NR_CLOSE  57
#endif

// 读取整文件到 buf（末尾补 '\0'）。useSvc=true 走 inline svc（绕 libc），
// 否则走 libc open/read（即反检测模块看得见、会过滤的那条路）。返回字节数，失败 -1。
static long yh_read_file_all(const char *path, char *buf, long cap, bool useSvc) {
    int fd = -1;
#ifdef YH_HAVE_SVC
    if (useSvc) {
        fd = (int) yh_svc(YH_NR_OPENAT, (long) -100 /*AT_FDCWD*/, (long) path,
                          (long) (O_RDONLY | O_CLOEXEC), 0);
    } else
#endif
    {
        fd = open(path, O_RDONLY | O_CLOEXEC);
    }
    if (fd < 0) return -1;

    long total = 0;
    while (total < cap - 1) {
        long n;
#ifdef YH_HAVE_SVC
        if (useSvc) {
            n = yh_svc(YH_NR_READ, fd, (long) (buf + total), (long) (cap - 1 - total), 0);
        } else
#endif
        {
            n = (long) read(fd, buf + total, (size_t) (cap - 1 - total));
        }
        if (n <= 0) break;
        total += n;
    }
    buf[total > 0 ? total : 0] = '\0';

#ifdef YH_HAVE_SVC
    if (useSvc) yh_svc(YH_NR_CLOSE, fd, 0, 0, 0);
    else
#endif
    close(fd);
    return total;
}

static int yh_count_lines(const char *s) {
    int c = 0;
    for (const char *p = s; *p; ++p) if (*p == '\n') c++;
    return c;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getInlineSvcMapsProbe(JNIEnv *env, jclass clazz) {
    (void) clazz;
    const long CAP = 2 * 1024 * 1024;
    char *rawBuf = (char *) malloc(CAP);
    char *libcBuf = (char *) malloc(CAP);
    if (!rawBuf || !libcBuf) {
        free(rawBuf);
        free(libcBuf);
        return env->NewStringUTF("{\"error\":\"oom\"}");
    }

#ifdef YH_HAVE_SVC
    const char *arch = "aarch64";
    bool svcUsed = true;
#else
    const char *arch = "other";
    bool svcUsed = false;  // 非 aarch64 退化为 libc（无 svc 旁路，仅尽力而为）
#endif
    long rawN = yh_read_file_all("/proc/self/maps", rawBuf, CAP, svcUsed);
    long libcN = yh_read_file_all("/proc/self/maps", libcBuf, CAP, false);

    // 第三路：fopen（反检测模块最常拦截的经典 API），诊断哪条 libc 路径被过滤
    char *fopenBuf = (char *) malloc(CAP);
    long fopenN = -1;
    if (fopenBuf) {
        FILE *fp = fopen("/proc/self/maps", "r");
        if (fp) {
            long t = 0;
            size_t r;
            while (t < CAP - 1 && (r = fread(fopenBuf + t, 1, (size_t) (CAP - 1 - t), fp)) > 0) t += r;
            fopenBuf[t > 0 ? t : 0] = '\0';
            fopenN = t;
            fclose(fp);
        } else {
            fopenBuf[0] = '\0';
        }
    }

    // 注入指纹：YumyHook 原生库 / shadowhook 引擎 / LSPosed / Zygisk / Riru / Xposed
    static const char *tokens[] = {
            "libyumyhook_native.so", "yumyhook_native", "libshadowhook.so",
            "shadowhook-enter", "shadowhook-exit", "shadowhook-hub",
            "com.yumito.yumyhook", "/data/adb/lspd", "zygisk", "riru",
            "libxposed", nullptr
    };

    char json[8192];
    int off = snprintf(json, sizeof(json),
                       "{\"arch\":\"%s\",\"svcBypass\":%s,\"rawBytes\":%ld,\"libcBytes\":%ld,\"fopenBytes\":%ld,"
                       "\"rawLines\":%d,\"libcLines\":%d,",
                       arch, svcUsed ? "true" : "false", rawN, libcN, fopenN,
                       yh_count_lines(rawBuf), yh_count_lines(libcBuf));

    bool hooked = false, concealed = false;
    bool rawHas[32] = {false}, libcHas[32] = {false}, fopenHas[32] = {false};
    for (int i = 0; tokens[i]; i++) {
        rawHas[i] = (rawN > 0) && (strstr(rawBuf, tokens[i]) != nullptr);
        libcHas[i] = (libcN > 0) && (strstr(libcBuf, tokens[i]) != nullptr);
        fopenHas[i] = fopenBuf && (fopenN > 0) && (strstr(fopenBuf, tokens[i]) != nullptr);
    }

    // 结构信号：扫 inline-svc 真 maps 里的 rwx(可写可执行)页——违反 W^X。
    // 文件映射的系统库本应 r-xp，出现 rwxp = 被 inline-hook 打了补丁(shadowhook)；
    // 匿名 rwx = 注入的蹦床/跳板。改名藏不住权限位，是 round9/10 之后仍暴露的破绽。
    int rwxAnon = 0, rwxFile = 0;
    for (const char *p = rawBuf; p && *p;) {
        const char *eol = strchr(p, '\n');
        size_t len = eol ? (size_t) (eol - p) : strlen(p);
        const char *sp = (const char *) memchr(p, ' ', len);
        if (sp && (size_t) (sp - p) + 4 < len) {
            const char *perms = sp + 1;  // "rwxp"
            if (perms[1] == 'w' && perms[2] == 'x') {
                bool fileBacked = false;
                for (const char *q = perms; q < p + len; q++) {
                    if (*q == '/') { fileBacked = true; break; }
                }
                if (fileBacked) rwxFile++; else rwxAnon++;
            }
        }
        if (!eol) break;
        p = eol + 1;
    }
    bool patched = (rwxFile > 0) || (rwxAnon > 0);

    off += snprintf(json + off, sizeof(json) - off, "\"rawHits\":[");
    bool first = true;
    for (int i = 0; tokens[i]; i++) {
        if (rawHas[i]) {
            off += snprintf(json + off, sizeof(json) - off, "%s\"%s\"",
                            first ? "" : ",", tokens[i]);
            first = false;
            hooked = true;
        }
    }
    off += snprintf(json + off, sizeof(json) - off, "],\"libcHits\":[");
    first = true;
    for (int i = 0; tokens[i]; i++) {
        if (libcHas[i]) {
            off += snprintf(json + off, sizeof(json) - off, "%s\"%s\"",
                            first ? "" : ",", tokens[i]);
            first = false;
        }
    }
    off += snprintf(json + off, sizeof(json) - off, "],\"fopenHits\":[");
    first = true;
    for (int i = 0; tokens[i]; i++) {
        if (fopenHas[i]) {
            off += snprintf(json + off, sizeof(json) - off, "%s\"%s\"",
                            first ? "" : ",", tokens[i]);
            first = false;
        }
    }
    // 被隐藏 = inline-svc 看得见，但某条 libc 路径（open 或 fopen）看不见
    off += snprintf(json + off, sizeof(json) - off, "],\"hiddenFromLibc\":[");
    first = true;
    for (int i = 0; tokens[i]; i++) {
        if (rawHas[i] && (!libcHas[i] || !fopenHas[i])) {
            off += snprintf(json + off, sizeof(json) - off, "%s\"%s\"",
                            first ? "" : ",", tokens[i]);
            first = false;
            concealed = true;
        }
    }
    snprintf(json + off, sizeof(json) - off,
             "],\"rwxAnon\":%d,\"rwxFile\":%d,\"patched\":%s,\"hooked\":%s,\"concealed\":%s}",
             rwxAnon, rwxFile, patched ? "true" : "false",
             (hooked || patched) ? "true" : "false", concealed ? "true" : "false");

    free(rawBuf);
    free(libcBuf);
    free(fopenBuf);
    return env->NewStringUTF(json);
}
