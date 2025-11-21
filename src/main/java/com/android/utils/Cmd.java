package com.android.utils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;

public final class Cmd {

    private static Method sPropGet;

    public static String getProperty(String propName) {
        String value = null;
        Object roSecureObj;
        try {
            if (sPropGet == null) {
                sPropGet = Class.forName("android.os.SystemProperties")
                        .getMethod("get", String.class);
            }
            if (sPropGet != null) {
                roSecureObj = sPropGet.invoke(null, propName);
                if (roSecureObj != null) {
                    value = (String) roSecureObj;
                }
            }
        } catch (Exception e) {
            value = null;
        }
        return value;
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
            ULog.e(e);
            return "";
        }
    }

    public static String exec_(String cmd) {
        StringBuilder output = new StringBuilder();
        int exitValue = -1001;
        try {
            // 创建一个进程来执行命令
            Process process = Runtime.getRuntime().exec(cmd);

            // 读取命令的输出
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            reader.close();

            // 等待进程结束
            process.waitFor();
            process.destroy();
            // 获取命令的退出值
            exitValue = process.exitValue();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return output.append("#").append(exitValue).toString();
    }
}
