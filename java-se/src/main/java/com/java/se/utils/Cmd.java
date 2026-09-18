package com.java.se.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
            Process process = Runtime.getRuntime().exec(cmd);
            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String currentLine = null;
            int count = 0;
            while ((currentLine = reader.readLine()) != null) {
                if (count > 50) {
                    break;
                }
                sb.append(currentLine);
                sb.append("\n");
                count++;
            }
            IO.close(reader);
            IO.close(inputStreamReader);
            IO.close(inputStream);
            if (process != null) {
                process.destroy();
            }
            return sb.toString();
        } catch (Exception e) {
            // 删除ULog，静默处理异常
        }
        return "";
    }

}
