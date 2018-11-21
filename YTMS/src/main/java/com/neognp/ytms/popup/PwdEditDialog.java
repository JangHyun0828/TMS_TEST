package com.neognp.ytms.popup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.neognp.ytms.R;
import com.trevor.library.template.BasicDialog;
import com.trevor.library.widget.NoCursorMenuEditText;

/* CAUTION
  갤럭시 S9+ 버그>팝업 안에 EditText 터치>커서의 마커가 자동으로 사라지기 전에 팝업 닫을 시, '
  Abort message: 'Failed to set damage region on surface 0x7765567910, error=EGL_BAD_ACCESS' 에러 발생
  ->
 EditText 대신 NoCursorMenuEditText를 사용하여 커서 마커가 뜨는 것을 차단
 */
@SuppressLint ("ValidFragment")
public class PwdEditDialog extends BasicDialog {

    private View contentView;
    private TextInputLayout pwdEditLayout0, pwdEditLayout1;
    private NoCursorMenuEditText pwdEdit0, pwdEdit1;

    private DialogListener listener;

    public PwdEditDialog(DialogListener listener) {
        setCancelable(true);
        this.listener = listener;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.pwd_edit_dialog, null, false);

        pwdEditLayout0 = contentView.findViewById(R.id.pwdEditLayout0);
        pwdEditLayout0.setErrorEnabled(true);
        pwdEdit0 = contentView.findViewById(R.id.pwdEdit0);

        pwdEditLayout1 = contentView.findViewById(R.id.pwdEditLayout1);
        pwdEditLayout1.setErrorEnabled(true);
        pwdEdit1 = contentView.findViewById(R.id.pwdEdit1);

        contentView.findViewById(R.id.btmBtn0).setOnClickListener(btnListener);
        contentView.findViewById(R.id.btmBtn1).setOnClickListener(btnListener);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private View.OnClickListener btnListener = new View.OnClickListener() {
        public void onClick(View v) {
            InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

            switch (v.getId()) {
                case R.id.btmBtn0:
                    hide();
                    if (listener != null)
                        listener.onCancel();
                    break;
                case R.id.btmBtn1:
                    String userPw0 = pwdEdit0.getText().toString().trim();
                    String userPw1 = pwdEdit1.getText().toString().trim();
                    if (userPw0.isEmpty()) {
                        pwdEditLayout0.setError("비밀번호를 입력해 주십시요.");
                        return;
                    }
                    if (userPw1.isEmpty()) {
                        pwdEditLayout1.setError("비밀번호를 입력해 주십시요.");
                        return;
                    }
                    if (!userPw0.equals(userPw1)) {
                        pwdEditLayout0.setError(null);
                        pwdEditLayout1.setError("재입력한 비밀번호가 일치하지 않습니다.");
                        return;
                    }
                    hide();
                    if (listener != null)
                        listener.onConfirm(userPw0);
                    break;
            }
        }
    };

    //@Override
    public void dismiss() {
        super.dismiss();
    }

    //@Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    public static PwdEditDialog show(Activity activity, DialogListener listener) {
        PwdEditDialog dialog = new PwdEditDialog(listener);
        return (PwdEditDialog) dialog.show((AppCompatActivity) activity);
    }

    public interface DialogListener {
        void onCancel();

        void onConfirm(String pwd);
    }

}
