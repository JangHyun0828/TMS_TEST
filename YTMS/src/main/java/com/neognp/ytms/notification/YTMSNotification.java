package com.neognp.ytms.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.neognp.ytms.R;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.neognp.ytms.carowner.main.CarOwnerMainActivity;
import com.neognp.ytms.delivery.main.DeliveryMainActivity;
import com.neognp.ytms.delivery.pallets.PalletsReceiptHistoryActivity;
import com.neognp.ytms.login.LoginActivity;
import com.neognp.ytms.shipper.main.ShipperMainActivity;
import com.neognp.ytms.thirdparty.main.ThirdPartyMainActivity;
import com.trevor.library.util.Setting;

// https://developer.android.com/training/notify-user/build-notification
// https://developer.android.com/guide/topics/ui/notifiers/notifications?hl=ko
// Build a PendingIntent with a back stack
// https://developer.android.com/training/notify-user/navigation#java

/* CAUTION 앱 상태에 따른 푸시 수신 차이점
   - background 상태: Launcher Activity(IntroActivity)실행 후, 타켓 Activity 가 실행됨 /  Notification 기본 레이아웃으로 표시 및  Firebase Console>알림>알림 제목/알림 텍스트 내용이 표시
   - foreground 상태: 타켓 Activity 가 바로 실행됨  / Notification 커스텀 레이아웃으로 표시 및  Firebase Console>추가옵션>맞춤데이터>date/title/text 키값 내용이 표시
*/
public class YTMSNotification {

    static String TAG = YTMSNotification.class.getSimpleName();

    public static final int notificationId = 1000;

    public static void show(Bundle args) {
        if (args == null)
            return;

        if (!Setting.getBoolean(Key.allowPush))
            return;

        try {
            Notification notification = null;

            Intent intent = null;
            if (Setting.getBoolean(Key.allowAutoLogin) && Key.getUserInfo() != null) {
                String AUTH_CD = Key.getUserInfo().getString("AUTH_CD");
                if (AUTH_CD == null)
                    return;

                // 화주
                if (AUTH_CD.equals("CST"))
                    intent = new Intent(MyApp.get(), ShipperMainActivity.class);
                    // 차주
                else if (AUTH_CD.equals("CAR"))
                    intent = new Intent(MyApp.get(), CarOwnerMainActivity.class);
                    // 배송센터
                else if (AUTH_CD.equals("DC"))
                    intent = new Intent(MyApp.get(), DeliveryMainActivity.class);
                    // 보관센터
                else if (AUTH_CD.equals("TPL"))
                    intent = new Intent(MyApp.get(), ThirdPartyMainActivity.class);
                    // 관리자: 배송센터>팔레트 요청 접수내역 이동
                else if (AUTH_CD.equals("MST"))
                    intent = new Intent(MyApp.get(), PalletsReceiptHistoryActivity.class);
            } else {
                intent = new Intent(MyApp.get(), LoginActivity.class);
            }

            // MainActivity 위에 쌓인 모든 Activity 삭제 | MainActivity 새로 실행 방지
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            // 앱 새로 실행 | 모든 Activity 삭제
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // CAUTION
            // FLAG_CANCEL_CURRENT 를 설정하지 않으면 이전에 최초 생성했던 같은 intent 가 계속 전달되서
            // Intent 안의 Bundle 이 계속 같은 값을 갖게 됨
            PendingIntent contentIntent = PendingIntent.getActivity(MyApp.get(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            String date = Key.SDF_CAL_FULL.format(Key.SDF_PAYLOAD_FULL.parse(args.getString(Key.date, "")));
            String title = args.getString(Key.title);
            String text = args.getString(Key.text);

            /* custom notification */
            RemoteViews notificationLayout = new RemoteViews(MyApp.get().getPackageName(), R.layout.notification_small);
            notificationLayout.setTextViewText(R.id.titleTxt, title);
            notificationLayout.setTextViewText(R.id.textTxt, text);
            notificationLayout.setTextViewText(R.id.dateTxt, date);

            // Oreo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // @formatter:off
                notification = new Notification.Builder(MyApp.get(), Key.CHANNEL_ID).
                setTicker(MyApp.get().getString(R.string.app_name)). // 자동 슬라이드 다운되도록 설정
                setContentTitle(title).
                setContentText(text).
                setSmallIcon(R.drawable.notificaion_icon).
                setStyle(new Notification.DecoratedCustomViewStyle()).
                setCustomContentView(notificationLayout).
                setVisibility(Notification.VISIBILITY_PUBLIC). // 잠금화면에 표시 여부 설정(5.0이상에서만 작동)
                setAutoCancel(true).
                setContentIntent(contentIntent).
                build();
                // @formatter:on

                NotificationManager notificationManager = MyApp.get().getSystemService(NotificationManager.class);
                NotificationChannel channel = new NotificationChannel(Key.CHANNEL_ID, "용마로지스", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
                channel.setSound(null, null);
                channel.enableLights(true);
                channel.setLightColor(MyApp.get().getColor(R.color.main_theme));
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[] {0, 1000});
                notificationManager.notify(notificationId, notification);
            } else {
                // @formatter:off
                notification = new NotificationCompat.Builder(MyApp.get(), Key.CHANNEL_ID).
                setPriority(NotificationCompat.PRIORITY_HIGH).
                setTicker(MyApp.get().getString(R.string.app_name)). // 자동 슬라이드 다운되도록 설정
                setContentTitle(title).
                setContentText(text).
                setSmallIcon(R.drawable.notificaion_icon).
                setStyle(new NotificationCompat.DecoratedCustomViewStyle()).
                setCustomContentView(notificationLayout).
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC). // 잠금화면에 표시 여부 설정(5.0이상에서만 작동)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).
                setVibrate(new long[]{0, 1000}).
                setLights(Color.CYAN, 0, 2000).
                setAutoCancel(true).
                setContentIntent(contentIntent).
                build();
                // @formatter:on

                NotificationManager notificationManager = (NotificationManager) MyApp.get().getSystemService(MyApp.get().NOTIFICATION_SERVICE);
                notificationManager.notify(notificationId, notification);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
