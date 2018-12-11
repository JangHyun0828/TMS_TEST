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
import android.widget.TextView;

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
public class CountEditDialog extends BasicDialog {

    private String title;
    private int maxLength;
    private String initCount;

    private View contentView;
    private TextInputLayout nameEditLayout;
    private NoCursorMenuEditText countEdit;

    private DialogListener listener;

    public CountEditDialog(String title, int maxLength, DialogListener listener) {
        setCancelable(true);
        this.title = title;
        this.maxLength = maxLength;
        this.listener = listener;
    }

    public CountEditDialog(String title, int maxLength, String initCount, DialogListener listener) {
        setCancelable(true);
        this.title = title;
        this.maxLength = maxLength;
        this.initCount = initCount;
        this.listener = listener;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.count_edit_dialog, null, false);

        ((TextView) contentView.findViewById(R.id.titleTxt)).setText(title);

        nameEditLayout = contentView.findViewById(R.id.countEditLayout);
        nameEditLayout.setHintEnabled(false);

        countEdit = contentView.findViewById(R.id.countEdit);
        InputFilter[] filter = new InputFilter[1];
        filter[0] = new InputFilter.LengthFilter(maxLength);
        countEdit.setFilters(filter);

        countEdit.addTextChangedListener(new TextWatcher() {
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

        // 초기값이 0이면 초기값 미표시
        if (initCount != null && !initCount.equals("0")) {
            countEdit.setText(initCount);
            countEdit.setSelection(countEdit.getText().length());
        }

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
                    String count = countEdit.getText().toString().trim();
                    if (count.isEmpty()) {
                        nameEditLayout.setErrorEnabled(true);
                        nameEditLayout.setError("수량을 입력해 주세요.");
                        return;
                    } else {
                        hide();
                        if (listener != null)
                            listener.onConfirm(count);
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

    public static CountEditDialog show(Activity activity, String title, int maxLength, DialogListener listener) {
        CountEditDialog dialog = new CountEditDialog(title, maxLength, listener);
        return (CountEditDialog) dialog.show((AppCompatActivity) activity);
    }

    public static CountEditDialog show(Activity activity, String title, int maxLength, String initCount, DialogListener listener) {
        CountEditDialog dialog = new CountEditDialog(title, maxLength, initCount, listener);
        return (CountEditDialog) dialog.show((AppCompatActivity) activity);
    }

    public interface DialogListener {
        void onCancel();

        void onConfirm(String count);
    }

}
