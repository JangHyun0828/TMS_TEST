package com.neognp.ytms.fcm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.TokenWatcher;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.RemoteMessage;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.neognp.ytms.notification.YTMSNotification;
import com.trevor.library.util.Setting;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

import java.util.Map;

// https://firebase.google.com/docs/cloud-messaging/android/receive?hl=ko
public class MyFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    static String TAG = MyFirebaseMessagingService.class.getSimpleName();

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e(TAG, "+ onNewToken(): " + s);
    }

    @SuppressLint ("StaticFieldLeak")
    public synchronized static void sendRegistrationToServer(Activity activity) {
        if (activity == null)
            return;

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(activity, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                try {
                    String token = instanceIdResult.getToken();
                    Log.e(TAG, "+ sendRegistrationToServer(): token=\n" + token);
                    if (token == null)
                        return;

                    if (Key.getUserInfo() == null)
                        return;

                    new AsyncTask<Void, Void, Bundle>() {
                        protected void onPreExecute() {
                        }

                        protected Bundle doInBackground(Void... arg0) {
                            JSONObject payloadJson = null;
                            try {
                                payloadJson = YTMSRestRequestor.buildPayload();
                                payloadJson.put("userId", Key.getUserInfo().getString("USER_ID"));
                                payloadJson.put("fcmToken", token);
                                if (Setting.getBoolean(Key.allowPush))
                                    payloadJson.put(Key.allowPush, "Y");
                                else
                                    payloadJson.put(Key.allowPush, "N");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return YTMSRestRequestor.requestPost(API.URL_PUSH_REG, false, payloadJson, true, true);
                        }

                        protected void onPostExecute(Bundle response) {
                        }
                    }.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        try {
            // TODO(developer): Handle FCM messages here.
            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            Log.e(TAG, "+ onMessageReceived(): from=" + remoteMessage.getFrom());

            // Check if message contains a data payload.
            Map<String, String> data = remoteMessage.getData();
            if (data != null && data.size() > 0) {
                Log.e(TAG, "+ onMessageReceived(): data=\n" + data);

                if (/* Check if data needs to be processed by long running job */ true) {
                    // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                    //scheduleJob();
                } else {
                    // Handle message within 10 seconds
                    //handleNow();
                }
            }

            // Check if message contains a notification payload.
            if (remoteMessage.getNotification() != null) {
                //Log.e(TAG, "+ onMessageReceived(): Title=" + remoteMessage.getNotification().getTitle());
                //Log.e(TAG, "+ onMessageReceived(): Body=" + remoteMessage.getNotification().getBody());

                // Firebase Console>Clould Messaging>추가옵션>맞춤 데이타: 키/값 입력란에 입력된 값
                Bundle args = new Bundle();
                args.putString(Key.title, data.get(Key.title));
                args.putString(Key.text, data.get(Key.text));
                args.putString(Key.date, data.get(Key.date));
                Log.e(TAG, "+ onMessageReceived(): args=\n" + TextUtil.formatBundleToString(args));
                YTMSNotification.show(args);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}