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
import android.widget.TextView;

import com.neognp.ytms.R;
import com.trevor.library.template.BasicDialog;
import com.trevor.library.util.AppUtil;

@SuppressLint ("ValidFragment")
public class CarAllocLocationInfoDialog extends BasicDialog {

    private View contentView;

    private String title;
    private String departure, address0, name0, phoneNo0;
    private String arrival, address1, name1, phoneNo1;
    private String remark;

    public CarAllocLocationInfoDialog(String title, String departure, String address0, String name0, String phoneNo0, String arrival, String address1, String name1, String phoneNo1, String remark) {
        setCancelable(true);

        this.title = title;

        this.departure = departure;
        this.address0 = address0;
        this.name0 = name0;
        this.phoneNo0 = phoneNo0;

        this.arrival = arrival;
        this.address1 = address1;
        this.name1 = name1;
        this.phoneNo1 = phoneNo1;

        this.remark = remark;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.car_alloc_locaton_info_dialog, null, false);

        ((TextView) contentView.findViewById(R.id.titleTxt)).setText(title);

        ((TextView) contentView.findViewById(R.id.departureTxt)).setText("상차지 : " + departure);
        ((TextView) contentView.findViewById(R.id.addressTxt0)).setText("주소 : " + address0);
        ((TextView) contentView.findViewById(R.id.nameTxt0)).setText("담당자 : " + name0);
        TextView phoneNoTxt0 = ((TextView) contentView.findViewById(R.id.phoneNoTxt0));
        SpannableString content = new SpannableString(phoneNo0);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        phoneNoTxt0.setText(content);
        phoneNoTxt0.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AppUtil.runCallApp(phoneNo0, true);
            }
        });

        ((TextView) contentView.findViewById(R.id.arrivalTxt)).setText("하차지 : " + arrival);
        ((TextView) contentView.findViewById(R.id.addressTxt1)).setText("주소 : " + address1);
        ((TextView) contentView.findViewById(R.id.nameTxt1)).setText("담당자 : " + name1);
        TextView phoneNoTxt1 = ((TextView) contentView.findViewById(R.id.phoneNoTxt1));
        content = new SpannableString(phoneNo1);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        phoneNoTxt1.setText(content);
        phoneNoTxt1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AppUtil.runCallApp(phoneNo1, true);
            }
        });

        ((TextView) contentView.findViewById(R.id.remarkTxt)).setText("비고 : " + remark);

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

    public static CarAllocLocationInfoDialog show(Activity activity, String title, String departure, String address0, String name0, String phoneNo0, String arrival, String address1, String name1, String phoneNo1, String remark) {
        CarAllocLocationInfoDialog dialog = new CarAllocLocationInfoDialog(title, departure, address0, name0, phoneNo0, arrival, address1, name1, phoneNo1, remark);
        return (CarAllocLocationInfoDialog) dialog.show((AppCompatActivity) activity);
    }

}
