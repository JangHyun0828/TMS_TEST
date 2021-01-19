package com.neognp.dtmsapp_v1.cust;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.neognp.dtmsapp_v1.R;
import com.neognp.dtmsapp_v1.app.API;
import com.neognp.dtmsapp_v1.app.Key;
import com.neognp.dtmsapp_v1.http.RestRequestor;
import com.neognp.dtmsapp_v1.popup.PopClient;
import com.neognp.dtmsapp_v1.popup.PopDept;
import com.neognp.dtmsapp_v1.popup.PopItem;
import com.trevor.library.template.BasicActivity;

import org.json.JSONObject;

import java.util.Calendar;

public class OrderNewActivity extends BasicActivity {

    public static final int CLIENT_REQUEST = 100;

    private boolean onReq;
    private Bundle args;

    private Calendar fromCal1, toCal1, curCal1, fromCal2, toCal2, curCal2;
    private Button btnDate1, btnDate2;

    private int OrderCnt;

    private EditText orderCntEdit, edtRemark;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cust_order_new_activity);

        //Calendar1 설정
        toCal1 = Calendar.getInstance();
        toCal1.set(Calendar.HOUR_OF_DAY, 0);
        toCal1.set(Calendar.MINUTE, 0);
        toCal1.set(Calendar.SECOND, 0);
        toCal1.set(Calendar.MILLISECOND, 0);

        fromCal1 = Calendar.getInstance();
        fromCal1.setTimeInMillis(toCal1.getTimeInMillis());

        curCal1 = Calendar.getInstance();
        curCal1.set(Calendar.HOUR_OF_DAY, 0);
        curCal1.set(Calendar.MINUTE, 0);
        curCal1.set(Calendar.SECOND, 0);
        curCal1.set(Calendar.MILLISECOND, 0);

        //Calendar2 설정
        toCal2 = Calendar.getInstance();
        toCal2.set(Calendar.HOUR_OF_DAY, 0);
        toCal2.set(Calendar.MINUTE, 0);
        toCal2.set(Calendar.SECOND, 0);
        toCal2.set(Calendar.MILLISECOND, 0);

        fromCal2 = Calendar.getInstance();
        fromCal2.setTimeInMillis(toCal2.getTimeInMillis());

        curCal2 = Calendar.getInstance();
        curCal2.set(Calendar.HOUR_OF_DAY, 0);
        curCal2.set(Calendar.MINUTE, 0);
        curCal2.set(Calendar.SECOND, 0);
        curCal2.set(Calendar.MILLISECOND, 0);

        //주문일자 클릭시
        btnDate1 = (Button) findViewById(R.id.btn_order_date);
        btnDate1.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                showCalendar1();
            }
        });

        //출고일자 클릭시
        btnDate2 = (Button) findViewById(R.id.btn_out_date);
        btnDate2.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                showCalendar2();
            }
        });

        orderCntEdit = findViewById(R.id.edit_order_cnt);
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

            if(args.getString("PAGE_GB").equals("ISNERT"))
            {
                ((Button)findViewById(R.id.btn_save)).setText("등록");
            }
            else if(args.getString("PAGE_GB").equals("UPDATE"))
            {
                ((Button)findViewById(R.id.btn_save)).setText("수정");
            }

            //사업장
            if(args.getString("DEPT_CD", "") == null || args.getString("DEPT_CD", "").equals(""))
            {

            }
            else
            {
                ((Button) findViewById(R.id.btn_dept)).setText(args.getString("DEPT_NM", ""));
                args.putString("DEPT_CD", args.getString("DEPT_CD"));
            }

            //주문일자
            Button btnOrderDt = findViewById(R.id.btn_order_date);
            if(args.getString("ORDER_DT", "") == null || args.getString("ORDER_DT", "").equals(""))
            {
                showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
                return;
            }
            else
            {
                btnOrderDt.setText(args.getString("ORDER_DT"));
            }

            //출고일자
            Button btnOutDt = findViewById(R.id.btn_out_date);
            if(args.getString("DELIVERY_DT", "") == null || args.getString("DELIVERY_DT", "").equals(""))
            {
                showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
                return;
            }
            else
            {
                btnOutDt.setText(args.getString("DELIVERY_DT"));
            }

            //거래처
            TextView txtClient = (TextView) findViewById(R.id.txt_col4_right);
            if(args.getString("CUST_CD", "") == null || args.getString("CUST_CD", "").equals(""))
            {
                showToast("네트워크 상태가 좋지않습니다.\n잠시후 다시 이용해주십시오.", false);
                return;
            }
            else
            {
                txtClient.setText(args.getString("CUST_NM"));
                args.putString("CUST_CD", args.getString("CUST_CD"));
            }

            //납품처
            if(args.getString("OUT_CD", "") == null || args.getString("OUT_CD", "").equals(""))
            {

            }
            else
            {
                ((Button) findViewById(R.id.btn_out)).setText(args.getString("OUT_NM", ""));
                args.putString("OUT_CD", args.getString("OUT_CD"));
            }

            //제품
            if(args.getString("ITEM_CD", "") == null || args.getString("ITEM_CD", "").equals(""))
            {

            }
            else
            {
                ((Button) findViewById(R.id.btn_item)).setText(args.getString("ITEM_NM_INFO", ""));
                args.putString("ITEM_CD", args.getString("ITEM_CD"));
                args.putString("ORDER_NO", args.getString("ORDER_NO"));
            }

            //수량
            if(args.getString("ITEM_CNT", "") == null || args.getString("ITEM_CNT", "").equals(""))
            {

            }
            else
            {
                args.putString("ITEM_CNT", args.getString("ITEM_CNT"));
                orderCntEdit.setText(args.getString("ITEM_CNT", ""));
                OrderCnt = Integer.parseInt(args.getString("ITEM_CNT"));
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
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void showCalendar1()
    {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener ()
        {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

                curCal1.set(Calendar.YEAR, year);
                curCal1.set(Calendar.MONTH, monthOfYear);
                curCal1.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnDate1.setText(Key.SDF_CAL_WEEKDAY.format(curCal1.getTime()));
                args.putString("ORDER_DT", Key.SDF_CAL_WEEKDAY.format(curCal1.getTime()));
            }

        }, curCal1.get(Calendar.YEAR), curCal1.get(Calendar.MONTH), curCal1.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showCalendar2()
    {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener ()
        {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

                curCal2.set(Calendar.YEAR, year);
                curCal2.set(Calendar.MONTH, monthOfYear);
                curCal2.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnDate2.setText(Key.SDF_CAL_WEEKDAY.format(curCal2.getTime()));
                args.putString("DELIVERY_DT", Key.SDF_CAL_WEEKDAY.format(curCal2.getTime()));
            }

        }, curCal2.get(Calendar.YEAR), curCal2.get(Calendar.MONTH), curCal2.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    public void onClick(View v)
    {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId())
        {
           //사업장
           case R.id.btn_dept:
                args.putString("REMARK", edtRemark.getText().toString());
                goPopDept();
                break;
            //납품처
            case R.id.btn_out:
                args.putString("ACTIVITY_GB", "ORDER");
                args.putString("REMARK", edtRemark.getText().toString());
                goPopClient();
                break;

            //제품
            case R.id.btn_item:
                args.putString("REMARK", edtRemark.getText().toString());
                goPopItem();
                break;

            case R.id.btn_minus:
                if (OrderCnt - 1 >= 0)
                {
                    OrderCnt--;
                    orderCntEdit.setText("" + OrderCnt);
                    orderCntEdit.setSelection(orderCntEdit.getText().length());
                    args.putString("ITEM_CNT", orderCntEdit.getText().toString());
                }
                break;

            case R.id.btn_plus:
                OrderCnt++;
                orderCntEdit.setText("" + OrderCnt);
                orderCntEdit.setSelection(orderCntEdit.getText().length());
                args.putString("ITEM_CNT", orderCntEdit.getText().toString());
                break;

            case R.id.btn_save:
                saveOrder();
                break;

            case R.id.btn_cancel:
                finish();
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private synchronized void saveOrder()
    {
        if (onReq)
            return;

        try
        {
            if (Key.getUserInfo() == null)
                return;

            String orderCnt = orderCntEdit.getText().toString();
            if (orderCnt.isEmpty() || orderCnt.equals("0") || orderCnt.equals("-")) {
                showToast("수량을 1개 이상 입력해 주십시요.", true);
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
                        payloadJson.put("seq", args.getString("SEQ", ""));
                        payloadJson.put("orderNo", args.getString("ORDER_NO", ""));
                        payloadJson.put("deptCd", args.getString("DEPT_CD", ""));
                        payloadJson.put("orderDt", args.getString("ORDER_DT", "").substring(0, 4) + args.getString("ORDER_DT", "").substring(6,8) + args.getString("ORDER_DT", "").substring(10,12));
                        payloadJson.put("deliveryDt", args.getString("DELIVERY_DT", "").substring(0, 4) + args.getString("DELIVERY_DT", "").substring(6,8) + args.getString("DELIVERY_DT", "").substring(10,12));
                        payloadJson.put("custCd", args.getString("CUST_CD", ""));
                        payloadJson.put("outCd", args.getString("OUT_CD", ""));
                        payloadJson.put("itemCd", args.getString("ITEM_CD", ""));
                        payloadJson.put("itemCnt", args.getString("ITEM_CNT", ""));
                        payloadJson.put("remark", args.getString("REMARK", ""));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    return RestRequestor.requestPost(API.URL_ORDER_SAVE, false, payloadJson, true, false);
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
                            if(args.getString("PAGE_GB").equals("ISNERT"))
                            {
                                showToast("제품 등록이 완료되었습니다.", false);
                            }
                            else if(args.getString("PAGE_GB").equals("UPDATE"))
                            {
                                showToast("제품 수정이 완료되었습니다.", false);
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

    public void goPopDept()
    {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);
        Intent intent = new Intent(OrderNewActivity.this, PopDept.class);
        intent.putExtras((Bundle) args.clone());
        startActivityForResult(intent, 100, options.toBundle());
        finish();
    }

    public void goPopClient()
    {
        if(args.getString("DEPT_CD", "").equals("") || args.getString("DEPT_CD", "") == null)
        {
            showToast("사업장을 먼저 선택하십시오.", true);
            return;
        }

        ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);
        Intent intent = new Intent(OrderNewActivity.this, PopClient.class);
        intent.putExtras((Bundle) args.clone());
        startActivityForResult(intent, 100, options.toBundle());
        finish();
    }

    public void goPopItem()
    {
        if(args.getString("DEPT_CD", "").equals("") || args.getString("DEPT_CD", "") == null)
        {
            showToast("사업장을 먼저 선택하십시오.", true);
            return;
        }

        ActivityOptions options = ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_in_from_right, R.anim.fade_out);
        Intent intent = new Intent(OrderNewActivity.this, PopItem.class);
        intent.putExtras((Bundle) args.clone());
        startActivityForResult(intent, 100, options.toBundle());
        finish();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

}