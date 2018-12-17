package com.neognp.ytms.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.neognp.ytms.R;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.login.LoginActivity;
import com.trevor.library.util.Setting;

import java.text.SimpleDateFormat;

// https://developer.android.com/training/notify-user/build-notification
// https://developer.android.com/guide/topics/ui/notifiers/notifications?hl=ko
public class YTMSNotification {

    static String TAG = YTMSNotification.class.getSimpleName();

    public static final String CHANNEL_ID = "rfcp";
    public static final int notificationId = 1000;

    public static void show(Context context, Bundle args) {
        if (args == null)
            return;

        if (!Setting.getBoolean(Key.allowPush))
            return;

        try {
            Intent intent = new Intent(context, LoginActivity.class);
            // MainActivity위에 쌓인 모든 Activity 삭제 | MainActivity 새로 실행 방지
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            // 앱 새로 실행 | 모든 Activity 삭제
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // CAUTION
            // FLAG_CANCEL_CURRENT를 설정하지 않으면 이전에 최초 생성했던 같은 intent가 계속 전달되서
            // Intent안의 Bundle이 계속 같은 값을 갖게 됨
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            String title = args.getString(Key.title);
            String message = args.getString(Key.message);
            String date = Key.SDF_CAL_FULL.format(Key.SDF_PAYLOAD_FULL.parse(args.getString(Key.date, "")));

            /* custom notification */
            RemoteViews notificationLayout = new RemoteViews(context.getPackageName(), R.layout.notification_small);
            //notificationLayout.setImageViewResource(R.id.pushIconImg, );
            notificationLayout.setTextViewText(R.id.titleTxt, title);
            notificationLayout.setTextViewText(R.id.msgTxt, message);
            notificationLayout.setTextViewText(R.id.dateTxt, date);

            // @formatter:off
            Notification customNotification = new NotificationCompat.Builder(context, CHANNEL_ID).
                setPriority(NotificationCompat.PRIORITY_HIGH).
                setTicker(context.getString(R.string.app_name)). // 자동 슬라이드 다운되도록 설정
                setSmallIcon(R.drawable.notificaion_icon).
                setSubText(title).
                setStyle(new NotificationCompat.DecoratedCustomViewStyle()).
                setCustomContentView(notificationLayout).
                // setCustomBigContentView(notificationLayoutExpanded).
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC). // 잠금화면에 표시 여부 설정(5.0이상에서만 작동)
                setVibrate(new long[]{0, 1000}).
                setLights(Color.CYAN, 0, 2000).
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).
                setAutoCancel(true). // 터치시 알림바에서 삭제
                //setContentIntent(contentIntent).
                build();
            // @formatter:on

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(notificationId, customNotification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
