package com.neognp.ytms.http;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.trevor.library.app.LibApp;
import com.trevor.library.util.DeviceUtil;
import com.trevor.library.util.JsonConverter;
import com.trevor.library.util.Setting;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class YTMSRestRequestor {

    private static String TAG = YTMSRestRequestor.class.getSimpleName();

    /**
     * 전송 parameter template 생성
     */
    public static JSONObject buildPayload() {
        JSONObject payloadJson = null;

        try {
            payloadJson = new JSONObject();
            String uuid = DeviceUtil.getUuid();
            // TEST
            if (uuid.endsWith("810d"))
                uuid = "ffffffff-0000-0000-0000-0000aaaaaaaa";
            payloadJson.put("uuid", uuid);
            payloadJson.put("phone_no", DeviceUtil.getPhoneNumber());
            payloadJson.put("manufacturer", Build.MANUFACTURER);
            payloadJson.put("network_operator", DeviceUtil.getOperatorName());
            payloadJson.put("model", Build.MODEL);
            payloadJson.put("os_type", "Android");
            payloadJson.put("os_version", Build.VERSION.RELEASE);
            payloadJson.put("app_version", DeviceUtil.getAppVersionName());
            payloadJson.put("content-Type", "application/json");
            if (Key.getUserInfo() != null) {
                payloadJson.put("userId", Key.getUserInfo().getString("userId"));
            }
        } catch (Exception e) {
        }

        return payloadJson;
    }

    public static Bundle requestPost(@NonNull String urlStr, boolean useSSL, @NonNull JSONObject payloadJson, boolean showDebug, boolean includeResStr) {
        return requestPost(null, urlStr, useSSL, payloadJson, showDebug, includeResStr);
    }

    public static Bundle requestPost(String domain, @NonNull String urlStr, boolean useSSL, @NonNull JSONObject payloadJson, boolean showDebug, boolean includeResStr) {
        Bundle response = new Bundle();

        if (!DeviceUtil.isNetworkAvailable()) {
            Bundle resBody = new Bundle();
            resBody.putString(Key.result_code, "-1");
            resBody.putString(Key.result_msg, MyApp.get().getString(R.string.error_network));
            response.putParcelable(Key.resBody, resBody);
            return response;
        }

        HttpURLConnection conn = null;

        try {
            if (urlStr == null)
                return null;

            if (domain == null)
                urlStr = Setting.getString("ip") + ":" + Setting.getInt("port") + urlStr;
            else
                urlStr = domain + urlStr;

            if (useSSL)
                urlStr = "https://" + urlStr;
            else
                urlStr = "http://" + urlStr;

            String payload = null;
            if (payloadJson != null)
                payload = payloadJson.toString(3);

            if (MyApp.onDebug && showDebug) {
                Log.e(TAG, "+ url:\n" + urlStr);
                Log.e(TAG, "+ payload:\n" + payload);
            }

            URL url = new URL(urlStr);

            if (useSSL) {
                //Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[] {};
                    }

                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                    }
                }};

                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                conn = (HttpsURLConnection) url.openConnection();
                ((HttpsURLConnection) conn).setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            if (payload != null) {
                writer.write(payload);
            }
            writer.flush();
            writer.close();
            os.close();

            conn.connect();

            int resCode = conn.getResponseCode();
            if (MyApp.onDebug && showDebug)
                Log.i(TAG, "+ response code: " + resCode);

            StringBuilder resStrBuilder = new StringBuilder();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            for (; ; ) {
                String line = bufferedReader.readLine();
                if (line == null)
                    break;
                resStrBuilder.append(line + ' ');
            }

            String resStr = resStrBuilder.toString();
            bufferedReader.close();

            if (resCode == HttpURLConnection.HTTP_OK) {
                // response>body
                Bundle resBody = (Bundle) JsonConverter.readFromJsonString(resStr);
                response.putParcelable(Key.resBody, resBody);

                if (MyApp.onDebug && showDebug) {
                    //if (resStr.startsWith("{")) {
                    //    JSONObject jsonObj = new JSONObject(resStr);
                    //    Log.i(TAG, "+ resStr:\n" + jsonObj.toString(3));
                    //} else if (resStr.startsWith("[")) {
                    //    JSONArray jsonAry = new JSONArray(resStr);
                    //    Log.i(TAG, "+ resStr:\n" + jsonAry.toString(3));
                    //}
                    Log.i(TAG, "+ response:\n" + TextUtil.formatIndentedString(response.toString()));
                }
            } else {
                Bundle resBody = new Bundle();
                resBody.putString(Key.result_code, "-1");
                resBody.putString(Key.result_msg, LibApp.get().getString(R.string.error_response) + "(" + resCode + ")"); // "응답 오류"
                response.putParcelable(Key.resBody, resBody);

                if (MyApp.onDebug && showDebug) {
                    //Log.i(TAG, "+ resStr:\n" + resStr);
                    Log.i(TAG, "+ response:\n" + TextUtil.formatIndentedString(response.toString()));
                }
            }

            if (includeResStr)
                response.putString(Key.resStr, resStr);

        } catch (Exception e) {
            e.printStackTrace();
            Bundle resBody = new Bundle();
            resBody.putString(Key.result_code, "-1");
            resBody.putString(Key.result_msg, e.toString());
            response.putParcelable(Key.resBody, resBody);
        } finally {
            try {
                if (conn != null)
                    conn.disconnect();
            } catch (Exception e2) {
            }
        }

        return response;
    }

}