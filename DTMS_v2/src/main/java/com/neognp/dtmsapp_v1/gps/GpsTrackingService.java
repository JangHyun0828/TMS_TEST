package com.neognp.dtmsapp_v1.gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.app.MyApp;
import com.neognp.dtmsapp_v1.http.RestRequestor;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class GpsTrackingService extends Service
{

    static String TAG = GpsTrackingService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder
    {
        public GpsTrackingService getService()
        {
            return GpsTrackingService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        // TODO release 코드 추가
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "+ onStartCommand(): intent= " + intent + " / flags= " + flags + " / startId=" + startId);
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.e(TAG, "+ onCreate(): ");

        Notification notification = null;

        // Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(Key.CHANNEL_COMMON, "DTMS", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);

            // @formatter:off
            notification = new Notification.Builder(getApplicationContext(), Key.CHANNEL_COMMON).
                    //setTicker(getString(R.string.app_name)). // 자동 슬라이드 다운되도록 설정
                            setSmallIcon(R.drawable.img_logo).
                            setContentTitle("DTMS").
                            setContentText("GPS 전송 중").
                    //setStyle(new Notification.DecoratedCustomViewStyle()).
                    //setCustomContentView(notificationLayout).
                            setVisibility(Notification.VISIBILITY_PUBLIC). // 잠금화면에 표시 여부 설정(5.0이상에서만 작동)
                    setAutoCancel(false).
                            build();
            // @formatter:on
        }
        else
        {
            // @formatter:off
            notification = new NotificationCompat.Builder(this, Key.CHANNEL_COMMON).
                    setPriority(NotificationCompat.PRIORITY_DEFAULT).
                    //setTicker(getString(R.string.app_name)). // 자동 슬라이드 다운되도록 설정
                            setSmallIcon(R.drawable.img_logo).
                            setContentTitle("DTMS").
                            setContentText("GPS 전송 중").
                    //setStyle(new NotificationCompat.DecoratedCustomViewStyle()).
                    //setCustomContentView(notificationLayout).
                            setVisibility(NotificationCompat.VISIBILITY_PUBLIC). // 잠금화면에 표시 여부 설정(5.0이상에서만 작동)
                    setAutoCancel(false). // 터치시 알림바에서 삭제 불가
                    build();
            // @formatter:on
        }

        startForeground(1, notification);

        initialize();

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

    // https://stackoverflow.com/questions/46481789/android-locationservices-fusedlocationapi-deprecated
    private FusedLocationProviderClient locationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    public boolean initialize() {
        try {
            locationClient = LocationServices.getFusedLocationProviderClient(MyApp.get());
            locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Log.i(TAG, "+ onSuccess(): location=" + location);
                    //Toast.makeText(getApplicationContext(), "onSuccess(): " + location, Toast.LENGTH_SHORT).show();
                }
            });

            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        requestGpsSend(location);
                    }
                }
            };
        } catch (SecurityException ex) {
            ex.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //@formatter:off
    private String[] permissions = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    //@formatter:on

    public void startTracking() {
        // TEST
//        if (DeviceUtil.getUuid().endsWith("810d"))
//            msgHandler.sendEmptyMessageDelayed(0, 5000);
//        Toast.makeText(getApplicationContext(), UUID.randomUUID().toString(), Toast.LENGTH_SHORT).show();

        if (locationClient == null || locationRequest == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            for (String permission : permissions)
            {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                    return;
            }
        }

        locationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        broadcastStatusUpdate(true);
    }

    public void stopTracking()
    {
        msgHandler.removeCallbacksAndMessages(null);

        if (locationClient != null)
            locationClient.removeLocationUpdates(locationCallback);

        broadcastStatusUpdate(false);
    }

    private SimpleDateFormat timeStampSdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault());

    @SuppressLint ("HandlerLeak")
    public Handler msgHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            String curDate = timeStampSdf.format(new Date());
            Log.i(TAG, "+ run(): " + curDate);
            Toast.makeText(getApplicationContext(), "DTMS Service: " + curDate, Toast.LENGTH_SHORT).show();
            msgHandler.sendEmptyMessageDelayed(0, 5000);
        }
    };

    private boolean onReq;
    private final AtomicInteger addrReqSeq = new AtomicInteger();
    private long prevSendTime;
    private Bundle targetAddress = null;

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestGpsSend(Location userLocation)
    {
        //if (onReq)
        //    return;

        try {
            if (Key.getUserInfo() == null)
                return;

            if (userLocation == null)
                return;

            final int reqSeqId = addrReqSeq.incrementAndGet();
            Log.e(TAG, "+ requestGpsSend(): reqSeqId=" + reqSeqId);

            new AsyncTask<Void, Void, Boolean>()
            {
                protected void onPreExecute()
                {
                    //onReq = true;
                }

                protected Boolean doInBackground(Void... arg0)
                {
                    try
                    {
                        double userLat = userLocation.getLatitude();
                        double userLon = userLocation.getLongitude();

                        long sendIntervalTime = System.currentTimeMillis() - prevSendTime;
                        // 최초 전송 여부 && 이전 전송시간과의 간격이 1분 미만인 경우
                        if (prevSendTime != 0 && sendIntervalTime < 1 * 60 * 1000)
                            return false;

                        JSONObject payloadJson = RestRequestor.buildPayload();
                        payloadJson.put("userId", Key.getUserInfo().getString("USER_ID"));
                        payloadJson.put("lat", userLat);
                        payloadJson.put("lon", userLon);
                        prevSendTime = System.currentTimeMillis();
                        Bundle response = RestRequestor.requestPost(API.URL_GPS_SEND, false, payloadJson, true, false);
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return true;
                }

                protected void onPostExecute(Boolean result)
                {
                    //onReq = false;

                    try
                    {

                    }

                    catch (Exception e)
                    {

                    }
                }
            }.execute();

        }
        catch (Exception e)
        {

        }
    }

    private synchronized void broadcastStatusUpdate(boolean isRunning)
    {
        Intent intent = new Intent(Key.ACTION_GPS_SERVICE_STATE);
        intent.putExtra("isRunning", isRunning);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private synchronized void broadcastLocationUpdate(int reqSeqId, Bundle userAddress)
    {
        int curSeqId = addrReqSeq.get();
        Log.e(TAG, "+ broadcastLocationUpdate(): reqSeqId/curSeqId: " + reqSeqId + "/" + curSeqId);

        if (reqSeqId != curSeqId)
            return;

        if (userAddress == null)
            return;

        try
        {
            Bundle args = new Bundle();
            args.putAll(userAddress);
            Intent intent = new Intent(Key.ACTION_GPS_SERVICE_LOCATION_UPDATED);
            intent.putExtra("args", args);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}