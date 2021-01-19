package com.neognp.ytms.notice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.neognp.ytms.R;

import com.trevor.library.template.BasicActivity;

public class NoticeDetailActivity extends BasicActivity {

    private Bundle args;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notice_detail_activity);

        setTitleBar("공지사항", R.drawable.selector_button_back, 0, 0);

        init();
    }

    private void init() {
        try {
            args = getIntent().getExtras();
            if (args == null)
                return;

            ((TextView) findViewById(R.id.subjectTxt)).setText(args.getString("NOTI_TITLE", ""));
            ((TextView) findViewById(R.id.contentTxt)).setText(args.getString("NOTI_CONT", ""));
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
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}