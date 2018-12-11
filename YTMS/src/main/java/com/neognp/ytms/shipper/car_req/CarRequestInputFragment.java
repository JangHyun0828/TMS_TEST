package com.neognp.ytms.shipper.car_req;


import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicFragment;

import org.json.JSONObject;

import java.util.Calendar;

public class CarRequestInputFragment extends BasicFragment implements View.OnClickListener {

    private boolean onReq;

    private Calendar curCal;
    private int palletsCnt, carsCnt;

    private View contentView;
    private Button curDateBtn;

    private TextView palletsFixedCntTxt, carsFixedCntTxt;
    private EditText palletsNewCntEdit, carsNewCntEdit;

    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public void onDetach() {
        super.onDetach();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.car_request_input_fragment, container, false);

        curCal = Calendar.getInstance();
        curCal.set(Calendar.HOUR_OF_DAY, 0);
        curCal.set(Calendar.MINUTE, 0);
        curCal.set(Calendar.SECOND, 0);
        curCal.set(Calendar.MILLISECOND, 0);

        curDateBtn = contentView.findViewById(R.id.curDateBtn);
        curDateBtn.setOnClickListener(this);
        curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));

        contentView.findViewById(R.id.prevDateBtn).setOnClickListener(this);
        contentView.findViewById(R.id.nextDateBtn).setOnClickListener(this);

        palletsFixedCntTxt = contentView.findViewById(R.id.palletsFixedCntTxt);
        View palletsInputView = contentView.findViewById(R.id.palletsInputView);
        palletsNewCntEdit = palletsInputView.findViewById(R.id.itemsCntEdit);
        palletsInputView.findViewById(R.id.minusBtn).setOnClickListener(palletsInputListener);
        palletsInputView.findViewById(R.id.plusBtn).setOnClickListener(palletsInputListener);

        carsFixedCntTxt = contentView.findViewById(R.id.carsFixedCntTxt);
        View carsInputView = contentView.findViewById(R.id.carsInputView);
        carsNewCntEdit = carsInputView.findViewById(R.id.itemsCntEdit);
        carsInputView.findViewById(R.id.minusBtn).setOnClickListener(carsInputListener);
        carsInputView.findViewById(R.id.plusBtn).setOnClickListener(carsInputListener);

        contentView.findViewById(R.id.bottomBtn0).setOnClickListener(this);
        contentView.findViewById(R.id.bottomBtn1).setOnClickListener(this);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    private void init() {
        try {
            search();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.prevDateBtn:
                setPrevDate();
                break;
            case R.id.curDateBtn:
                showCalendar();
                break;
            case R.id.nextDateBtn:
                setNextDate();
                break;
            case R.id.bottomBtn0:
                requestCountSave();
                break;
            case R.id.bottomBtn1:
                startActivity(new Intent(getActivity(), CarRequestHistoryActivity.class));
                break;
        }
    }

    private View.OnClickListener palletsInputListener = new View.OnClickListener() {
        public void onClick(View v) {
            InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

            switch (v.getId()) {
                case R.id.minusBtn:
                    if (palletsCnt - 1 >= 0) {
                        palletsCnt--;
                        palletsNewCntEdit.setText("" + palletsCnt);
                    }
                    break;
                case R.id.plusBtn:
                    palletsCnt++;
                    palletsNewCntEdit.setText("" + palletsCnt);
                    break;
            }

            palletsNewCntEdit.setSelection(palletsNewCntEdit.getText().length());
        }
    };

    private View.OnClickListener carsInputListener = new View.OnClickListener() {
        public void onClick(View v) {
            InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

            switch (v.getId()) {
                case R.id.minusBtn:
                    if (carsCnt - 1 >= 0) {
                        carsCnt--;
                        carsNewCntEdit.setText("" + carsCnt);
                    }
                    break;
                case R.id.plusBtn:
                    carsCnt++;
                    carsNewCntEdit.setText("" + carsCnt);
                    break;
            }

            carsNewCntEdit.setSelection(carsNewCntEdit.getText().length());
        }
    };

    private void showCalendar() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

                curCal.set(Calendar.YEAR, year);
                curCal.set(Calendar.MONTH, monthOfYear);
                curCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));

                search();
            }
        }, curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH), curCal.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void setPrevDate() {
        curCal.add(Calendar.DAY_OF_YEAR, -1);
        curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
        search();
    }

    private void setNextDate() {
        curCal.add(Calendar.DAY_OF_YEAR, 1);
        curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
        search();
    }

    private void search() {
        try {
            // 오늘 날짜 이후로 설정 금지
            //Calendar todayCal = Calendar.getInstance();
            //todayCal.set(Calendar.HOUR_OF_DAY, 0);
            //todayCal.set(Calendar.MINUTE, 0);
            //todayCal.set(Calendar.SECOND, 0);
            //todayCal.set(Calendar.MILLISECOND, 0);
            //if (curCal.after(todayCal)) {
            //    showSnackbar(R.drawable.ic_insert_invitation_black_24dp, "조회 종료일은 오늘 날짜 이후로 지정할 수 없습니다.");
            //    return;
            //}

            requestCount();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestCount() {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            final String requestDt = Key.SDF_PAYLOAD.format(curCal.getTime());

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("custCd", Key.getUserInfo().getString("CLIENT_CD"));
                        payloadJson.put("requestDt", requestDt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_SHIPPER_CAR_REQUEST_CNT, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            Bundle data = resBody.getBundle(Key.data);
                            setData(data);
                        } else {
                            showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast(e.getMessage(), false);
                    }
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setData(Bundle data) {
        if (data == null)
            return;

        try {
            palletsFixedCntTxt.setText(data.getString("PALLET_CNT", "0"));
            palletsCnt = Integer.parseInt(data.getString("C_PALLET_CNT", "0"));
            palletsNewCntEdit.setText("" + palletsCnt);
            palletsNewCntEdit.setSelection(palletsNewCntEdit.getText().length());

            carsFixedCntTxt.setText(data.getString("CAR_CNT", "0"));
            carsCnt = Integer.parseInt(data.getString("C_PALLET_CNT", "0"));
            carsNewCntEdit.setText("" + carsCnt);
            carsNewCntEdit.setSelection(carsNewCntEdit.getText().length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestCountSave() {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            final String requestDt = Key.SDF_PAYLOAD.format(curCal.getTime());

            String palletCnt = palletsNewCntEdit.getText().toString();
            if (palletCnt.isEmpty() || palletCnt.equals("0") || palletCnt.equals("-")) {
                showToast("팔레트 수를 1개 이상 입력해 주십시요.", true);
                return;
            }

            //String carCnt = carsNewCntEdit.getText().toString();
            //if (carCnt.isEmpty() || carCnt.equals("0") || carCnt.equals("-")) {
            //    showToast("차량 대수를 1대 이상 입력해 주십시요.", true);
            //    return;
            //}
            // 임시
            String carCnt = "1";

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("custCd", Key.getUserInfo().getString("CLIENT_CD"));
                        payloadJson.put("requestDt", requestDt);
                        payloadJson.put("palletCnt", palletCnt);
                        payloadJson.put("carCnt", carCnt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_SHIPPER_CAR_REQUEST_CNT_SAVE, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            showToast("차량 요청이 저장되었습니다.", true);
                        } else {
                            showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast(e.getMessage(), false);
                    }
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}