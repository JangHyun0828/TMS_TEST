package com.neognp.ytms.popup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.trevor.library.template.BasicDialog;
import com.trevor.library.util.AppUtil;

@SuppressLint ("ValidFragment")
public class LocationInfoDialog extends BasicDialog {

    private View contentView;

    private String title, address, name, phoneNo;

    public LocationInfoDialog(String title, String address, String name, String phoneNo) {
        setCancelable(true);
        this.title = title;
        this.address = address;
        this.name = name;
        this.phoneNo = phoneNo;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.locaton_info_dialog, null, false);

        ((TextView) contentView.findViewById(R.id.titleTxt)).setText(title);

        ((TextView) contentView.findViewById(R.id.addressTxt)).setText("주소 : " + address);

        ((TextView) contentView.findViewById(R.id.nameTxt)).setText("담당자 : " + name);

        TextView phoneNoTxt = ((TextView) contentView.findViewById(R.id.phoneNoTxt));
        SpannableString content = new SpannableString(phoneNo);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        phoneNoTxt.setText(content);
        phoneNoTxt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AppUtil.runCallApp(phoneNo, true);
            }
        });

        contentView.findViewById(R.id.btmBtn0).setOnClickListener(btnListener);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private View.OnClickListener btnListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btmBtn0:
                    hide();
                    break;
            }
        }
    };

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    public static LocationInfoDialog show(Activity activity, String title, String address, String name, String phonNo) {
        LocationInfoDialog dialog = new LocationInfoDialog(title, address, name, phonNo);
        return (LocationInfoDialog) dialog.show((AppCompatActivity) activity);
    }

}
