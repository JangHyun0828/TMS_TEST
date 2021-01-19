package com.neognp.dtmsapp_v1.popup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neognp.dtmsapp_v1.R;
import com.trevor.library.template.BasicDialog;

@SuppressLint ("ValidFragment")
public class ConfirmCancelDialog extends BasicDialog {

    private View contentView;

    private String msg, leftBtnStr, rightBtnStr;
    private boolean cancelable;
    private DialogListener listener;

    public ConfirmCancelDialog(String msg, boolean cancelable, DialogListener listener)
    {
        this.msg = msg;
        this.cancelable = cancelable;
        setCancelable(cancelable);
        this.listener = listener;
    }

    public ConfirmCancelDialog(String msg, String leftBtnStr, String rightBtnStr, boolean cancelable, DialogListener listener)
    {
        this.msg = msg;
        this.leftBtnStr = leftBtnStr;
        this.rightBtnStr = rightBtnStr;
        this.cancelable = cancelable;
        setCancelable(cancelable);
        this.listener = listener;
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.confirm_cancel_dialog, null, false);

        contentView.findViewById(R.id.btmBtn0).setOnClickListener(btnListener);
        contentView.findViewById(R.id.btmBtn1).setOnClickListener(btnListener);

        TextView msgTxt = (TextView) contentView.findViewById(R.id.msgTxt);
        msgTxt.setText(this.msg);

        if (leftBtnStr != null)
        {
            Button leftBtn = (Button) contentView.findViewById(R.id.btmBtn0);
            leftBtn.setText(leftBtnStr);
        }

        if (rightBtnStr != null)
        {
            Button rightBtn = (Button) contentView.findViewById(R.id.btmBtn1);
            rightBtn.setText(rightBtnStr);
        }

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
            switch (v.getId()) {
                case R.id.btmBtn0:
                    hide();
                    if (listener != null)
                        listener.onCancel();
                    break;
                case R.id.btmBtn1:
                    hide();
                    if (listener != null)
                        listener.onConfirm();
                    break;
            }
        }
    };

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    public static ConfirmCancelDialog show(Activity activity, String msg, boolean cancelable, DialogListener listener) {
        ConfirmCancelDialog dialog = new ConfirmCancelDialog(msg, cancelable, listener);
        return (ConfirmCancelDialog) dialog.show((AppCompatActivity) activity);
    }

    public static ConfirmCancelDialog show(Activity activity, String msg, String leftBtnStr, String rightBtnStr, boolean cancelable, DialogListener listener) {
        ConfirmCancelDialog dialog = new ConfirmCancelDialog(msg, leftBtnStr, rightBtnStr, cancelable, listener);
        return (ConfirmCancelDialog) dialog.show((AppCompatActivity) activity);
    }

    public interface DialogListener {
        void onCancel();

        void onConfirm();
    }

}
