package com.neognp.dtmsapp_v1.admin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.fcm.MyFirebaseMessagingService;
import com.neognp.dtmsapp_v1.gps.GpsTrackingService;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.neognp.dtmsapp_v1.menu.PwdChangeDialog;
import com.neognp.dtmsapp_v1.popup.ConfirmCancelDialog;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;


public class MainActivity extends BasicActivity {

	private boolean onReq;
	private DrawerLayout dLayout;
	private GpsTrackingService mGpsTrackingService;
	private boolean onReqGpsTransmitCheck;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.admin_main_activity);

		((TextView)findViewById(R.id.txt_car_no)).setVisibility(View.INVISIBLE);

		TextView txtUserNm = (TextView) findViewById(R.id.txt_user_hello);
		String prf_user_nm = Key.getUserInfo().getString("USER_NM", "");
		SpannableString user_name = new SpannableString(prf_user_nm + "님, 안녕하세요.");
		user_name.setSpan(new ForegroundColorSpan(Color.parseColor("#0097D8")), 0, prf_user_nm.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		txtUserNm.setText(user_name);

		dLayout = (DrawerLayout) findViewById(R.id.dLayout);
		//String userNm = Key.getUserInfo().getString("USER_NM", "").replaceAll("님", "님\n");
		//((TextView)findViewById(R.id.menu_userNm)).setText(userNm);

        //비밀번호 변경
        dLayout.findViewById(R.id.menu_changePwd).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				PwdChangeDialog.show(MainActivity.this, false, new PwdChangeDialog.DialogListener()
				{
					@Override
					public void onCancel()
					{
						return;
					}

					@Override
					public void onConfirm(String userPwd, String flag)
					{
						if(flag.equalsIgnoreCase("A"))
						{
							showToast("비밀번호를 입력 후 다시 시도해주십시오.", false);
						}
						else if(flag.equalsIgnoreCase("B"))
						{
							showToast("입력하신 비밀번호가 일치하지 않습니다.", false);
						}
						else
						{
							//editPassword(userPwd);
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
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

    private void logout() {
        ConfirmCancelDialog.show(this, "자동로그인을 해제하시겠습니까?", "취소", "확인", true, new ConfirmCancelDialog.DialogListener() {
            public void onCancel() {
            }

            public void onConfirm() {
                SharedPreferences.Editor spe = getSharedPreferences("DTMS", 0).edit();
                spe.putBoolean("chkLogin", false);
				spe.commit();

				showToast("자동로그인이 해제되었습니다.", false);
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
	}

    public void onClick(View v)
	{
    	InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId())
		{
			case R.id.btnMenu1:
				goDispatch();
				break;

			case R.id.btnMenu2:
				goMap();
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

	public void goDispatch()
	{
		Intent intent = new Intent(getContext(), DispatchListActivity.class);
		startActivity(intent);
	}

	public void goMap()
	{
		Intent intent = new Intent(getContext(), LocationActivity.class);
		startActivity(intent);
	}

	public void onBackPressed()
	{
		ConfirmCancelDialog.show(this, "앱을 종료하시겠습니까?", "취소", "확인", false, new ConfirmCancelDialog.DialogListener() {
			public void onCancel()
			{

			}

			public void onConfirm()
			{
				finish();
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}
}