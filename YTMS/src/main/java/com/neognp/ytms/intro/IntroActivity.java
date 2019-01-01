package com.neognp.ytms.intro;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;

import com.neognp.ytms.R;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.carowner.main.CarOwnerMainActivity;
import com.neognp.ytms.delivery.main.DeliveryMainActivity;
import com.neognp.ytms.login.LoginActivity;
import com.neognp.ytms.shipper.main.ShipperMainActivity;
import com.trevor.library.popup.ConfirmCancelDialog;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.Setting;

public class IntroActivity extends BasicActivity {

    private final long DELAY_MILLIS = 1500l;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_activity);

        if (!hasPermissions())
            requestPermissions();
        else
            init(true);
    }

    private void init(boolean delay) {
        if (delay) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    if (!Setting.getBoolean(Key.allowAutoLogin))
                        showLoginActivity();
                    else
                        showMainActivity();
                }
            }, DELAY_MILLIS);
        } else {
            if (!Setting.getBoolean(Key.allowAutoLogin))
                showLoginActivity();
            else
                showMainActivity();
        }
    }

    private void showLoginActivity() {
        finish();
        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out);
        Intent intent = new Intent(getContext(), LoginActivity.class);
        // 앱 새로 실행 | 모든 Activity 삭제
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent, options.toBundle());
    }

    public void showMainActivity() {
        if (Key.getUserInfo() == null) {
            showLoginActivity();
            return;
        }

        String AUTH_CD = Key.getUserInfo().getString("AUTH_CD");
        if (AUTH_CD == null) {
            showLoginActivity();
            return;
        }

        ActivityOptions options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_from_right, R.anim.fade_out);
        Intent intent = null;
        if (AUTH_CD.equals("CST")) {
            intent = new Intent(getContext(), ShipperMainActivity.class);
        } else if (AUTH_CD.equals("CAR")) {
            intent = new Intent(getContext(), CarOwnerMainActivity.class);
        } else if (AUTH_CD.equals("DC")) {
            intent = new Intent(getContext(), DeliveryMainActivity.class);
        } else if (AUTH_CD.equals("TPL")) {
            //intent = new Intent(getContext(), .class);
            showToast("준비중...", true);
            return;
        }

        // 앱 새로 실행 | 모든 Activity 삭제
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent, options.toBundle());
        finish();
    }

    public static final int REQ_PERMISSION_ALL = 1000;

    //@formatter:off
    private String[] permissions = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
    };
    //@formatter:on

    private boolean hasPermissions() {
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        //    return true;

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSION_ALL);
    }

    @Override
    public void onRequestPermissionsResult(int permissionCode, String permissions[], int[] grantResults) {
        try {
            switch (permissionCode) {
                case REQ_PERMISSION_ALL: {
                    boolean allPermissionEnabled = true;
                    for (int grandResult : grantResults) {
                        if (grandResult != PackageManager.PERMISSION_GRANTED) {
                            allPermissionEnabled = false;
                        }
                    }

                    if (allPermissionEnabled)
                        init(false);
                    else
                        showPermissionDeniedDialog();
                }
            }
        } catch (Exception e) {
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}