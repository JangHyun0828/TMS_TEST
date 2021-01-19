package com.neognp.dtmsapp_v1.menu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neognp.dtmsapp_v1.R;
import com.trevor.library.template.BasicDialog;

@SuppressLint ("ValidFragment")
public class PwdChangeDialog extends BasicDialog
{

    private View contentView;

    private String leftBtnStr, rightBtnStr, strPwd, strPwdCheck;
    private boolean cancelable;
    private DialogListener listener;
    private EditText edtPwd, edtPwdCheck, pwdCheck;

    public PwdChangeDialog(boolean cancelable, DialogListener listener)
    {
        this.cancelable = cancelable;
        setCancelable(cancelable);
        this.listener = listener;
    }

    public PwdChangeDialog(String leftBtnStr, String rightBtnStr, boolean cancelable, DialogListener listener)
    {
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
        contentView = inflater.inflate(R.layout.password_change_dialog, null, false);

        contentView.findViewById(R.id.btmBtn0).setOnClickListener(btnListener);
        contentView.findViewById(R.id.btmBtn1).setOnClickListener(btnListener);

        edtPwd = ((EditText) contentView.findViewById(R.id.edtNewPwd));
        edtPwdCheck = ((EditText) contentView.findViewById(R.id.edtNewPwdCheck));

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

        pwdCheck = (EditText) contentView.findViewById(R.id.edtNewPwdCheck);

        pwdCheck.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    ((Button)contentView.findViewById(R.id.btmBtn1)).performClick();
                }
                return false;
            }
        });

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
                    strPwd = edtPwd.getText().toString().trim();
                    strPwdCheck = edtPwdCheck.getText().toString().trim();
                    String flag = "";

                    if(strPwd.equalsIgnoreCase("") || strPwdCheck.equalsIgnoreCase(""))
                    {
                       flag = "A";
                    }

                    if(!strPwd.equalsIgnoreCase(strPwdCheck))
                    {
                        flag = "B";
                    }

                    if (listener != null)
                    {
                        if(flag.equalsIgnoreCase(""))
                        {
                            hide();
                        }
                        listener.onConfirm(strPwd, flag);
                    }
                    break;
            }
        }
    };

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        super.onDismiss(dialog);
    }

    public static PwdChangeDialog show(Activity activity, boolean cancelable, DialogListener listener)
    {
        PwdChangeDialog dialog = new PwdChangeDialog(cancelable, listener);
        return (PwdChangeDialog) dialog.show((AppCompatActivity) activity);
    }

    public static PwdChangeDialog show(Activity activity, String leftBtnStr, String rightBtnStr, boolean cancelable, DialogListener listener)
    {
        PwdChangeDialog dialog = new PwdChangeDialog(leftBtnStr, rightBtnStr, cancelable, listener);
        return (PwdChangeDialog) dialog.show((AppCompatActivity) activity);
    }

    public interface DialogListener
    {
        void onCancel();

        void onConfirm(String pwd, String flag);
    }

}