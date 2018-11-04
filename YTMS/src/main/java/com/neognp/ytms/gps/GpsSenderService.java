package com.neognp.ytms.gps;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.neognp.ytms.R;
import com.neognp.ytms.app.Key;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GpsSenderService extends Service {

    static String TAG = GpsSenderService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public GpsSenderService getService() {
            return GpsSenderService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO release 코드 추가
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "+ onStartCommand(): intent= " + intent + " / flags= " + flags + " / startId=" + startId);
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "+ onCreate(): ");

        Notification notification = null;
        String channelId = "YTMSCarOwner";

        /* custom notification */
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_small);
        //notificationLayout.setImageViewResource(R.id.pushIconImg, );
        notificationLayout.setTextViewText(R.id.titleTxt, "운송화주");
        notificationLayout.setTextViewText(R.id.msgTxt, "GPS 전송 중");

        // Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(channelId, "운송화주", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);

            // @formatter:off
            notification = new Notification.Builder(getApplicationContext(), channelId).
            //setTicker(getString(R.string.app_name)). // 자동 슬라이드 다운되도록 설정
            setSmallIcon(R.drawable.notificaion_icon).
            setContentTitle("운송화주").
            setContentText("GPS 전송 중").
            //setStyle(new Notification.DecoratedCustomViewStyle()).
            //setCustomContentView(notificationLayout).
            setVisibility(Notification.VISIBILITY_PUBLIC). // 잠금화면에 표시 여부 설정(5.0이상에서만 작동)
            setAutoCancel(false).
            build();
            // @formatter:on
        } else {
            // @formatter:off
            notification = new NotificationCompat.Builder(this, "CarOwner").
            setPriority(NotificationCompat.PRIORITY_DEFAULT).
            //setTicker(getString(R.string.app_name)). // 자동 슬라이드 다운되도록 설정
            setSmallIcon(R.drawable.notificaion_icon).
            setContentTitle("운송화주").
            setContentText("GPS 전송 중").
            //setStyle(new NotificationCompat.DecoratedCustomViewStyle()).
            //setCustomContentView(notificationLayout).
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC). // 잠금화면에 표시 여부 설정(5.0이상에서만 작동)
            setAutoCancel(false). // 터치시 알림바에서 삭제 불가
            build();
            // @formatter:on
        }

        startForeground(1, notification);

        startTracking();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "+ onDestroy(): ");

        stopTracking();

        // Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); // remove notification
        }
    }

    public boolean initialize() {
        return true;
    }

    private SimpleDateFormat payloadSdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault());

    @SuppressLint ("HandlerLeak")
    public Handler msgHandler = new Handler() {
        public void handleMessage(Message msg) {
            String curDate = payloadSdf.format(new Date());
            Log.i(TAG, "+ run(): " + curDate);
            Toast.makeText(getApplicationContext(), "YTMS Service: " + curDate, Toast.LENGTH_SHORT).show();
            msgHandler.sendEmptyMessageDelayed(0, 5000);
        }
    };

    public void startTracking() {
        msgHandler.sendEmptyMessageDelayed(0, 5000);
    }

    public void stopTracking() {
        msgHandler.removeCallbacksAndMessages(null);
    }

    private synchronized void broadcastLocationUpdate(Location location) {
        Log.i(TAG, "+ broadcastLocationUpdate(): location=" + location);
        Intent intent = new Intent(Key.ACTION_LOCATION_UPDATED);
        intent.putExtra("location", location);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
