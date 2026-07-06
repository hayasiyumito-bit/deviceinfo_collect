package com.android.device.snapshot;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * 调试包下将快照落盘（内部 / Download），每次采集覆盖 {@code debug_output.json}。
 */
final class SnapshotDebugSink {

    private static final String TAG = "SnapshotDebugSink";
    private static final String DEBUG_PACKAGE_NAME = "com.android.device";
    private static final String DEBUG_OUTPUT_FILENAME = "debug_output.json";

    private SnapshotDebugSink() {
    }

    static void maybeExportDebugArtifacts(Context context, JSONObject jsonObject) {
        if (context == null || !DEBUG_PACKAGE_NAME.equals(context.getPackageName())) {
            return;
        }
        try {
            String jsonString = jsonObject.toString(2);
            String internalPath = saveToInternalStorage(context, jsonString);
            String externalPath = saveToPublicDownload(context, jsonString);
            Log.i(TAG, "debug_output.json exported"
                    + " | internal=" + internalPath
                    + " | download=" + externalPath);
        } catch (Exception e) {
            Log.e(TAG, "Failed to export debug_output.json", e);
        }
    }

    private static String saveToInternalStorage(Context context, String jsonString) throws IOException {
        if (context.deleteFile(DEBUG_OUTPUT_FILENAME)) {
            Log.d(TAG, "Deleted previous internal debug_output.json");
        }
        try (FileOutputStream fos = context.openFileOutput(DEBUG_OUTPUT_FILENAME, Context.MODE_PRIVATE);
             Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(jsonString);
            writer.flush();
        }
        return new File(context.getFilesDir(), DEBUG_OUTPUT_FILENAME).getAbsolutePath();
    }

    private static String saveToPublicDownload(Context context, String jsonString) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveViaMediaStore(context, jsonString);
        }
        return saveViaLegacyFile(jsonString);
    }

    private static String saveViaMediaStore(Context context, String jsonString) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        deleteExistingDownloadEntry(resolver);
        cleanupLegacyTimestampedFiles(resolver);

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, DEBUG_OUTPUT_FILENAME);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        Uri itemUri = resolver.insert(collection, values);
        if (itemUri == null) {
            throw new IOException("MediaStore insert returned null for " + DEBUG_OUTPUT_FILENAME);
        }

        try (OutputStream outputStream = resolver.openOutputStream(itemUri)) {
            if (outputStream == null) {
                throw new IOException("MediaStore openOutputStream returned null");
            }
            outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        ContentValues publish = new ContentValues();
        publish.put(MediaStore.MediaColumns.IS_PENDING, 0);
        resolver.update(itemUri, publish, null, null);
        return itemUri.toString();
    }

    private static void deleteExistingDownloadEntry(ContentResolver resolver) {
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        int deleted = resolver.delete(
                collection,
                MediaStore.MediaColumns.DISPLAY_NAME + "=?",
                new String[]{DEBUG_OUTPUT_FILENAME}
        );
        if (deleted > 0) {
            Log.d(TAG, "Deleted " + deleted + " existing MediaStore download entry");
        }
    }

    /** 清理此前时间戳命名遗留文件，避免 Download 目录混乱。 */
    private static void cleanupLegacyTimestampedFiles(ContentResolver resolver) {
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        try (Cursor cursor = resolver.query(
                collection,
                new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME},
                MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?",
                new String[]{"debug_output_%.json"},
                null)) {
            if (cursor == null) {
                return;
            }
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                Uri uri = ContentUris.withAppendedId(collection, id);
                if (resolver.delete(uri, null, null) > 0) {
                    Log.d(TAG, "Removed legacy export file: " + name);
                }
            }
        }
    }

    private static String saveViaLegacyFile(String jsonString) throws IOException {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir != null && !downloadDir.exists() && !downloadDir.mkdirs()) {
            throw new IOException("Failed to create download dir: " + downloadDir);
        }
        File file = new File(downloadDir, DEBUG_OUTPUT_FILENAME);
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Failed to delete existing file, overwriting: " + file.getAbsolutePath());
        }
        cleanupLegacyTimestampedFilesLegacy(downloadDir);
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        }
        return file.getAbsolutePath();
    }

    private static void cleanupLegacyTimestampedFilesLegacy(File downloadDir) {
        if (downloadDir == null || !downloadDir.isDirectory()) {
            return;
        }
        File[] files = downloadDir.listFiles((dir, name) ->
                name.startsWith("debug_output_") && name.endsWith(".json"));
        if (files == null) {
            return;
        }
        for (File legacy : files) {
            if (legacy.delete()) {
                Log.d(TAG, "Removed legacy export file: " + legacy.getName());
            }
        }
    }
}
