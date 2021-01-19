package com.neognp.dtmsapp_v1.car;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.neognp.dtmsapp_v1.popup.PopClient;
import com.neognp.dtmsapp_v1.popup.PopPallet;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.ImageFilePath;

import org.json.JSONObject;

import java.util.Calendar;

public class PalletInActivity extends BasicActivity {

    public static final int RESULT_INPUT_PALLETS_COUNT = 100;
    private static final int REQ_PICK_IMAGE = 400;

    private boolean onReq;
    private Bundle args;

    private Calendar fromCal, toCal, curCal;
    private Button btnDate, btnPrevDate, btnNextDate;

    private int palletsCnt;

    private EditText palletsCntEdit, edtRemark;

    private String saveGb;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_pallets_in_activity);

        curCal = Calendar.getInstance();
        curCal.set(Calendar.HOUR_OF_DAY, 0);
        curCal.set(Calendar.MINUTE, 0);
        curCal.set(Calendar.SECOND, 0);
        curCal.set(Calendar.MILLISECOND, 0);

        //날짜 클릭시
        btnDate = (Button) findViewById(R.id.btn_pallet_date);
        btnDate.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                showCalendar();
            }
        });

        palletsCntEdit = findViewById(R.id.edit_pallets_cnt);
        edtRemark = findViewById(R.id.edt_remark);

        init();
    }

    protected void onResume()
    {
        super.onResume();
    }

    protected void onPause()
    {
        super.onPause();
    }

    private void init()
    {
        try
        {
            args = getIntent().getExtras();

            if(args.getString("PAGE_GB").equals("NEW"))
            {
                ((Button)findViewById(R.id.btn_save)).setText("등록");
                saveGb = "INSERT";
            }
            else if(args.getString("PAGE_GB").equals("EDIT"))
            {
                ((Button)findViewById(R.id.btn_save)).setText("수정");
                saveGb = "UPDATE";
            }

            //사업장
            TextView txtDeptNm = (TextView) findViewById(R.id.txt_col1_right);
            if(args.getString("DEPT_CD", "") == null || args.getString("DEPT_CD", "").equals(""))
            {
                showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
                return;
            }
            else
            {
                txtDeptNm.setText(Key.getUserInfo().getString("USER_DEPT_NM"));
                args.putString("DEPT_NM", Key.getUserInfo().getString("USER_DEPT_NM"));
                args.putString("DEPT_CD", Key.getUserInfo().getString("DEPT_CD"));
            }

            //입출일자
            if(args.getString("MOVE_DT") == null || args.getString("MOVE_DT").equals(""))
            {
                btnDate.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
            }
            else
            {
                btnDate.setText(args.getString("MOVE_DT"));
            }

            //납품처
            if(args.getString("OUT_CD", "") == null || args.getString("OUT_CD", "").equals(""))
            {

            }
            else
            {
                ((Button) findViewById(R.id.btn_client)).setText(args.getString("OUT_NM", ""));
                args.putString("OUT_CD", args.getString("OUT_CD"));
            }

            //차량
            TextView txtCarNo = (TextView) findViewById(R.id.txt_col2_right);
            if(args.getString("CAR_NO", "") == null || args.getString("CAR_NO", "").equals(""))
            {
                showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
                return;
            }
            else
            {
                txtCarNo.setText(args.getString("CAR_NO"));
                args.putString("CAR_NO", args.getString("CAR_NO"));
            }

            //팔레트
            if(args.getString("PALLET_CD", "") == null || args.getString("PALLET_CD", "").equals(""))
            {

            }
            else
            {
                ((Button) findViewById(R.id.btn_pallet)).setText(args.getString("PALLET_NM", ""));
                args.putString("PALLET_CD", args.getString("PALLET_CD"));
            }

            //입출형식
            if(args.getString("MOVE_TYPE", "") == null || args.getString("MOVE_TYPE", "").equals(""))
            {
                ((Button) findViewById(R.id.btn_in)).setSelected(true);
                ((Button) findViewById(R.id.btn_in)).setTextColor(Color.parseColor("#FFFFFF"));
                args.putString("MOVE_TYPE", "I");
            }
            else
            {
                ((Button) findViewById(R.id.btn_in)).setSelected(true);
                ((Button) findViewById(R.id.btn_in)).setTextColor(Color.parseColor("#FFFFFF"));
                //args.putString("MOVE_TYPE", args.getString("MOVE_TYPE"));
            }

            //수량
            if(args.getString("MOVE_QTY", "") == null || args.getString("MOVE_QTY", "").equals(""))
            {

            }
            else
            {
                args.putString("MOVE_QTY", args.getString("MOVE_QTY"));
                palletsCntEdit.setText(args.getString("MOVE_QTY", ""));
                palletsCnt = Integer.parseInt(args.getString("MOVE_QTY"));
            }

            //비고
            if(args.getString("REMARK", "") == null || args.getString("REMARK", "").equals(""))
            {

            }
            else
            {
                edtRemark.setText(args.getString("REMARK"));
                args.putString("REMARK", args.getString("REMARK"));
            }

            //팔레트 사진
            if(!args.getString("FILE1_YN", "").equalsIgnoreCase("N"))
            {
                ((ImageView) findViewById(R.id.layout_cert1)).setBackgroundResource(R.drawable.border_textview_gray);
            }
            else
            {
                ((ImageView) findViewById(R.id.layout_cert1)).setBackgroundResource(R.drawable.border_textview);
            }

            if(!args.getString("FILE2_YN", "").equalsIgnoreCase("N"))
            {
                ((ImageView) findViewById(R.id.layout_cert2)).setBackgroundResource(R.drawable.border_textview_gray);
            }
            else
            {
                ((ImageView) findViewById(R.id.layout_cert2)).setBackgroundResource(R.drawable.border_textview);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void showCalendar()
    {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener ()
        {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

                curCal.set(Calendar.YEAR, year);
                curCal.set(Calendar.MONTH, monthOfYear);
                curCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnDate.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
                args.putString("MOVE_DT", Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
            }

        }, curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH), curCal.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    public void onClick(View v)
    {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId())
        {
           case R.id.btn_minus:
                if (palletsCnt - 1 >= 0)
                {
                    palletsCnt--;
                    palletsCntEdit.setText("" + palletsCnt);
                    palletsCntEdit.setSelection(palletsCntEdit.getText().length());
                    args.putString("MOVE_QTY", palletsCntEdit.getText().toString());
                }
                break;

            case R.id.btn_plus:
                palletsCnt++;
                palletsCntEdit.setText("" + palletsCnt);
                palletsCntEdit.setSelection(palletsCntEdit.getText().length());
                args.putString("MOVE_QTY", palletsCntEdit.getText().toString());
                break;

            case R.id.btn_client:
                args.putString("ACTIVITY_GB", "PALLET");
                args.putString("USER_GB", "CAR");
                args.putString("REMARK", edtRemark.getText().toString());
                goPopClient();
                break;

            case R.id.btn_pallet:
                args.putString("ACTIVITY_GB", "PALLET");
                args.putString("REMARK", edtRemark.getText().toString());
                goPopPallet();
                break;

            case R.id.btn_in:
                ((Button) findViewById(R.id.btn_in)).setSelected(true);
                ((Button) findViewById(R.id.btn_in)).setTextColor(Color.parseColor("#FFFFFF"));
                args.putString("MOVE_TYPE", "I");
                break;

            case R.id.layout_cert1:
                goCamera("FILE1");
                break;

            case R.id.layout_cert2:
                goCamera("FILE2");
                break;

            case R.id.btn_save:
                if(saveGb.equals("INSERT"))
                {
                    args.putString("REMARK", edtRemark.getText().toString());
                    savePallet();
                }
                else if(saveGb.equals("UPDATE"))
                {
                    args.putString("REMARK", edtRemark.getText().toString());
                    savePallet();
                }
                break;

            case R.id.btn_cancel:
                finish();
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private synchronized void savePallet()
    {
        if (onReq)
            return;

        try
        {
            if (Key.getUserInfo() == null)
                return;

            if (args.getString("OUT_CD", "").equals("") || args.getString("OUT_CD") == null)
            {
                showToast("납품처를 입력해주십시오.", true);
                return;
            }

            if (args.getString("PALLET_CD", "").equals("") || args.getString("PALLET_CD") == null)
            {
                showToast("팔레트를 입력해주십시오..", true);
                return;
            }

            String palletCnt = palletsCntEdit.getText().toString();
            if (palletCnt.isEmpty() || palletCnt.equals("0") || palletCnt.equals("-")) {
                showToast("팔레트 수를 1개 이상 입력해 주십시요.", true);
                return;
            }

            new AsyncTask<Void, Void, Bundle>()
            {
                protected void onPreExecute()
                {
                    onReq = true;
                    showLoadingDialog(null, false);
                }

                protected Bundle doInBackground(Void... arg0)
                {
                    JSONObject payloadJson = null;
                    try
                    {
                        payloadJson = RestRequestor.buildPayload();
                        payloadJson.put("deptCd", Key.getUserInfo().getString("DEPT_CD"));
                        payloadJson.put("userId", Key.getUserInfo().getString("USER_ID"));
                        payloadJson.put("planNo", args.getString("PLAN_NO", ""));
                        payloadJson.put("moveSeq", args.getString("MOVE_SEQ", ""));
                        payloadJson.put("moveDt", args.getString("MOVE_DT", "").substring(0, 4) + args.getString("MOVE_DT", "").substring(6,8) + args.getString("MOVE_DT", "").substring(10,12));
                        payloadJson.put("outCd", args.getString("OUT_CD", ""));
                        payloadJson.put("carCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("palletCd", args.getString("PALLET_CD", ""));
                        payloadJson.put("itemType", args.getString("ITEM_TYPE", ""));
                        payloadJson.put("moveType", args.getString("MOVE_TYPE", ""));
                        payloadJson.put("moveQty", args.getString("MOVE_QTY", ""));
                        payloadJson.put("remark", args.getString("REMARK", ""));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    return RestRequestor.requestPost(API.URL_PALLET_SAVE, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response)
                {
                    onReq = false;
                    dismissLoadingDialog();

                    try
                    {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200"))
                        {
                            if(saveGb.equals("INSERT"))
                            {
                                showToast("팔레트 등록이 완료되었습니다.", false);
                            }
                            else if(saveGb.equals("UPDATE"))
                            {
                                showToast("팔레트 수정이 완료되었습니다.", false);
                            }
                            setResult(100);
                            finish();
                        }
                        else
                        {
                            showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        showToast(e.getMessage(), false);
                    }
                }
            }.execute();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void goPopClient()
    {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);
        Intent intent = new Intent(PalletInActivity.this, PopClient.class);
        intent.putExtras((Bundle) args.clone());
        startActivityForResult(intent, 0, options.toBundle());
        finish();
    }

    public void goPopPallet()
    {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);
        Intent intent = new Intent(PalletInActivity.this, PopPallet.class);
        intent.putExtras((Bundle) args.clone());
        startActivityForResult(intent, 0, options.toBundle());
        finish();
    }

    public void goCamera(String fileUrl)
    {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);
        args.putString("FILE", fileUrl);
        args.putString("INOUT", "IN");
        Intent intent = new Intent(PalletInActivity.this, PalletCameraActivity.class);
        intent.putExtras((Bundle) args.clone());
        startActivityForResult(intent, 0, options.toBundle());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_IMAGE)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                if (data == null)
                    return;

                Uri selectedFileUri = data.getData();
                String photoPath = ImageFilePath.getPath(this, selectedFileUri);
                // CAUTION "raw:/ 로 시작하는 디렉토리 인식 못함 ex) "raw:/storage/emulated/0/Download/Test_Pic/pic3.jpg"
                Log.e(TAG, "+ onActivityResult(): photoPath: " + photoPath);
                //photoPath = storage/emulated/0/Download/DTMS/DTMS_PAPER.jpg
                //setPreviewPhoto(photoPath);
            }
            else
            {
                //finish();
                showToast("*onActivityResult*", true);
            }
        }
    }

}