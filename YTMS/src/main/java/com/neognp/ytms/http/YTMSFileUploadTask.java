package com.neognp.ytms.http;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.neognp.ytms.R;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.trevor.library.app.LibApp;
import com.trevor.library.util.DeviceUtil;
import com.trevor.library.util.JsonConverter;
import com.trevor.library.util.Setting;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class YTMSFileUploadTask extends AsyncTask<Void, Void, Bundle> {

    String TAG = YTMSFileUploadTask.class.getSimpleName();

    private String domain;
    private String urlStr;
    private boolean useSSL;
    private JSONObject payloadJson;
    private String fileParamName;
    private String filePath;
    private HashMap<String, String> fileParams;
    private FileUploadTaskListener listener;
    private boolean showDebug;

    private HttpURLConnection conn;
    private OutputStream os;
    private Bundle response = new Bundle();

    private String delimiter = "--";
    private String boundary = "##" + MyApp.get().getPackageName() + "##";

    public YTMSFileUploadTask(String domain, String urlStr, boolean useSSL, JSONObject payloadJson, String fileParamName, String filePath, FileUploadTaskListener listener, boolean showDebug) {
        this.domain = domain;
        this.urlStr = urlStr;
        this.useSSL = useSSL;
        this.payloadJson = payloadJson;
        this.fileParamName = fileParamName;
        this.filePath = filePath;
        this.listener = listener;
        this.showDebug = showDebug;
    }

    public YTMSFileUploadTask(String domain, String urlStr, boolean useSSL, JSONObject payloadJson, HashMap<String, String> fileParams, FileUploadTaskListener listener, boolean showDebug) {
        this.domain = domain;
        this.urlStr = urlStr;
        this.useSSL = useSSL;
        this.payloadJson = payloadJson;
        this.fileParams = fileParams;
        this.listener = listener;
        this.showDebug = showDebug;
    }

    protected void onPreExecute() {
        if (listener != null)
            listener.onStartUpload();
    }

    protected Bundle doInBackground(Void... params) {
        // 공통 파라미터
        if (payloadJson == null)
            return null;

        if (!DeviceUtil.isNetworkAvailable()) {
            Bundle resBody = new Bundle();
            resBody.putString(Key.result_code, "-1");
            resBody.putString(Key.result_msg, MyApp.get().getString(R.string.error_network));
            response.putParcelable(Key.resBody, resBody);
            return response;
        }

        URL url = null;

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

            if (MyApp.onDebug && showDebug) {
                Log.i(TAG, "+ url:\n" + urlStr);
                Log.i(TAG, "+ payload:\n" + payloadJson);
            }

            url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();
            os = conn.getOutputStream();

            // 일반 파라미터 전송
            if (payloadJson != null) {
                sendParameters(payloadJson);
            }

            // 1개 파일 전송
            if (filePath != null) {
                sendFile(fileParamName, filePath);
            }
            // 1개 이상 파일 전송
            else if (fileParams != null) {
                Set<String> keys = fileParams.keySet();
                for (String fileParamName : keys) {
                    String filePath = fileParams.get(fileParamName);
                    if (filePath != null)
                        sendFile(fileParamName, filePath);
                }
            }

            //  body 종료 전송
            os.write((delimiter + boundary + delimiter + "\r\n").getBytes());
            os.flush();

            os.close();

            int resCode = conn.getResponseCode();
            if (LibApp.onDebug && showDebug)
                Log.i(TAG, "+ response code: " + resCode);

            StringBuilder responseStringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            for (; ; ) {
                String stringLine = bufferedReader.readLine();
                if (stringLine == null)
                    break;
                responseStringBuilder.append(stringLine + ' ');
            }

            String resStr = responseStringBuilder.toString();
            bufferedReader.close();
            //if (LibApp.onDebug && showDebug)
            //    Log.i(TAG, "+ resStr:\n" + resStr);

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

    protected void onPostExecute(Bundle response) {
        if (listener != null)
            listener.onFinishUpload(response);
    }

    // CAUTION doInBackground() 종료 후 호출됨
    @Override
    protected void onCancelled() {
        Log.i(TAG, "+ onCancelled():");

        if (listener != null)
            listener.onCancelUpload();
    }

    public void abortUpload() {
        try {
            cancel(true);

            if (conn != null)
                conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendParameters(String name, String value) throws Exception {
        os.write((delimiter + boundary + "\r\n").getBytes());
        os.write("Content-Type: text/plain\r\n".getBytes());
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes());
        os.write(("\r\n" + value + "\r\n").getBytes());
        os.flush();
    }

    private void sendParameters(JSONObject params) throws Exception {
        if (params == null)
            return;

        Iterator<String> keys = params.keys();
        while (keys.hasNext()) {
            String name = keys.next();
            String value = params.getString(name);

            os.write((delimiter + boundary + "\r\n").getBytes());
            os.write("Content-Type: application/json\r\n".getBytes());
            os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes());
            os.write(("\r\n" + value + "\r\n").getBytes());
            os.flush();
        }
    }

    public void sendFile(String fileParamName, String filePath) {
        try {
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);

            // 파일 파라미터 전송
            os.write((delimiter + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"" + fileParamName + "\"; fileName=\"" + fileName + "\"\r\n").getBytes());
            os.write(("Content-Type: application/octet-stream\r\n").getBytes());
            os.write(("Content-Transfer-Encoding: binary\r\n").getBytes());
            os.write("\r\n".getBytes());

            // 파일 로딩
            FileInputStream fin = new FileInputStream(filePath);
            int uploadFileSize = fin.available();
            Log.e(TAG, "+ uploading file: " + fileName + " / " + uploadFileSize + "(" + TextUtil.formatFileSize(uploadFileSize) + ")");

            int maxBufferSize = 1024;
            int bufferSize = Math.min(uploadFileSize, maxBufferSize);
            byte[] buffer = new byte[bufferSize];
            int byteRead = fin.read(buffer, 0, bufferSize);
            int totalSend = 0;

            // 파일 전송
            while (byteRead > 0) {
                os.write(buffer);
                totalSend += byteRead;

                if (listener != null) {
                    int progress = (int) ((double) totalSend / uploadFileSize * 100);
                    //Log.e(TAG, "+ totalSend/fileSize: " + totalSend + "/" + fileSize);
                    //Log.e(TAG, "+ progress: " + progress);
                    listener.onProgressUpload(progress);
                }

                bufferSize = Math.min(fin.available(), maxBufferSize);
                byteRead = fin.read(buffer, 0, bufferSize);
            }

            os.write("\r\n".getBytes());
            os.flush();
            fin.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface FileUploadTaskListener {

        void onStartUpload();

        void onProgressUpload(float progress);

        void onFinishUpload(Bundle resBundle);

        void onCancelUpload();

    }

}