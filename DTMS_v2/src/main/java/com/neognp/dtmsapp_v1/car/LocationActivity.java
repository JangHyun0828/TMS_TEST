package com.neognp.dtmsapp_v1.car;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.app.MyApp;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.neognp.dtmsapp_v1.popup.ConfirmCancelDialog;
import com.skt.Tmap.TMapTapi;
import com.skt.Tmap.TMapView;
import com.trevor.library.app.LibKey;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.DisplayUtil;
import com.trevor.library.util.FontUtil;
import com.trevor.library.util.MapUtil;
import com.trevor.library.util.Setting;

import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LocationActivity extends BasicActivity implements
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

	private SupportMapFragment mapFragment;
	private GoogleMap mMap;
	private com.google.android.gms.maps.model.Marker Marker = null;

	private Float outLat;
	private Float outLon;

	static private int count;

	TimerTask timerTask;
	Timer timer = new Timer();

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.car_location_activity);

		setTitleBar("차량 위치", R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

		mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mainMap);
		mapFragment.getMapAsync(this);

		ConstraintLayout mapLayout = new ConstraintLayout(this);
		TMapView tmapview = new TMapView(this);

		count = 1;

		init();
	}

	private void init()
	{
		try
		{
			args = getIntent().getExtras();

			if (args == null)
				return;

			outLat = Float.valueOf(args.getString("OUT_LAT"));
			outLon = Float.valueOf(args.getString("OUT_LON"));

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	protected void onResume()
	{
		super.onResume();
		startTimerTask();
	}

	protected void onPause()
	{
		super.onPause();
		stopTimerTask();

	}

	protected void onDestroy()
	{
		super.onDestroy();
		stopTimerTask();
	}

	public void onClick(View v)
	{
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

		switch (v.getId())
		{
			case R.id.titleLeftBtn0:
				finish();
				break;

			case R.id.titleRightBtn1:
				break;
		}
	}

	@Override
	public void onMapReady(GoogleMap googleMap)
	{
		mMap = googleMap;

		mMap.setOnCameraIdleListener(this);
		mMap.setOnCameraMoveListener(this);
		mMap.setOnMarkerClickListener(this);
		mMap.setOnInfoWindowClickListener(this);
		mMap.getUiSettings().setMyLocationButtonEnabled(true);
		mMap.getUiSettings().setMapToolbarEnabled(false);
		mMap.getUiSettings().setRotateGesturesEnabled(false);
//		mMap.getUiSettings().setTiltGesturesEnabled(false);
		mMap.getUiSettings().setCompassEnabled(true);

		reDrawMap();
	}

	public void startTimerTask()
	{
		stopTimerTask();

		timerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				reDrawMap();
			}
		};
		timer.schedule(timerTask,(10*6000),(10*6000));
	}

	public void stopTimerTask()
	{
		if(timerTask != null)
		{
			timerTask.cancel();
			timerTask = null;
		}
	}

	private synchronized void reDrawMap()
	{
		if (onReq)
			return;

		try
		{
			if (Key.getUserInfo() == null)
				return;

			new AsyncTask<Void, Void, Bundle>()
			{
				protected void onPreExecute()
				{
					onReq = true;
					showLoadingDialog(null, false);
				}

				protected Bundle doInBackground(Void... arg0)
				{
					JSONObject payloadJson = null;
					try
					{
						payloadJson = RestRequestor.buildPayload();
						payloadJson.put("driverHp", args.getString("DRIVER_HP"));
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					return RestRequestor.requestPost(API.URL_CAR_LOCATION, false, payloadJson, true, false);
				}

				protected void onPostExecute(Bundle response)
				{
					onReq = false;
					dismissLoadingDialog();

					try
					{
						Bundle resBody = response.getBundle(Key.resBody);
						String result_code = resBody.getString(Key.result_code);
						String result_msg = resBody.getString(Key.result_msg);

						if (result_code.equals("200"))
						{
							Bundle data = resBody.getBundle(Key.data);

							String MOBILE_NO = data.getString("MOBILE_NO", "");
							String CAR_NO = data.getString("CAR_NO", "");
							Float CAR_LAT = Float.valueOf(data.getString("CAR_LAT", ""));
							Float CAR_LON = Float.valueOf(data.getString("CAR_LON", ""));

							try
							{
								mMap.clear();
								if(count == 1)
								{
									mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(outLat, outLon), Key.DEFAULT_ZOOM_LEVEL_SINGLE_PIN));
								}
								addMarkerCenter(outLat, outLon);
								addMarkerCar(CAR_NO, CAR_LAT, CAR_LON);
								requestMarkerAddress(outLat, outLon);
								count += 1;
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}

							//Tmap 앱 바로가기
							ImageButton btnTmap = (ImageButton) findViewById(R.id.btnTmap);
							btnTmap.setOnClickListener(new View.OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									ConfirmCancelDialog.show(LocationActivity.this, "[" + args.getString("OUT_NM") + "]" +"로 경로안내 시작하시겠습니까?", "취소", "확인", true, new ConfirmCancelDialog.DialogListener()
									{
										public void onCancel()
										{

										}

										public void onConfirm()
										{
											if(getPackageList())
											{
												//설치가 되어있을 시
												TMapTapi tmaptapi = new TMapTapi(LocationActivity.this);
												tmaptapi.setSKTMapAuthentication("817f49e8-500c-46a1-8c04-7da1fda24d07");
												tmaptapi.invokeNavigate(args.getString("OUT_NM"), outLon, outLat, 0, true);

											}
											else
											{
												//설치가 안되어 있을 시
												String url = "market://details?id=" + "com.skt.tmap.ku";
												Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
												startActivity(i);
											}
										}
									});
								}
							});
						}
						else
						{
							showToast(result_msg + "(result_code:" + result_msg + ")", true);
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						showToast(e.getMessage(), false);
					}
				}
			}.execute();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	//Tmap 앱 설치 유무 확인
	public boolean getPackageList()
	{
		boolean isExist = false;

		PackageManager pkgMgr = getPackageManager();
		List<ResolveInfo> mApps;
		Intent tmapIntent = new Intent(Intent.ACTION_MAIN, null);
		tmapIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		mApps = pkgMgr.queryIntentActivities(tmapIntent, 0);

		try
		{
			for(int i = 0; i <mApps.size(); i++)
			{
				if(mApps.get(i).activityInfo.packageName.startsWith("com.skt.tmap.ku"))
				{
					isExist = true;
					break;
				}
			}
		}
		catch (Exception e)
		{
			isExist = false;
		}

		return isExist;
	};

	@Override
	public void onCameraIdle()
	{
//		requestMapCenterAddress();
	}

	@Override
	public void onCameraMove()
	{
		//Log.i(TAG, "+ onCameraMove(): zoomLevel=" + getZoomLevel());
	}

	@Override
	public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker)
	{
		Log.d(TAG, "+ onMarkerClick(): marker=" + marker);

		return false;
		//return true; // 말풍선 안보이게
	}

	@Override
	public void onInfoWindowClick(com.google.android.gms.maps.model.Marker marker)
	{
		onMarkerClick(marker);
	}

	public float getZoomLevel()
	{
		if (mMap != null)
		{
			return Math.round(mMap.getCameraPosition().zoom);
		}
		return 0;
	}

	public void addMarkerCenter(Float centerLat, Float centerLon)
	{
		try
		{
			if (mMap == null || centerLat == null)
				return;

			final String outNm = args.getString("OUT_NM");
			final LatLng loc = new LatLng(centerLat, centerLon);

			Bitmap bmp = BitmapFactory.decodeResource(MyApp.get().getResources(), R.drawable.img_out).copy(Bitmap.Config.ARGB_8888, true);
			Canvas canvas = new Canvas(bmp);
			Paint paint = new Paint();
			Typeface pinFont = FontUtil.getFont(Setting.getString(LibKey.fontPathMedium));
			paint.setTypeface(pinFont);
			paint.setTextSize(DisplayUtil.SpToPx(17));
			paint.setColor(0xff323332);
			Rect textRect = new Rect();

			if(Marker != null)
			{
				Marker.remove();
			}
			LatLng latlng = new LatLng(centerLat, centerLon);

			MarkerOptions markerOptions = new MarkerOptions();
			markerOptions.position(latlng);
			markerOptions.title(outNm);
			markerOptions.snippet(MapUtil.getAddress(centerLat, centerLon));
			markerOptions.draggable(false);

			com.google.android.gms.maps.model.Marker marker = mMap.addMarker(markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bmp)).anchor(0.5f, 1));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void addMarkerCar(String CAR_NO, Float LAT, Float LON)
	{
		try
		{
			if (mMap == null || LAT == null || LON == null)
				return;

			final String name = CAR_NO;
			final LatLng loc = new LatLng(LAT, LON);

			Bitmap bmp = BitmapFactory.decodeResource(MyApp.get().getResources(), R.drawable.img_car).copy(Bitmap.Config.ARGB_8888, true);
			Canvas canvas = new Canvas(bmp);
			Paint paint = new Paint();
			Typeface pinFont = FontUtil.getFont(Setting.getString(LibKey.fontPathMedium));
			paint.setTypeface(pinFont);
			paint.setTextSize(DisplayUtil.SpToPx(17));
			paint.setColor(0xff323332);
			Rect textRect = new Rect();

			if(Marker != null)
			{
				Marker.remove();
			}
			LatLng latlng = new LatLng(LAT, LON);

			MarkerOptions markerOptions = new MarkerOptions();
			markerOptions.position(latlng);
			markerOptions.title(name);
			markerOptions.snippet(MapUtil.getAddress(LAT, LON));
			markerOptions.draggable(false);

			com.google.android.gms.maps.model.Marker marker = mMap.addMarker(markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bmp)).anchor(0.5f, 1));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@SuppressLint ("StaticFieldLeak")
	private synchronized void requestMarkerAddress(Float centerLat, Float centerLon)
	{
		if (onReq)
			return;

		if (centerLat == null)
			return;

		try
		{
			new AsyncTask<Void, Void, String>()
			{
				protected void onPreExecute()
				{
					onReq = true;
				}

				protected String doInBackground(Void... arg0)
				{
					String reqAddress = "";
					try {
						reqAddress = MapUtil.getAddress(centerLat, centerLon);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return reqAddress;
				}

				protected void onPostExecute(String markerAddress)
				{
					onReq = false;
					dismissLoadingDialog();

					try
					{
						//((TextView) findViewById(R.id.centerAddressTxt)).setText(markerAddress);
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

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == 10)
		{
			if(resultCode == RESULT_OK)
			{

			}
		}
	}
}