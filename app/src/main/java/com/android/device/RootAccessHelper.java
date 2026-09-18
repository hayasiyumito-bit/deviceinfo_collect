package com.android.device;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通过 su 探测 Root；每次采集前可重新尝试，结果仅当次有效，不做 SP/持久化缓存。
 */
public final class RootAccessHelper {

    private static final String TAG = "RootAccessHelper";
    private static final long SU_TIMEOUT_MS = 3500L;

    private static final AtomicBoolean attemptStarted = new AtomicBoolean(false);
    private static final AtomicBoolean attemptFinished = new AtomicBoolean(false);
    private static final AtomicBoolean rootGranted = new AtomicBoolean(false);
    private static final AtomicReference<String> attemptDetail = new AtomicReference<>("未尝试");
    private static final AtomicLong attemptSequence = new AtomicLong(0L);

    private static volatile CountDownLatch attemptLatch = new CountDownLatch(0);

    private RootAccessHelper() {
    }

    /** 每次全量采集前调用，重置状态并重新尝试 su。 */
    public static void beginFreshAttempt() {
        synchronized (RootAccessHelper.class) {
            attemptSequence.incrementAndGet();
            attemptStarted.set(false);
            attemptFinished.set(false);
            rootGranted.set(false);
            attemptDetail.set("尝试 su 中…");
            attemptLatch = new CountDownLatch(1);
        }
        startAttemptAsync();
    }

    private static void startAttemptAsync() {
        if (!attemptStarted.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(RootAccessHelper::runAttempt, "root-access-probe");
        thread.setDaemon(true);
        thread.start();
    }

    public static void awaitAttempt(long timeoutMs) {
        CountDownLatch latch = attemptLatch;
        if (latch == null) {
            return;
        }
        try {
            latch.await(Math.max(0L, timeoutMs), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean isAttemptFinished() {
        return attemptFinished.get();
    }

    public static boolean isRootGranted() {
        return rootGranted.get();
    }

    public static String getAttemptDetail() {
        return attemptDetail.get();
    }

    public static long getAttemptSequence() {
        return attemptSequence.get();
    }

    private static void runAttempt() {
        try {
            if (trySuOneShot("su -c id")) {
                markGranted("su -c id 成功");
                return;
            }
            if (trySuOneShot("su 0 id")) {
                markGranted("su 0 id 成功");
                return;
            }
            if (trySuInteractive()) {
                markGranted("su 交互式 id 成功");
                return;
            }
            markDenied("未获取 Root（su 不可用或已拒绝）");
        } catch (Throwable t) {
            markDenied("未获取 Root");
            Log.d(TAG, "Root probe skipped: " + t.getMessage());
        }
    }

    private static void markGranted(String detail) {
        rootGranted.set(true);
        attemptDetail.set(detail);
        finishAttempt();
        Log.d(TAG, detail);
    }

    private static void markDenied(String detail) {
        rootGranted.set(false);
        attemptDetail.set(detail);
        finishAttempt();
        Log.d(TAG, detail);
    }

    private static void finishAttempt() {
        attemptFinished.set(true);
        CountDownLatch latch = attemptLatch;
        if (latch != null) {
            latch.countDown();
        }
    }

    private static boolean trySuOneShot(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", command});
            if (!process.waitFor(SU_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroy();
                return false;
            }
            if (process.exitValue() != 0) {
                return false;
            }
            String output = readStream(process);
            return outputContainsRootUid(output);
        } catch (IOException | InterruptedException | SecurityException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        } finally {
            destroyQuietly(process);
        }
    }

    private static boolean trySuInteractive() {
        Process process = null;
        DataOutputStream stdin = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/bin/su"});
            stdin = new DataOutputStream(process.getOutputStream());
            stdin.writeBytes("id\n");
            stdin.writeBytes("exit\n");
            stdin.flush();
            if (!process.waitFor(SU_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroy();
                return false;
            }
            if (process.exitValue() != 0) {
                return false;
            }
            String output = readStream(process);
            return outputContainsRootUid(output);
        } catch (IOException | InterruptedException | SecurityException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        } finally {
            closeQuietly(stdin);
            destroyQuietly(process);
        }
    }

    private static String readStream(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        return output.toString();
    }

    private static boolean outputContainsRootUid(String output) {
        if (output == null || output.isEmpty()) {
            return false;
        }
        return output.contains("uid=0") || output.contains("(root)");
    }

    private static void destroyQuietly(Process process) {
        if (process == null) {
            return;
        }
        try {
            process.destroy();
        } catch (Throwable ignored) {
        }
    }

    private static void closeQuietly(DataOutputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }
}
