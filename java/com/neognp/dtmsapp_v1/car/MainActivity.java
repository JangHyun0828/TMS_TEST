package com.neognp.dtmsapp_v1.car;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.fcm.MyFirebaseMessagingService;
import com.neognp.dtmsapp_v1.gps.GpsTrackingService;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.neognp.dtmsapp_v1.login.LoginActivity;
import com.neognp.dtmsapp_v1.menu.PwdChangeDialog;
import com.neognp.dtmsapp_v1.popup.ConfirmCancelDialog;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;

import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends BasicActivity
{
	private boolean onReq;
	private DrawerLayout dLayout;
	private GpsTrackingService mGpsTrackingService;
	private boolean onReqGpsTransmitCheck;
	private ArrayList<Bundle> listItems = new ArrayList<Bundle>();

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.car_main_activity);

		TextView txtProfile = (TextView) findViewById(R.id.txt_car_no);
		txtProfile.setText(Key.getUserInfo().getString("PRF_NM", ""));

		TextView txtUserNm = (TextView) findViewById(R.id.txt_user_hello);

		String prf_user_nm = Key.getUserInfo().getString("USER_NM", "");
		SpannableString user_name = new SpannableString(prf_user_nm + "님, 안녕하세요");
		user_name.setSpan(new ForegroundColorSpan(Color.parseColor("#0097D8")), 0, prf_user_nm.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		txtUserNm.setText(user_name);

		//GPS
		IntentFilter filter = new IntentFilter();
		filter.addAction(Key.ACTION_GPS_SERVICE_START);
		filter.addAction(Key.ACTION_GPS_SERVICE_STOP);
		filter.addAction(Key.ACTION_GPS_SERVICE_STATE);
		filter.addAction(Key.ACTION_GPS_SERVICE_LOCATION_UPDATED);
		LocalBroadcastManager.getInstance(this).registerReceiver(gpsServiceReceiver, filter);

		dLayout = (DrawerLayout) findViewById(R.id.dLayout);
		((TextView) findViewById(R.id.menu_userNm)).setText(prf_user_nm);

		//비밀번호 변경
		dLayout.findViewById(R.id.menu_changePwd).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PwdChangeDialog.show(MainActivity.this, false, new PwdChangeDialog.DialogListener() {
					@Override
					public void onCancel() {
						return;
					}

					@Override
					public void onConfirm(String userPwd, String flag) {
						if (flag.equalsIgnoreCase("A")) {
							showToast("비밀번호를 입력 후 다시 시도해주십시오.", false);
						} else if (flag.equalsIgnoreCase("B")) {
							showToast("입력하신 비밀번호가 일치하지 않습니다.", false);
						} else {
							editPassword(userPwd);
						}
					}
				});
			}
		});

		//로그아웃
		dLayout.findViewById(R.id.menu_auto).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				logout();
				dLayout.closeDrawer(Gravity.RIGHT);
			}
		});

		init();
	}

	private void init()
	{
		try
		{
			//GoogleHandler Play 서비스 호환 여부 체크
			GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);

			// 서버 콘솔에 DTMS 토픽이 없을 경우, 이 메서드 호출로 서버 콘솔에 자동으로 토픽 생성 (생성하는데 몇 시간 걸림)
			FirebaseMessaging.getInstance().subscribeToTopic(Key.CHANNEL_COMMON);

			// FCM Token 서버에 전달
			MyFirebaseMessagingService.sendRegistrationToServer(MainActivity.this);

			requestGpsTransmitCheck();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

    private void logout()
	{
        ConfirmCancelDialog.show(this, "자동로그인을 해제하시겠습니까?", "취소", "확인", true, new ConfirmCancelDialog.DialogListener() {
            public void onCancel()
			{

            }

            public void onConfirm()
			{
                SharedPreferences.Editor spe = getSharedPreferences("DTMS", 0).edit();
                spe.putBoolean("chkLogin", false);
				spe.commit();

				showToast("자동로그인이 해제되었습니다.", false);

				/*GPS 서비스 종료*/
				requestGpsServiceStop();

				finish();
				ActivityOptions options = ActivityOptions.makeCustomAnimation(MainActivity.this, R.anim.fade_in, R.anim.fade_out);
				Intent intent = new Intent(getContext(), LoginActivity.class);
				// 앱 새로 실행 | 모든 Activity 삭제
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent, options.toBundle());
            }
        });
    }

	protected void onResume()
	{
		super.onResume();
	}

	protected void onPause()
	{
		super.onPause();
	}

	protected void onRestart()
	{
		super.onRestart();
	}

	protected void onDestroy()
	{
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsServiceReceiver);
	}

	/* Service 를 시작시킨 Activity 종료 시, Service 도 함께 종료 */
	private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
		String TAG = ServiceConnection.class.getSimpleName();

		public void onServiceConnected(ComponentName componentName, IBinder service)
        {
			mGpsTrackingService = ((GpsTrackingService.LocalBinder) service).getService();
			Log.e(TAG, "+ onServiceConnected(): succeed");
		}

		public void onServiceDisconnected(ComponentName componentName)
        {
			Log.e(TAG, "+ onServiceDisconnected(): ");
			mGpsTrackingService = null;
		}
	};

	private final BroadcastReceiver gpsServiceReceiver = new BroadcastReceiver()
    {
		String TAG = "gpsServiceReceiver";

		public void onReceive(Context context, Intent intent)
		{
			try
			{
				if (intent == null)
					return;

				String action = intent.getAction();

				if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_START))
				{
					Intent serviceIntent = new Intent(MainActivity.this, GpsTrackingService.class);
					startService(serviceIntent);
//					startGpsTrackingService();
				}

				else if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_STOP))
				{
					Intent serviceIntent = new Intent(MainActivity.this, GpsTrackingService.class);
					stopService(serviceIntent);
//					stopGpsTrackingService();
				}

				else if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_STATE))
				{

				}

				else if (action.equalsIgnoreCase(Key.ACTION_GPS_SERVICE_LOCATION_UPDATED))
				{

				}

			} catch (Exception e)

			{
				e.printStackTrace();
			}
		}
	};

	private void stopGpsTrackingService()
	{
		Intent serviceIntent = new Intent(this, GpsTrackingService.class);
		stopService(serviceIntent);
	}

	private synchronized void requestGpsServiceStop()
	{
		Intent intent = new Intent(Key.ACTION_GPS_SERVICE_STOP);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressLint ("StaticFieldLeak")
	private synchronized void requestGpsTransmitCheck()
	{
		if (onReqGpsTransmitCheck)
			return;

		if (Key.getUserInfo() == null)
			return;

		startGpsTrackingService();
	}

	private void startGpsTrackingService()
	{
		Intent serviceIntent = new Intent(this, GpsTrackingService.class);

		/** 홈 화면>모든 앱 종료시 Service 유지 **/
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
		{
			startService(serviceIntent);
		}
		// Oreo
		else {
			startForegroundService(serviceIntent);
		}
	}

    public void onClick(View v)
	{
    	InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId())
		{
			case R.id.btnMenu1:
				goRequestDispatch();
				break;

			case R.id.btnMenu2:
				goDispatchList();
				break;

			case R.id.btnMenu3:
				goLoadList();
				break;

			case R.id.btnMenu4:
				goPallet();
				break;

			case R.id.btnMenu5:
				goRecord();
				break;

			case R.id.btnMenu6:
				goNotice();
				break;

			case R.id.layout_call:
				ConfirmCancelDialog.show(MainActivity.this, "고객센터에 전화를 거시겠습니까?", "취소", "확인", true, new ConfirmCancelDialog.DialogListener()
				{
					public void onCancel()
					{

					}
					public void onConfirm()
					{
						AppUtil.runCallApp("0000-0000", true);
					}
				});
				break;

			case R.id.btn_menu:
				dLayout.openDrawer(Gravity.RIGHT);
				break;

			case R.id.menu_close:
				dLayout.closeDrawer(Gravity.RIGHT);
				break;
        }
    }

	@SuppressLint("StaticFieldLeak")
	private synchronized void editPassword(String userPw)
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
						payloadJson.put("userId", Key.getUserInfo().getString("USER_ID"));
						payloadJson.put("userPw", userPw);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					return RestRequestor.requestPost(API.URL_CHANGE_PWD, false, payloadJson, true, false);
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
							showToast("비밀번호가 변경되었습니다.", false);

						}
						else
						{
							showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
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

	public void goRequestDispatch()
	{
		Intent intent = new Intent(this, DispatchRequestActivity.class);
		startActivityForResult(intent, 100);
	}

	public void goDispatchList()
	{
		Intent intent = new Intent(this, DispatchListActivity.class);
		startActivityForResult(intent, 100);
	}

	public void goLoadList()
	{
		Intent intent = new Intent(this, LoadListActivity.class);
		startActivityForResult(intent, 100);
	}

	public void goPallet()
	{
		Intent intent = new Intent(this, PalletActivity.class);
		intent.putExtra("DEPT_NM", Key.getUserInfo().getString("USER_DEPT_NM"));
		intent.putExtra("CAR_NO", Key.getUserInfo().getString("PRF_NM"));
		startActivity(intent);
	}

	public void goRecord()
	{
		Intent intent = new Intent(this, RecordListActivity.class);
		startActivity(intent);
	}

	public void goNotice()
	{
		ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_from_right, R.anim.fade_out);
		startActivity(new Intent(this, NoticeListActivity.class), options.toBundle());
	}

	public void onBackPressed()
	{
		ConfirmCancelDialog.show(this, "앱을 종료하시겠습니까? \n관제도 종료됩니다.", "취소", "확인", false, new ConfirmCancelDialog.DialogListener() {
			public void onCancel()
			{

			}

			public void onConfirm()
			{
				/*GPS 서비스 종료*/
				requestGpsServiceStop();
				finish();
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}
}