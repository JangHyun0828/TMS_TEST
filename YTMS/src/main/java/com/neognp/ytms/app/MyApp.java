package com.neognp.ytms.app;

import android.util.Log;

import com.trevor.library.app.LibApp;
import com.trevor.library.app.LibKey;
import com.trevor.library.util.DeviceUtil;
import com.trevor.library.util.Setting;

public class MyApp extends LibApp {

    public void initApp() {
        Log.i(TAG, "+ initApp(): " + DeviceUtil.getAppName());

        onTest = true;
        onDebug = true;

        // TEST
        //Setting.clearAll();

        /* 앱 최초 실행 시, 앱 설정 기본값 세팅 */
        try {
            Setting.putString(LibKey.fontPathNormal, Key.FONT_NORMAL);
            Setting.putString(LibKey.fontPathBold, Key.FONT_BOLD);

            if (!Setting.contains("ip"))
                Setting.putString("ip", API.DEFAULT_IP);
            if (!Setting.contains("port"))
                Setting.putInt("port", API.DEFAULT_PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}