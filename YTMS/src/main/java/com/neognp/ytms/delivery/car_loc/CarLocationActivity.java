package com.neognp.ytms.delivery.car_loc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.app.LibKey;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.DisplayUtil;
import com.trevor.library.util.FontUtil;
import com.trevor.library.util.MapUtil;
import com.trevor.library.util.Setting;

import org.json.JSONObject;

public class CarLocationActivity extends BasicActivity implements
        //@formatter:off
        View.OnClickListener,
        OnMapReadyCallback,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener
       //@formatter:on
{

    private boolean onReq;
    private Bundle args;

    private double centerLat = Key.DEFAULT_LAT, centerLon = Key.DEFAULT_LON;

    private SupportMapFragment mapFragment;
    private GoogleMap mMap;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_location_activity);

        setTitleBar("차량 위치", R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mainMap);
        mapFragment.getMapAsync(this);

        init();
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    private void init() {
        try {
            args = getIntent().getExtras();

            // TEST
            //args = new Bundle();
            //args.putDouble("lat", Key.DEFAULT_LAT);
            //args.putDouble("lon", Key.DEFAULT_LON);
            //args.putString("CAR_NO", "경기99바1234");

            if (args == null)
                return;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                finish();
                break;
            case R.id.titleRightBtn1:
                requestCarLocation();
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        //mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);

        try {
            // 기본 위치로 지도 이동
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Key.DEFAULT_LAT, Key.DEFAULT_LON), Key.DEFAULT_ZOOM_LEVEL_SINGLE_PIN));
            requestCarLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraIdle() {
        //requestMapCenterAddress();
    }

    @Override
    public void onCameraMove() {
        //Log.i(TAG, "+ onCameraMove(): zoomLevel=" + getZoomLevel());
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "+ onMarkerClick(): marker=" + marker);

        return false;
        //return true; // 말풍선 안보이게
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        onMarkerClick(marker);
    }

    //private final AtomicInteger addrReqSeq = new AtomicInteger();
    //
    //@SuppressLint ("StaticFieldLeak")
    //private void requestMapCenterAddress() {
    //    final int reqSeqId = addrReqSeq.incrementAndGet();
    //    final double reqCenterLat = mMap.getCameraPosition().target.latitude;
    //    final double reqCenterLon = mMap.getCameraPosition().target.longitude;
    //
    //    Log.e(TAG, "+ requestMapCenterAddress(): reqSeqId=" + reqSeqId);
    //
    //    new AsyncTask<Void, Void, String>() {
    //        protected String doInBackground(Void... arg0) {
    //            String reqAddress = "";
    //            try {
    //                reqAddress = MapUtil.getAddress(reqCenterLat, reqCenterLon);
    //            } catch (Exception e) {
    //                e.printStackTrace();
    //            }
    //            return reqAddress;
    //        }
    //
    //        protected void onPostExecute(String reqAddress) {
    //            try {
    //                updateMapCenterAddress(reqSeqId, reqCenterLat, reqCenterLon, reqAddress);
    //            } catch (Exception e) {
    //                e.printStackTrace();
    //            }
    //        }
    //    }.execute();
    //}
    //
    //private synchronized void updateMapCenterAddress(int reqSeqId, double reqCenterLat, double reqCenterLon, String reqAddress) {
    //    int curSeqId = addrReqSeq.get();
    //    Log.e(TAG, "+ updateMapCenterAddress(): reqSeqId/curSeqId/reqAddress: " + reqSeqId + "/" + curSeqId + "/" + reqAddress);
    //
    //    if (reqSeqId != curSeqId)
    //        return;
    //
    //    centerLat = reqCenterLat;
    //    centerLon = reqCenterLon;
    //    ((TextView) findViewById(R.id.carAddressTxt)).setText(reqAddress);
    //}

    public double[] getMapCenterLocation() {
        return new double[] {centerLat, centerLon};
    }

    public float getZoomLevel() {
        if (mMap != null)
            return Math.round(mMap.getCameraPosition().zoom);

        return 0;
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestCarLocation() {
        if (onReq)
            return;

        try {
            if (args == null)
                return;

            if (Key.getUserInfo() == null)
                return;

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, true);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("carCd", args.getString("CAR_CD"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_LOCATION, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    //dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            Bundle data = resBody.getBundle("data");

                            if (data == null) {
                                dismissLoadingDialog();
                                showToast("차량 위치를 확인할 수 없습니다.", true);
                                return;
                            }

                            addMarker(data);
                            requestMarkerAddress(data);
                        } else {
                            dismissLoadingDialog();
                            showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        dismissLoadingDialog();
                        showToast(e.getMessage(), false);
                    }

                    // TEST
                    //Bundle car = new Bundle();
                    //car.putDouble("LAT", Key.DEFAULT_LAT);
                    //car.putDouble("LON", Key.DEFAULT_LON);
                    //car.putString("CAR_NO", "경기99바1234");
                    //addMarker(car);
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addMarker(final Bundle car) {
        try {
            if (mMap == null || car == null)
                return;

            double loc_latitude = Double.parseDouble(car.getString("LAT"));
            double loc_longitude = Double.parseDouble(car.getString("LON"));
            final LatLng loc = new LatLng(loc_latitude, loc_longitude);

            final String name = car.getString("CAR_NO", "");

            Bitmap bmp = BitmapFactory.decodeResource(MyApp.get().getResources(), R.drawable.pin_icon_side).copy(Bitmap.Config.ARGB_8888, true);
            //bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);

            Canvas canvas = new Canvas(bmp);
            Paint paint = new Paint();
            Typeface pinFont = FontUtil.getFont(Setting.getString(LibKey.fontPathMedium));
            paint.setTypeface(pinFont);
            paint.setTextSize(DisplayUtil.SpToPx(17));
            paint.setColor(0xff323332);
            Rect textRect = new Rect();
            paint.getTextBounds(name, 0, name.length(), textRect);

            int offsetY = DisplayUtil.DpToPx(73f); // 핀 이미지 안에서 상단 핀 영역의 높이 + 하단 여백
            float charY = offsetY - (paint.getFontMetrics().ascent + paint.getFontMetrics().descent);

            // 1줄
            if (textRect.width() <= canvas.getWidth()) {
                canvas.drawText(name, (canvas.getWidth() - textRect.width()) / 2, charY, paint);
            }
            // 2줄 이상
            else {
                float charX = 0;
                for (int i = 0; i < name.length(); i++) {
                    String ch = String.valueOf(name.charAt(i));
                    int chW = (int) paint.measureText(ch);
                    if (charX + chW > canvas.getWidth()) {
                        charX = 0;
                        charY += textRect.height() + DisplayUtil.DpToPx(0.5f);
                    }
                    canvas.drawText(ch, charX, charY, paint);
                    charX += chW;
                }
            }

            Marker marker = mMap.addMarker(new MarkerOptions().
                    position(loc).
                    icon(BitmapDescriptorFactory.fromBitmap(bmp)).
                    anchor(0.5f, 1));

            //Log.d(TAG, "+ addMarker(): marker=" + marker);

            // 차량 위치로 지도 중심 이동
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc_latitude, loc_longitude), Key.DEFAULT_ZOOM_LEVEL_SINGLE_PIN));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestMarkerAddress(Bundle car) {
        if (onReq)
            return;

        if (car == null)
            return;

        try {
            double lat = Double.parseDouble(car.getString("LAT"));
            double lon = Double.parseDouble(car.getString("LON"));

            new AsyncTask<Void, Void, String>() {
                protected void onPreExecute() {
                    onReq = true;
                }

                protected String doInBackground(Void... arg0) {
                    String reqAddress = "";
                    try {
                        reqAddress = MapUtil.getAddress(lat, lon);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return reqAddress;
                }

                protected void onPostExecute(String markerAddress) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        ((TextView) findViewById(R.id.carAddressTxt)).setText(markerAddress);
                    } catch (Exception e) {
                    }
                }
            }.execute();
        } catch (Exception e) {
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}