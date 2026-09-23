package com.android.device.sdk;

import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

/**
 * 极简 HTTP 上报客户端(无第三方依赖,基于 {@link HttpURLConnection})。
 * 同步阻塞;POST JSON 到 {@link ReportConfig#endpoint()},带 {@code X-Api-Key},可选 gzip。
 */
public final class ReportClient {

    private static final String TAG = "DeviceCollectReport";

    private ReportClient() {}

    /** 上报结果。 */
    public static final class Result {
        public final boolean ok;
        public final int httpCode;
        public final String message;

        Result(boolean ok, int httpCode, String message) {
            this.ok = ok;
            this.httpCode = httpCode;
            this.message = message;
        }

        @Override public String toString() {
            return "Result{ok=" + ok + ", http=" + httpCode + ", msg=" + message + '}';
        }
    }

    static Result post(ReportConfig config, JSONObject envelope) {
        HttpURLConnection conn = null;
        try {
            byte[] body = envelope.toString().getBytes("UTF-8");
            boolean gzip = config.gzip();
            if (gzip) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                GZIPOutputStream gz = new GZIPOutputStream(bos);
                gz.write(body);
                gz.close();
                body = bos.toByteArray();
            }

            URL url = new URL(config.endpoint());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(config.connectTimeoutMs());
            conn.setReadTimeout(config.readTimeoutMs());
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("X-Api-Key", config.apiKey());
            if (gzip) {
                conn.setRequestProperty("Content-Encoding", "gzip");
            }
            conn.setFixedLengthStreamingMode(body.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int code = conn.getResponseCode();
            boolean ok = code >= 200 && code < 300;
            String resp = readStream(ok ? conn.getInputStream() : conn.getErrorStream());
            if (!ok) {
                Log.w(TAG, "report failed http=" + code + " resp=" + resp);
            }
            return new Result(ok, code, resp);
        } catch (Exception e) {
            Log.w(TAG, "report error", e);
            return new Result(false, -1, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readStream(InputStream in) {
        if (in == null) {
            return "";
        }
        try (InputStream s = in) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = s.read(buf)) != -1) {
                bos.write(buf, 0, n);
                if (bos.size() > 64 * 1024) {
                    break;
                }
            }
            return bos.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
