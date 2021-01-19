package com.neognp.dtmsapp_v1.intro;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;

import androidx.core.app.ActivityCompat;

import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.login.LoginActivity;
import com.trevor.library.popup.ConfirmCancelDialog;
import com.trevor.library.template.BasicActivity;

public class IntroActivity extends BasicActivity
{
	private final long DELAY_MILLIS = 1500L;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.intro_activity);

		if (!hasPermissions())
			requestPermissions();
		else
			init(true);

		//statusBar 색상 지정하기
		View view = getWindow().getDecorView();
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			if(view != null)
			{
				view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
			}
		}
	}

	private void init(boolean delay)
	{
		if (delay)
		{
			new Handler().postDelayed(new Runnable()
			{
				public void run()
				{
					showLoginActivity();
				}
			}, DELAY_MILLIS);
		}
		else
		{
				showLoginActivity();
		}
	}

	private void showLoginActivity()
	{
		finish();
		ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out);
		Intent intent = new Intent(getContext(), LoginActivity.class);
		// 앱 새로 실행 | 모든 Activity 삭제
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent, options.toBundle());
	}

	public static final int REQ_PERMISSION_ALL = 1000;

	//@formatter:off
	private String[] permissions =
	{
			Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.CALL_PHONE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_BACKGROUND_LOCATION,
			Manifest.permission.CAMERA
	};
	//@formatter:on
	private boolean hasPermissions()
	{
		//if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
		//    return true;

		for (String permission : permissions)
		{
			if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
			{
				return false;
			}
		}

		return true;
	}

	private void requestPermissions()
	{
		ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSION_ALL);
	}

	@Override
	public void onRequestPermissionsResult(int permissionCode, String permissions[], int[] grantResults)
	{
		try
		{
			switch (permissionCode)
			{
				case REQ_PERMISSION_ALL:
				{
					boolean allPermissionEnabled = true;
					for (int grandResult : grantResults)
					{
//						if (grandResult != PackageManager.PERMISSION_GRANTED)
//						{
//							allPermissionEnabled = false;
//						}
					}

					if (allPermissionEnabled)
						init(false);
					else
						showPermissionDeniedDialog();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void showPermissionDeniedDialog() {
		String msg = "앱 권한 허용을 묻는 팝업에서 모든 권한을 허용해 주십시요.\n\n또는, 설정>애플리케이션(앱)>" + getString(R.string.app_name) + ">권한>모든 권한을 활성화해 주십시요";

		ConfirmCancelDialog.show(this, msg, "종료", "권한 설정", false, new ConfirmCancelDialog.DialogListener() {
			public void onCancel() {
			}

			public void onConfirm() {
				try {
					finish();
					Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					Uri uri = Uri.fromParts("package", getPackageName(), null);
					intent.setData(uri);
					startActivityForResult(intent, 100);
				} catch (Exception e) {
				}
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}
}