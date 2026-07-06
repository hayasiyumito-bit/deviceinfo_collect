package com.android.utils;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public final class Cmd {

    private static Method sPropGet;

    /** 防止 Xposed Hook SystemProperties.get 时重入导致死循环。 */
    private static final ThreadLocal<Boolean> IN_JAVA_PROPERTY_READ =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private Cmd() {
    }

    /**
     * 读取系统属性：优先 getprop 子进程（不经过 Java SystemProperties），避免 Hook 重入卡死。
     */
    public static String getProperty(String propName) {
        String shellValue = getPropertyViaShell(propName);
        if (!shellValue.isEmpty()) {
            return shellValue;
        }
        return getPropertyViaJavaApi(propName);
    }

    /** 仅通过 /system/bin/getprop 读取，供 Hook 对比中的 getprop 通道使用。 */
    public static String getPropertyViaShell(String propName) {
        if (propName == null || propName.isEmpty()) {
            return "";
        }
        String result = exe("getprop " + propName);
        return result != null ? result.trim() : "";
    }

    /**
     * 通过 SystemProperties Java API 读取；带重入保护，Hook 模块内部再读属性时不会死循环。
     */
    public static String getPropertyViaJavaApi(String propName) {
        if (propName == null || propName.isEmpty()) {
            return "";
        }
        if (Boolean.TRUE.equals(IN_JAVA_PROPERTY_READ.get())) {
            return "";
        }
        IN_JAVA_PROPERTY_READ.set(Boolean.TRUE);
        try {
            if (sPropGet == null) {
                sPropGet = Class.forName("android.os.SystemProperties")
                        .getMethod("get", String.class);
            }
            if (sPropGet != null) {
                Object value = sPropGet.invoke(null, propName);
                if (value instanceof String) {
                    return (String) value;
                }
            }
        } catch (ReflectiveOperationException e) {
            return "";
        } finally {
            IN_JAVA_PROPERTY_READ.set(Boolean.FALSE);
        }
        return "";
    }

    public static String exec(String command) {
        BufferedOutputStream bufferedOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        Process process = null;
        try {
            try {
                process = Runtime.getRuntime().exec("sh");
                bufferedOutputStream = new BufferedOutputStream(process.getOutputStream());
                bufferedInputStream = new BufferedInputStream(process.getInputStream());
                bufferedOutputStream.write(command.getBytes());
                bufferedOutputStream.write('\n');
                bufferedOutputStream.flush();
                bufferedOutputStream.close();

                process.waitFor();

                return getStrFromBufferInputSteam(bufferedInputStream);
            } finally {
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (process != null) {
                    process.destroy();
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String getStrFromBufferInputSteam(BufferedInputStream bufferedInputStream) {
        if (null == bufferedInputStream) {
            return "";
        }
        int BUFFER_SIZE = 512;
        byte[] buffer = new byte[BUFFER_SIZE];
        StringBuilder result = new StringBuilder();
        try {
            while (true) {
                int read = bufferedInputStream.read(buffer);
                if (read > 0) {
                    result.append(new String(buffer, 0, read));
                }
                if (read < BUFFER_SIZE) {
                    break;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return result.toString();
    }


    public static String exe(String cmd) {
        try {
            StringBuilder sb = new StringBuilder();
            String[] command = { "/bin/sh", "-c", cmd};
            Process process = Runtime.getRuntime().exec(command);
            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                sb.append(currentLine);
                sb.append("\n");
            }
            IO.close(reader);
            IO.close(inputStreamReader);
            IO.close(inputStream);

            process.destroy();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String exec_(String cmd) {
        StringBuilder output = new StringBuilder();
        int exitValue = -1001;
        try {
            Process process = Runtime.getRuntime().exec(cmd);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();

            process.waitFor();
            process.destroy();
            exitValue = process.exitValue();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return output.append("#").append(exitValue).toString();
    }
}
