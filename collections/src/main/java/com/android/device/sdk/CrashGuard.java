package com.android.device.sdk;

import android.content.Context;
import android.os.Build;

import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * App 崩溃捕获与上报。
 *
 * <p>设计：<b>崩溃时先落盘、下次启动再上报</b>——在正在崩溃的进程里发网络极不可靠，
 * 故未捕获异常时只把一条崩溃信封写到私有目录，并**链式回调原 handler**让 App 照常崩；
 * 下次进程启动时由 {@link #uploadPending} 扫描目录补传到服务端 {@code /api/v1/crash}。
 *
 * <p>崩溃信封用 {@link DeviceCollectSdk#deviceId} 标记设备——与采集上报**同一 device_id**，
 * 因此服务端可按 device_id 把「崩溃 ← → 机型/采集数据」关联起来。
 *
 * <p>用法（宿主 App 启动时，主线程即可）：
 * <pre>{@code
 * ReportConfig cfg = new ReportConfig(baseUrl, apiKey)
 *         .appPackage(BuildConfig.APPLICATION_ID).appVersion(BuildConfig.VERSION_NAME)
 *         .source("yumyhook");
 * CrashGuard.install(context, cfg);           // 装崩溃捕获（幂等）
 * CrashGuard.uploadPending(context, cfg);     // 后台补传上次崩溃（内部起线程）
 * }</pre>
 */
public final class CrashGuard {

    private static final String DIR = "device_crash";
    private static final String SUFFIX = ".json";
    /** 待传崩溃文件数量上限，超出丢最旧的，防异常风暴撑爆磁盘。 */
    private static final int MAX_PENDING = 50;
    /** 单次启动最多补传条数，避免堵塞。 */
    private static final int MAX_UPLOAD_PER_RUN = 20;
    /** 超过此天数仍没传上去的崩溃文件直接丢弃（陈旧无意义）。 */
    private static final long STALE_MS = 14L * 86400_000L;

    private static volatile boolean installed = false;

    private CrashGuard() {}

    /** 安装未捕获异常捕获器（进程内幂等；自动链式回调原 handler）。 */
    public static void install(final Context context, final ReportConfig config) {
        if (installed || context == null || config == null) {
            return;
        }
        synchronized (CrashGuard.class) {
            if (installed) {
                return;
            }
            final Context app = context.getApplicationContext();
            final Thread.UncaughtExceptionHandler prev =
                    Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    try {
                        persist(app, config, thread, ex);
                    } catch (Throwable ignore) {
                        // 捕获器内绝不再抛，避免掩盖原始崩溃 / 二次崩溃。
                    }
                    // 链式回调系统原 handler（RuntimeInit$KillApplicationHandler），让 App 正常崩溃。
                    if (prev != null) {
                        prev.uncaughtException(thread, ex);
                    } else {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(10);
                    }
                }
            });
            installed = true;
        }
    }

    /** 把一条崩溃信封写到私有目录（供下次启动补传）。 */
    private static void persist(Context app, ReportConfig config, Thread thread, Throwable ex) {
        File dir = new File(app.getFilesDir(), DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        pruneForCapacity(dir);

        StringWriter sw = new StringWriter(4096);
        ex.printStackTrace(new PrintWriter(sw));
        String stack = sw.toString();
        if (stack.length() > 200_000) {          // 堆栈护栏，防超大
            stack = stack.substring(0, 200_000);
        }

        JSONObject o = new JSONObject();
        try {
            o.put("schema", DeviceCollectSdk.SCHEMA_VERSION);
            o.put("sdk_version", DeviceCollectSdk.SDK_VERSION);
            o.put("device_id", DeviceCollectSdk.deviceId(app));
            if (config.source() != null && !config.source().isEmpty()) {
                o.put("source", config.source());
            }
            o.put("app_package", config.appPackage());
            o.put("app_version", config.appVersion());
            // 自崩：崩在本 App。宿主注入场景由调用方通过 config.appPackage() 传宿主包名即可对应。
            o.put("hooked_package", config.appPackage());
            o.put("process", processName(app));
            o.put("thread", thread != null ? thread.getName() : null);
            o.put("fatal", true);
            o.put("exception", ex.getClass().getName());
            o.put("message", ex.getMessage());
            o.put("stacktrace", stack);
            o.put("device_model", Build.MANUFACTURER + " " + Build.MODEL);
            o.put("android_sdk", Build.VERSION.SDK_INT);
            o.put("crashed_at", System.currentTimeMillis());
        } catch (Exception e) {
            return;
        }

        File tmp = new File(dir, System.currentTimeMillis() + SUFFIX + ".tmp");
        File dst = new File(dir, System.currentTimeMillis() + "_" + Math.abs(o.hashCode()) + SUFFIX);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
            fos.write(o.toString().getBytes("UTF-8"));
            fos.flush();
            fos.getFD().sync();                 // 崩溃在即，强制落盘
        } catch (Exception e) {
            tmp.delete();
            return;
        }
        if (!tmp.renameTo(dst)) {               // 原子提交
            tmp.delete();
        }
    }

    /** 后台补传上次崩溃：扫描目录逐条 POST，成功即删；顺带丢弃陈旧文件。 */
    public static void uploadPending(final Context context, final ReportConfig config) {
        if (context == null || config == null) {
            return;
        }
        final Context app = context.getApplicationContext();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    uploadPendingSync(app, config);
                } catch (Throwable ignore) {
                }
            }
        }, "device-crash-upload");
        t.setDaemon(true);
        t.start();
    }

    /** 同步补传（供调用方已在工作线程时直接用）。 */
    public static void uploadPendingSync(Context context, ReportConfig config) {
        File dir = new File(context.getApplicationContext().getFilesDir(), DIR);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        java.util.Arrays.sort(files);           // 按文件名(时间戳前缀)升序，先传最旧
        long now = System.currentTimeMillis();
        int sent = 0;
        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".tmp")) {        // 半成品清掉
                f.delete();
                continue;
            }
            if (!name.endsWith(SUFFIX)) {
                continue;
            }
            if (now - f.lastModified() > STALE_MS) {
                f.delete();
                continue;
            }
            if (sent >= MAX_UPLOAD_PER_RUN) {
                break;
            }
            JSONObject env = readJson(f);
            if (env == null) {                  // 坏文件丢弃
                f.delete();
                continue;
            }
            ReportClient.Result r = ReportClient.postTo(config, config.crashEndpoint(), env);
            // 2xx（含 204）视为成功；400（服务端明确拒收，如格式错）也删，避免反复重传坏数据。
            if (r.ok || r.httpCode == 400) {
                f.delete();
            }
            // 其它（网络失败/5xx）保留，下次再传。
            sent++;
        }
    }

    /** 数量超限时删最旧，给新崩溃腾位。 */
    private static void pruneForCapacity(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length < MAX_PENDING) {
            return;
        }
        java.util.Arrays.sort(files);           // 最旧在前
        int toDelete = files.length - MAX_PENDING + 1;
        for (int i = 0; i < toDelete && i < files.length; i++) {
            files[i].delete();
        }
    }

    private static JSONObject readJson(File f) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return new JSONObject(bos.toString("UTF-8"));
        } catch (Exception e) {
            return null;
        }
    }

    private static String processName(Context app) {
        try {
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.FileReader("/proc/self/cmdline"));
            try {
                String line = r.readLine();
                if (line != null) {
                    String s = line.trim().replace("\0", "");
                    if (!s.isEmpty()) {
                        return s;
                    }
                }
            } finally {
                r.close();
            }
        } catch (Exception ignore) {
        }
        return app.getPackageName();
    }
}
