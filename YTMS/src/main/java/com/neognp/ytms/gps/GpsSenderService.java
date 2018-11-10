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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.neognp.ytms.R;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;

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

        //initLocationService();

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

    // https://stackoverflow.com/questions/46481789/android-locationservices-fusedlocationapi-deprecated

    private FusedLocationProviderClient locationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location location;

    private void initLocationService() {
        try {
            locationClient = LocationServices.getFusedLocationProviderClient(MyApp.get());
            locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Log.i(TAG, "+ onSuccess(): location=" + location);
                    Toast.makeText(getApplicationContext(), "onSuccess(): " + location, Toast.LENGTH_SHORT).show();
                }
            });

            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            locationRequest.setSmallestDisplacement(1); // 1미터 이동시 마다 업데이트
            locationRequest.setFastestInterval(1000);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        Log.i(TAG, "+ onLocationResult(): location=" + location);
                        Toast.makeText(getApplicationContext(), "onLocationResult(): " + location, Toast.LENGTH_SHORT).show();
                    }
                }
            };
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startTracking() {
        msgHandler.sendEmptyMessageDelayed(0, 5000);

        //if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        //    // TODO: Consider calling
        //    //    ActivityCompat#requestPermissions
        //    // here to request the missing permissions, and then overriding
        //    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //    //                                          int[] grantResults)
        //    // to handle the case where the user grants the permission. See the documentation
        //    // for ActivityCompat#requestPermissions for more details.
        //    return;
        //}
        //locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
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

}
