package com.neognp.dtmsapp_v1.popup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neognp.dtmsapp_v1.R;
import com.trevor.library.template.BasicDialog;

@SuppressLint ("ValidFragment")
public class ConfirmDialog extends BasicDialog implements DialogInterface.OnCancelListener {

    private String msg;
    private DialogListener listener;

    private View contentView;

    public ConfirmDialog(String msg, DialogListener listener) {
        setCancelable(true);
        this.msg = msg;
        this.listener = listener;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.confirm_dialog, null, false);

        TextView msgTxt = (TextView) contentView.findViewById(R.id.msgTxt);
        msgTxt.setText(this.msg);

        // 팝업 및 팝업 밖 터치 시 닫기
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (listener != null)
            listener.onCancel();
    }

    public static ConfirmDialog show(Activity activity, String msg, DialogListener listener) {
        ConfirmDialog dialog = new ConfirmDialog(msg, listener);
        return (ConfirmDialog) dialog.show((AppCompatActivity)activity);
    }

    public interface DialogListener {
        void onCancel();
    }

}
