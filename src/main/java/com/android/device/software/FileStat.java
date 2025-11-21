package com.android.device.software;

import android.system.ErrnoException;
import android.system.Os;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class FileStat {
    /**
     * 根据文件路径获取文件信息并封装为JSON对象
     *
     * @param path 文件路径
     * @return 包含文件信息的JSON对象
     * @throws JSONException JSON异常
     * @throws IOException IO异常
     * @throws ErrnoException 错误号异常
     */
    public static JSONObject getFileByPath(String path) {
        JSONObject jsonObject = new JSONObject();
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                long Modified_Time = file.lastModified();
                long size = file.length();
                FileInputStream fis = new FileInputStream(file);
                FileDescriptor fd = fis.getFD();
                long inode = Os.fstat(fd).st_ino;
                long uid = Os.fstat(fd).st_uid;
                jsonObject.put("uid", uid);
                jsonObject.put("inode", inode);
                jsonObject.put("size", size);
                jsonObject.put("path", path);
                jsonObject.put("modified_time", Modified_Time);
            }
        } catch (JSONException | IOException | ErrnoException e) {
            return jsonObject;
        }
        return jsonObject;
    }

    /**
     * 获取指定路径下的文件统计信息
     *
     * @return 包含文件统计信息的JSONArray对象
     */
    public static JSONArray getFileStat() {
        JSONArray array = new JSONArray();
        array.put(getFileByPath("/system/framework/services.jar"));
        array.put(getFileByPath("/system/lib/libandroid_runtime.so"));
        array.put(getFileByPath("/system/lib/libandroid_servers.so"));
        array.put(getFileByPath("/system/lib/libc.so"));
        array.put(getFileByPath("/system/framework/framework.jar"));
        array.put(getFileByPath("/system/lib/libandroid.so"));
        array.put(getFileByPath("/system/framework/ext.jar"));
        array.put(getFileByPath("/default.prop"));
        array.put(getFileByPath("/system/build.prop"));
        return array;
    }
}
