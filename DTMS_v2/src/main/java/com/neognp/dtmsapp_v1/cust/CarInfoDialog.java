package com.neognp.dtmsapp_v1.cust;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.popup.ConfirmCancelDialog;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.template.BasicDialog;
import com.trevor.library.util.AppUtil;

@SuppressLint ("ValidFragment")
public class CarInfoDialog extends BasicDialog
{
    private View contentView;

    private String title;
    private String driverNm, carNo, phoneNo, remark1, remark2;

    public CarInfoDialog(String title, String driverNm, String carNo, String phoneNo, String remark1, String remark2)
    {
        setCancelable(true);

        this.title = title;

        this.driverNm = driverNm;
        this.carNo = carNo;
        this.phoneNo = phoneNo;
        this.remark1 = remark1;
        this.remark2 = remark2;
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.car_info_dialog, null, false);

        ((TextView) contentView.findViewById(R.id.titleTxt)).setText(title);

        ((TextView) contentView.findViewById(R.id.driverNmTxt)).setText("기사명 : " + driverNm);
        ((TextView) contentView.findViewById(R.id.carNoTxt)).setText("차량번호 : " + carNo);
        TextView phoneNoTxt = ((TextView) contentView.findViewById(R.id.phoneNoTxt));
        SpannableString content = new SpannableString(phoneNo);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        phoneNoTxt.setText("연락처 : " + content);
        if(phoneNo != null)
        {
            phoneNoTxt.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    ConfirmCancelDialog.show(((BasicActivity) getActivity()), "해당 기사에게 전화 연결하시겠습니까?", "취소", "확인", true, new ConfirmCancelDialog.DialogListener() {
                        public void onCancel()
                        {

                        }

                        public void onConfirm()
                        {
                            AppUtil.runCallApp(phoneNo, true);
                        }
                    });
                }
            });
        }

        if(remark1.equals("") || remark1 == null)
        {
            ((TextView) contentView.findViewById(R.id.remarkTxt1)).setText("비고 : 없음");
        }
        else
        {
            ((TextView) contentView.findViewById(R.id.remarkTxt1)).setText("비고1 : " + remark1);
        }

        if(remark2.equals("") || remark2 == null)
        {
            ((TextView) contentView.findViewById(R.id.remarkTxt2)).setText("비고 : 없음");
        }
        else
        {
            ((TextView) contentView.findViewById(R.id.remarkTxt2)).setText("비고2 : " + remark2);
        }

        contentView.findViewById(R.id.btmBtn0).setOnClickListener(btnListener);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }

    private View.OnClickListener btnListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.btmBtn0:
                    hide();
                    break;
            }
        }
    };

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        super.onDismiss(dialog);
    }

    public static CarInfoDialog show(Activity activity, String title, String driverNm, String carNo, String phoneNo, String remark1, String remark2)
    {
        CarInfoDialog dialog = new CarInfoDialog(title, driverNm, carNo, phoneNo, remark1, remark2);
        return (CarInfoDialog) dialog.show((AppCompatActivity) activity);
    }

}
