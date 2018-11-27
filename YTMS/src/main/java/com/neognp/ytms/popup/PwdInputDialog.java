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
public class PwdInputDialog extends BasicDialog {

    private View contentView;
    private TextInputLayout nameEditLayout;
    private NoCursorMenuEditText pwdEdit;

    private DialogListener listener;

    public PwdInputDialog(DialogListener listener) {
        setCancelable(true);
        this.listener = listener;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.pwd_input_dialog, null, false);

        nameEditLayout = contentView.findViewById(R.id.countEditLayout);
        nameEditLayout.setHintEnabled(false);

        pwdEdit = contentView.findViewById(R.id.countEdit);

        pwdEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()) {
                    nameEditLayout.setErrorEnabled(false);
                    nameEditLayout.setHint(null);
                }
            }
        });

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
                    String userPwd = pwdEdit.getText().toString().trim();
                    if (userPwd.isEmpty()) {
                        nameEditLayout.setErrorEnabled(true);
                        nameEditLayout.setError("비밀번호를 입력해 주세요.");
                        return;
                    } else {
                        hide();
                        if (listener != null)
                            listener.onConfirm(userPwd);
                    }
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

    public static PwdInputDialog show(Activity activity, DialogListener listener) {
        PwdInputDialog dialog = new PwdInputDialog(listener);
        return (PwdInputDialog) dialog.show((AppCompatActivity) activity);
    }

    public interface DialogListener {
        void onCancel();

        void onConfirm(String count);
    }

}
