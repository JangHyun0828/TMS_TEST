package com.neognp.ytms.popup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.template.BasicDialog;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressLint ("ValidFragment")
public class PersonalDaySurveyDialog extends BasicDialog implements DialogInterface.OnCancelListener {

    private boolean onReq;

    public static final SimpleDateFormat SDF_YEAR = new SimpleDateFormat("yyyy", Locale.getDefault());
    public static final SimpleDateFormat SDF_MMDDE = new SimpleDateFormat("MM월 dd일 (E)", Locale.getDefault());

    private String notiCd, date, replyYn;

    private View contentView;
    private RadioGroup surveyGroup;

    public PersonalDaySurveyDialog(String notiCd, String date, String replyYn) {
        setCancelable(true);
        this.notiCd = notiCd;
        this.date = date;
        this.replyYn = replyYn;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        contentView = inflater.inflate(R.layout.personal_day_survey_dialog, null, false);

        TextView yearTxt = contentView.findViewById(R.id.yearTxt);
        TextView dateTxt = contentView.findViewById(R.id.dateTxt);

        surveyGroup = contentView.findViewById(R.id.surveyGroup);

        contentView.findViewById(com.trevor.library.R.id.btmBtn0).setOnClickListener(btnListener);
        contentView.findViewById(com.trevor.library.R.id.btmBtn1).setOnClickListener(btnListener);

        // 팝업 및 팝업 밖 터치 시 닫기
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        try {
            if (date != null) {
                yearTxt.setText(SDF_YEAR.format(Key.SDF_PAYLOAD.parse(date)));
                dateTxt.setText(SDF_MMDDE.format(Key.SDF_PAYLOAD.parse(date)));
            }

            if (replyYn != null) {
                if (replyYn.equalsIgnoreCase("N"))
                    ((RadioButton) contentView.findViewById(R.id.personalDayRadio)).setChecked(true);
                else if (replyYn.equalsIgnoreCase("Y"))
                    ((RadioButton) contentView.findViewById(R.id.dutyDayRadio)).setChecked(true);
                else if (replyYn.equalsIgnoreCase("X"))
                    ((RadioButton) contentView.findViewById(R.id.unsettledRadio)).setChecked(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return contentView;
    }

    private View.OnClickListener btnListener = new View.OnClickListener() {
        public void onClick(View v) {
            int id = v.getId();

            if (id == com.trevor.library.R.id.btmBtn0) {
                hide();
            } else if (id == com.trevor.library.R.id.btmBtn1) {
                int optionIdx = surveyGroup.getCheckedRadioButtonId();
                if (optionIdx == -1) {
                    ((BasicActivity) getActivity()).showToast("휴무 여부를 선택해 주십시요.", true);
                    return;
                }

                String replyYn = "";
                switch (optionIdx) {
                    // 휴무
                    case R.id.personalDayRadio:
                        replyYn = "N";
                        break;
                    // 근무
                    case R.id.dutyDayRadio:
                        replyYn = "Y";
                        break;
                    // 미정
                    case R.id.unsettledRadio:
                        replyYn = "X";
                        break;
                    default:
                        replyYn = "X";
                }

                requestPersonalDaySelection(replyYn);
            }
        }
    };

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    public static PersonalDaySurveyDialog show(Activity activity, String notiCd, String date, String replyYn) {
        PersonalDaySurveyDialog dialog = new PersonalDaySurveyDialog(notiCd, date, replyYn);
        return (PersonalDaySurveyDialog) dialog.show((AppCompatActivity) activity);
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestPersonalDaySelection(String replyYn) {
        if(!isAdded())
            return;

        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            if (notiCd == null || replyYn == null)
                return;

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    ((BasicActivity) getActivity()).showLoadingDialog(null, true);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("notiCd", notiCd);
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("clientCd", Key.getUserInfo().getString("CLIENT_CD"));
                        payloadJson.put("replyYn", replyYn);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_NOTICE_REPLY, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    ((BasicActivity) getActivity()).dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            ((BasicActivity) getActivity()).showToast("휴무일 조사가 저장됐습니다.", true);
                        } else {
                            ((BasicActivity) getActivity()).showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ((BasicActivity) getActivity()).showToast(e.getMessage(), false);
                    }

                    hide();
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
