package com.neognp.dtmsapp_v1.car;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.google.android.gms.common.GoogleApiAvailability;
import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;


public class DispatchRequestActivity extends BasicActivity {

	private boolean onReq;

	private String nowStatus;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.car_dispatch_request_activity);

		init();
	}

	private void init()
	{
		try
		{
			//GoogleHandler Play 서비스 호환 여부 체크
			GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);

			checkDispatch();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	protected void onResume()
	{
		super.onResume();
		checkDispatch();
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
			case R.id.dispatch_request:
				if(nowStatus.equals("10"))
				{
					showToast("배차신청이 완료된 상태입니다.", true);
					return;
				}
				requestDispatch();
				break;

			case R.id.dispatch_cancel:
				if(nowStatus.equals("10"))
				{

				}
				else
				{
					showToast("이미 배차취소가 불가능한 상태입니다.", true);
					return;
				}
				requestDispatch();
				break;
        }
    }

	@SuppressLint("StaticFieldLeak")
	private synchronized void checkDispatch()
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
						payloadJson.put("carCd", Key.getUserInfo().getString("USER_CD"));
						payloadJson.put("driverHp", Key.getUserInfo().getString("USER_ID"));
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					return RestRequestor.requestPost(API.URL_CHECK_DISPATCH, false, payloadJson, true, false);
				}

				protected void onPostExecute(Bundle response)
				{
					onReq = false;
					dismissLoadingDialog();

					try
					{
						Bundle resBody = response.getBundle(Key.resBody);
						String result_code = resBody.getString(Key.result_code);
						String result_status = resBody.getString("now_status", "");
						String status_msg = resBody.getString("status_msg", "");

						if (result_code.equals("200"))
						{
							nowStatus = result_status;

							Button requestBtn = (Button) findViewById(R.id.dispatch_request);
							Button cancelBtn = (Button) findViewById(R.id.dispatch_cancel);

							if(nowStatus.equals("10"))
							{
								requestBtn.setBackgroundResource(R.drawable.selector_button_round_gray);
								cancelBtn.setBackgroundResource(R.drawable.selector_button_round_navy);
							}
							else
							{
								requestBtn.setBackgroundResource(R.drawable.selector_button_round_navy);
								cancelBtn.setBackgroundResource(R.drawable.selector_button_round_gray);
							}
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

	@SuppressLint("StaticFieldLeak")
	private synchronized void requestDispatch()
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

					String request = "";

					try
					{
						payloadJson = RestRequestor.buildPayload();
						payloadJson.put("carCd", Key.getUserInfo().getString("USER_CD"));
						payloadJson.put("driverHp", Key.getUserInfo().getString("USER_ID"));
						if(nowStatus.equals("10"))
						{
							request = "cancel";
						}
						else
						{
							request = "request";
						}
						payloadJson.put("request", request);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					return RestRequestor.requestPost(API.URL_REQUEST_DISPATCH, false, payloadJson, true, false);
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
						String status_msg = resBody.getString("status_msg", "");

						if (result_code.equals("200"))
						{
							checkDispatch();
							showToast(status_msg, true);
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

	public void onBackPressed()
	{
		setResult(RESULT_OK);
		finish();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}
}