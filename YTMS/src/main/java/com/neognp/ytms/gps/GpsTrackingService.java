package com.neognp.ytms.gps;

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
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
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
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.util.DeviceUtil;
import com.trevor.library.util.MapUtil;
import com.trevor.library.util.TextUtil;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class GpsTrackingService extends Service {

    static String TAG = GpsTrackingService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public GpsTrackingService getService() {
            return GpsTrackingService.this;
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
    private Location location;

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
            locationRequest.setSmallestDisplacement(1); // 1미터 이동시 마다 업데이트
            locationRequest.setFastestInterval(1000);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        Log.i(TAG, "+ onLocationResult(): location=" + location);
                        //Toast.makeText(getApplicationContext(), "onLocationResult(): " + location, Toast.LENGTH_SHORT).show();
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
        if (DeviceUtil.getUuid().endsWith("810d"))
            msgHandler.sendEmptyMessageDelayed(0, 5000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                    return;
            }
        }

        locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public void stopTracking() {
        msgHandler.removeCallbacksAndMessages(null);

        locationClient.removeLocationUpdates(locationCallback);
    }

    private SimpleDateFormat timeStampSdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault());

    @SuppressLint ("HandlerLeak")
    public Handler msgHandler = new Handler() {
        public void handleMessage(Message msg) {
            String curDate = timeStampSdf.format(new Date());
            Log.i(TAG, "+ run(): " + curDate);
            Toast.makeText(getApplicationContext(), "YTMS Service: " + curDate, Toast.LENGTH_SHORT).show();
            msgHandler.sendEmptyMessageDelayed(0, 5000);
        }
    };

    private boolean onReq;
    private final AtomicInteger addrReqSeq = new AtomicInteger();

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestGpsSend(Location userLocation) {
        //if (onReq)
        //    return;

        final int reqSeqId = addrReqSeq.incrementAndGet();
        Log.e(TAG, "+ requestGpsSend(): reqSeqId=" + reqSeqId);

        try {
            if (Key.getUserInfo() == null)
                return;

            if (userLocation == null)
                return;

            double userLat = userLocation.getLatitude();
            double userLon = userLocation.getLongitude();

            new AsyncTask<Void, Void, Boolean>() {
                Bundle targetAddress = null;
                Bundle userAddress = null;

                protected void onPreExecute() {
                    //onReq = true;
                }

                protected Boolean doInBackground(Void... arg0) {
                    try {
                        String checkYn = "N";
                        float targetDistance = 0;
                        // 목적지와의 거리
                        if (targetAddress != null) {
                            Double targetLat = Double.parseDouble(targetAddress.getString("LAT"));
                            Double targetLon = Double.parseDouble(targetAddress.getString("LON"));
                            float[] distance = new float[3];
                            Location.distanceBetween(userLat, userLon, targetLat, targetLon, distance);

                            // 목적지의 targetRadius 반경안에  있는지 여부 체크
                            targetDistance = distance[0];
                            Float targetRadius = Float.parseFloat(targetAddress.getString("RAD_METER"));
                            if (targetDistance <= targetRadius)
                                checkYn = "Y";
                        }

                        JSONObject payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("lat", userLat);
                        payloadJson.put("lon", userLon);
                        payloadJson.put("checkYn", checkYn);
                        Bundle response = YTMSRestRequestor.requestPost(API.URL_GPS_SEND, false, payloadJson, true, false);
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);
                        if (result_code.equals("200")) {
                            targetAddress = resBody.getBundle("data");
                            targetAddress.putFloat("targetDistance", targetDistance);
                        } else {
                        }

                        userAddress = new Bundle();
                        userAddress.putDouble("userLat", userLat);
                        userAddress.putDouble("userLon", userLon);
                        userAddress.putString("userAddress", MapUtil.getAddress(userLat, userLon));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }

                protected void onPostExecute(Boolean result) {
                    //onReq = false;

                    try {
                        broadcastLocationUpdate(reqSeqId, targetAddress, userAddress);
                    } catch (Exception e) {
                    }
                }
            }.execute();
        } catch (Exception e) {
        }
    }

    private Bundle targetAddress;

    private synchronized void broadcastLocationUpdate(int reqSeqId, Bundle targetAddress, Bundle userAddress) {
        int curSeqId = addrReqSeq.get();
        Log.e(TAG, "+ broadcastLocationUpdate(): reqSeqId/curSeqId/reqAddress: " + reqSeqId + "/" + curSeqId);

        if (reqSeqId != curSeqId)
            return;

        if (targetAddress == null || userAddress == null)
            return;

        this.targetAddress = targetAddress;

        try {
            Bundle args = new Bundle();

            // 목적지 임계 거리
            args.putFloat("targetRadius", Float.parseFloat(targetAddress.getString("RAD_METER")));

            // 목적지 위도, 경도
            args.putDouble("targetLat", Double.parseDouble(targetAddress.getString("LAT")));
            args.putDouble("targetLon", Double.parseDouble(targetAddress.getString("LON")));

            // 목적지 주소
            args.putString("targetAddress", targetAddress.getString("ADDR"));

            // 사용자 위도, 경도
            args.putDouble("userLat", userAddress.getDouble("userLat"));
            args.putDouble("userLon", userAddress.getDouble("userLon"));

            // 사용자 현재 위치의 주소
            String[] addrAry = userAddress.getString("userAddress").split(" ");
            String userAddress0 = "", userAddress1 = "";
            if (addrAry.length >= 2) {
                userAddress0 = addrAry[0] + " " + addrAry[1];
                for (int i = 2; i < addrAry.length; i++)
                    userAddress1 += " " + addrAry[i];
                userAddress1 = userAddress1.trim();
            } else {
                userAddress0 = userAddress.getString("userAddress");
            }
            args.putString("userAddress0", userAddress0);
            args.putString("userAddress1", userAddress1);

            Log.e(TAG, "+ broadcastLocationUpdate(): arg=\n" + TextUtil.formatBundleToString(args));

            Intent intent = new Intent(Key.ACTION_LOCATION_UPDATED);
            intent.putExtra("args", args);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
