package com.neognp.dtmsapp_v1.app;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.neognp.dtmsapp_v1.R;
import com.trevor.library.util.JsonConverter;
import com.trevor.library.util.Setting;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Locale;

public class Key {

    public static String TAG = Key.class.getSimpleName();

    // 사용자 정보
    private static Bundle userInfo;

    public static synchronized void saveUserInfo(JSONObject userInfoJson) {
        try {
            if (userInfoJson == null)
                return;

            Setting.putString("userInfo", userInfoJson.toString(3));
            userInfo = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void updateUserInfo(JSONObject modUserInfoJson) {
        try {
            if (modUserInfoJson == null)
                return;

            JSONObject userInfoJson = new JSONObject(Setting.getString("userInfo"));

            Iterator<String> keys = modUserInfoJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();

                if (modUserInfoJson.get(key) instanceof Integer) {
                    userInfoJson.put(key, modUserInfoJson.getInt(key));
                } else if (modUserInfoJson.get(key) instanceof String) {
                    userInfoJson.put(key, modUserInfoJson.getString(key));
                }
            }

            saveUserInfo(userInfoJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bundle getUserInfo()
    {
        if (userInfo == null)
            userInfo = (Bundle) JsonConverter.readFromJsonString(Setting.getString("userInfo"));
        return userInfo;
    }

    // <REQUEST>
    // public static final int REQUEST_=;

    // <RESULT>
    public static final int RESULT_CLEAR_HISTORY = 2000;
    public static final int RESULT_AUTHORIZED_PHONE_NUM = 2001;
    public static final int RESULT_REFRESH = 2002;

    // <Action>
    public static final String ACTION_RECEIVED_PUSH = MyApp.get().getPackageName() + ".ACTION_RECEIVED_PUSH";
    public static final String ACTION_START_ACTIVITY = MyApp.get().getPackageName() + ".ACTION_START_ACTIVITY";
    public static final String ACTION_GPS_SERVICE_START = MyApp.get().getPackageName() + ".ACTION_GPS_SERVICE_START";
    public static final String ACTION_GPS_SERVICE_STOP = MyApp.get().getPackageName() + ".ACTION_GPS_SERVICE_STOP";
    public static final String ACTION_GPS_SERVICE_STATE = MyApp.get().getPackageName() + ".ACTION_GPS_SERVICE_STATE";
    public static final String ACTION_GPS_SERVICE_LOCATION_UPDATED = MyApp.get().getPackageName() + ".ACTION_GPS_SERVICE_LOCATION_UPDATED";

    // <Directory>
    // 파일 저장 위치: /storage/emulated/0/Download/com.neognp.dtmsapp/DTMS
    public static File getDebugStorage() {
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + MyApp.get().getString(R.string.app_name));
        if (storageDir != null) {
            if (!storageDir.mkdirs()) {
                if (!storageDir.exists()) {
                    Log.d(TAG, "+ getStorageDir(): failed to create directory");
                    return null;
                }
            }
        }

        return storageDir;
    }

    // <File>
    public static final String FONT_NORMAL = "fonts/noto_sans_kr_regular_hestia.otf";
    public static final String FONT_MEDIUM = "fonts/noto_sans_kr_medium_hestia.otf";
    public static final String FONT_BOLD = "fonts/noto_sans_kr_bold_hestia.otf";

    // <Key>
    public static final String CHANNEL_COMMON = "DTMS";
    public static final String allowPush = "allowPush";
    public static final String allowAutoLogin = "allowAutoLogin";
    public static final String args = "args";
    public static final String title = "title";
    public static final String text = "text";
    public static final String message = "message";
    public static final String date = "date";
    public static final String result_code = "result_code";
    public static final String result_msg = "result_msg";
    public static final String resBody = "resBody";
    public static final String resStr = "resStr";
    public static final String data = "data";
    public static final String className = "className";

    // <Value>
    public static final SimpleDateFormat SDF_PAYLOAD = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    public static final SimpleDateFormat SDF_PAYLOAD_FULL = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
    public static final SimpleDateFormat SDF_CAL_DEFAULT = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
    public static final SimpleDateFormat SDF_CAL_WEEKDAY = new SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.getDefault());
    public static final SimpleDateFormat SDF_CAL_FULL = new SimpleDateFormat("yyyy.MM.dd(E) HH:mm:ss", Locale.getDefault());

    public static final float DEFAULT_ZOOM_LEVEL_SINGLE_PIN = 15.0f;
    public static final double DEFAULT_LAT = 37.318058, DEFAULT_LON = 126.787925; //(주)대양그룹
}