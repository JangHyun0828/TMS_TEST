package com.neognp.ytms.carowner.account;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.DeviceUtil;
import com.trevor.library.web.MyWebChromeClient;
import com.trevor.library.web.MyWebViewClient;

import org.json.JSONObject;

public class TermActivity extends BasicActivity {

    private boolean onReq;
    private Bundle args;

    private WebView contentWeb;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.term_activity);

        contentWeb = (WebView) findViewById(R.id.contentWeb);
        contentWeb.setWebViewClient(new MyWebViewClient(this)); // 앱에서 url 직접 처리
        contentWeb.setWebChromeClient(new MyWebChromeClient(this));
        //contentWeb.clearCache(true); // cache 사용 금지
        contentWeb.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // cache 사용
        contentWeb.getSettings().setSupportZoom(true);
        contentWeb.getSettings().setBuiltInZoomControls(true);
        contentWeb.getSettings().setDisplayZoomControls(false);
        contentWeb.getSettings().setUseWideViewPort(true);
        contentWeb.getSettings().setLoadWithOverviewMode(true);
        contentWeb.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        contentWeb.setScrollbarFadingEnabled(false);
        contentWeb.getSettings().setSupportMultipleWindows(true);
        init();
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    private void init() {
        try {
            args = getIntent().getExtras();
            String key = args.getString("key");
            String title = "";
            String url = "http://" + API.DEFAULT_IP + ":" + API.DEFAULT_PORT;
            if(key.equals("privacy"))
            {
                title = "개인정보취급방침";
                url = url + API.URL_WEB_PRIVACY;
            }
            else if(key.equals("location"))
            {
                title = "위치정보제공동의";
                url = url + API.URL_WEB_LOCATION;
            }

            setTitleBar(title, R.drawable.selector_button_back, 0, 0);
            contentWeb.loadUrl(url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (contentWeb.canGoBack()) {
                        contentWeb.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                finish();
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}