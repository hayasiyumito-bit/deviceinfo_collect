package com.android.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.InvalidAlgorithmParameterException;
import java.io.UnsupportedEncodingException;

import android.util.Base64;


public class Http {
    private static final String TAG = "HttpTAG";
    public static final String BAD_CONNECT = "bad_connect";
    private static final String ENCRYPT_KEY = "WLkc8lqw!z%C*F-X";
    private static final String ENCRYPT_IV = "fqqtj4w9cj0qkTG8";
    private static final int CONNECTION_TIMEOUT = 3000;

    private static Cipher getCipher(int mode, String key, String iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CFB/NOPadding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
        cipher.init(mode, skeySpec, ivParameterSpec);
        return cipher;
    }

    public static byte[] encrypt(String key, String iv, byte[] data) {
        boolean condition = (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(iv) && (data.length > 0));
        if (!condition) {
            throw new IllegalArgumentException();
        }
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, key, iv);
            return cipher.doFinal(data);
        } catch (Exception e) {
            Log.e("deviceinfo", "http encrypt exception.");
            return null;
        }
    }

    public static String encrypt(String key, String iv, String data) throws UnsupportedEncodingException {
        boolean condition = (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(iv) && !TextUtils.isEmpty(data));
        if (!condition) {
            throw new IllegalArgumentException();
        }
        byte[] encrypt = encrypt(key, iv, data.getBytes(StandardCharsets.UTF_8));
        if (encrypt == null) {
            return null;
        }
        return new String(Base64.encode(encrypt, Base64.URL_SAFE), StandardCharsets.UTF_8);
        //return Base64.encodeToString(encrypt, Base64.DEFAULT);
    }

    public static String uploadData(String requestBody, String endpointUrl, String businessId) throws UnsupportedEncodingException {
        if (TextUtils.isEmpty(requestBody)) {
            return "";
        }

        String encryptedBody = encrypt(ENCRYPT_KEY, ENCRYPT_IV, requestBody);
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("biz", businessId);

        if (TextUtils.isEmpty(endpointUrl)) {
            endpointUrl = "https://iboot.site/dio/rsd";
        }

        return submitPostData(endpointUrl, requestHeaders, encryptedBody);
    }

    /*
     * Function  :   封装请求体信息
     * Param     :   params请求体内容，encode编码格式
     */
    private static StringBuffer buildQueryString(Map<String, String> queryParameters) {
        StringBuffer queryString = new StringBuffer();
        try {
            for (Map.Entry<String, String> param : queryParameters.entrySet()) {
                queryString.append(param.getKey())
                        .append("=")
                        .append(param.getValue())
                        .append("&");
            }
            queryString.deleteCharAt(queryString.length() - 1);
        } catch (Exception e) {
            Log.e(TAG, "Error building query string", e);
        }
        return queryString;
    }

    public static String submitGetData(String baseUrl, Map<String, String> queryParameters, String requestBody) throws UnsupportedEncodingException {
        if (TextUtils.isEmpty(requestBody)) {
            return BAD_CONNECT;
        }

        String encryptedBody = encrypt(ENCRYPT_KEY, ENCRYPT_IV, requestBody);
        try {
            URL url = new URL(baseUrl + "?" + buildQueryString(queryParameters).toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Charset", "utf-8");
            connection.setRequestProperty("Content-Type", "text/plain");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "HTTP response code:" + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                return processResponse(inputStream);
            }
        } catch (IOException e) {
            Log.e(TAG, "GET request failed", e);
            return BAD_CONNECT;
        }
        return BAD_CONNECT;
    }

    private static String submitPostData(String endpointUrl, Map<String, String> requestHeaders, String requestBody) {
        byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);
        try {
            URL url = new URL(endpointUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            connection.setRequestProperty("Accept-Charset", "utf-8");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setRequestProperty("Content-Length", String.valueOf(postData.length));

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(postData);
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    return processResponse(inputStream);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "POST request failed", e);
            return BAD_CONNECT;
        }
        return BAD_CONNECT;
    }


    private static String processResponse(InputStream inputStream) {
        String responseContent = null;
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputBuffer.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading response", e);
        }
        responseContent = outputBuffer.toString();
        return responseContent;
    }
}
