#include <jni.h>
#include <string>

#include <sys/system_properties.h>
#include <android/sensor.h>
#include <android/log.h>
#include <errno.h>

#define LOG_TAG "SensorHelper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define PROPERTY_VALUE_MAX 92
extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getSystemPropertyByFind(JNIEnv *env, jclass clazz,
                                                                 jstring key) {
    // 获取Java字符串的UTF-8编码C字符串表示
    const char *keyStr = env->GetStringUTFChars(key, nullptr);
    if (keyStr == nullptr) {
        // 如果GetStringUTFChars返回nullptr，则抛出异常并返回
        return env->NewStringUTF("Error: Unable to retrieve key string");
    }

    // 分配内存以存储系统属性的结果
    char result[PROPERTY_VALUE_MAX]; // 使用Android系统定义的宏来分配足够的内存
    result[0] = '\0'; // 初始化结果为空字符串

    // 调用__system_property_get函数获取系统属性
    int len = __system_property_get(keyStr, result);
    if (len <= 0) {
        // 如果获取系统属性失败，则结果可能为空字符串或发生错误
        env->ReleaseStringUTFChars(key, keyStr); // 释放资源
        return env->NewStringUTF("Error: Property not found or unable to retrieve");
    }

    // 释放Java字符串的UTF-8编码C字符串表示所占用的资源
    env->ReleaseStringUTFChars(key, keyStr);

    // 将结果转换为jstring并返回
    jstring jStr = env->NewStringUTF(result);
    return jStr;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getLibcutilsPropertyGet(JNIEnv *env, jclass clazz,
                                                                 jstring key) {
    const char *keyStr = env->GetStringUTFChars(key, nullptr);
    if (keyStr == nullptr) {
        return env->NewStringUTF("");
    }

    char result[PROPERTY_VALUE_MAX];
    result[0] = '\0';
    int len = __system_property_get(keyStr, result);
    env->ReleaseStringUTFChars(key, keyStr);

    if (len <= 0) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(result);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_device_Jni_JniInterface_getASensorList(JNIEnv *env, jclass clazz) {
/*    ASensorManager *sensorManager = ASensorManager_getInstance();
    if (sensorManager) {
        LOGD("sensorManager not null");

        // 首先获取传感器数量
        int count = ASensorManager_getSensorList(sensorManager, nullptr);
        LOGD("sensorList count %d", count);
        if (count > 0) {
            // 分配ASensorRef数组
//            ASensorRef *sensorRefs = new ASensorRef[count];

            // 获取传感器列表
            const ASensor *const *sensorList;
            count = ASensorManager_getSensorList(sensorManager, &sensorList);
//            if (sensorList) {
//                LOGD("Sensorlist not null %d", count);
//            } else {
//                LOGD("Sensorlist null");
//            }
            // 将ASensorList转换为ASensorRef数组
//            for (int i = 0; i < count; i++) {
////                sensorRefs[i] = const_cast<ASensor *>(sensorList[i]);
//                sensorRefs[i] = sensorList[i];
//            }
            // 现在你可以使用sensorRefs数组
            for (int i = 0; i < count; i++) {
                if (sensorList[i]) {
                    LOGD("Sensor not null %d", i);
                    LOGD("Sensor name %d: %s", i, ASensor_getName(sensorList[i]));
                    LOGD("Sensor vendor %d: %s", i, ASensor_getVendor(sensorList[i]));
                } else {
                    LOGD("Sensor null %d", i);
                }
            }
            // 清理内存
//            delete[] sensorRefs;
        }
    } else {
        LOGD("sensorManager null");
    }*/
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

    jstring java_str = env->NewStringUTF("jni");
    return java_str;
}