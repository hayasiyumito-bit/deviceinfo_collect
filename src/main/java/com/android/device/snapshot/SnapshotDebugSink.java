package com.android.device.snapshot;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * 调试包下将快照落盘（内部 / 外部），与采集逻辑解耦。
 */
final class SnapshotDebugSink {

    private static final String TAG = "SnapshotDebugSink";
    private static final String DEBUG_PACKAGE_NAME = "com.android.device";
    private static final String DEBUG_OUTPUT_FILENAME = "debug_output.json";
    private static final String EXTERNAL_OUTPUT_PATH = "/sdcard/Download/" + DEBUG_OUTPUT_FILENAME;

    private SnapshotDebugSink() {
    }

    static void maybeExportDebugArtifacts(Context context, JSONObject jsonObject) {
        if (!DEBUG_PACKAGE_NAME.equals(context.getPackageName())) {
            return;
        }
        try {
            String jsonString = jsonObject.toString();
            saveToInternalStorage(context, jsonString);
            saveToExternalStorage(jsonString);
        } catch (Exception e) {
            Log.w(TAG, "Failed to save debug files", e);
        }
    }

    private static void saveToInternalStorage(Context context, String jsonString) throws IOException {
        try (FileOutputStream fos = context.openFileOutput(DEBUG_OUTPUT_FILENAME, Context.MODE_PRIVATE);
             Writer writer = new OutputStreamWriter(fos)) {
            writer.write(jsonString);
            Log.d(TAG, "Debug file saved to internal storage");
        }
    }

    private static void saveToExternalStorage(String jsonString) {
        try {
            File file = new File(EXTERNAL_OUTPUT_PATH);
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "Failed to delete existing file");
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(jsonString.getBytes());
                Log.d(TAG, "Debug file saved to external storage");
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to save to external storage", e);
        }
    }
}
