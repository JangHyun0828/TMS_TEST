package com.neognp.ytms.carowner.account;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.neognp.ytms.R;
import com.trevor.library.template.BasicFragment;
import com.trevor.library.widget.DropDownSelector;

import java.util.ArrayList;

public class CarInfoFragment extends BasicFragment implements View.OnClickListener {

    private View contentView;

    private DropDownSelector car_typeSelector, car_tonSelector;
    private EditText car_gbEdit, load_typeEdit;

    private CarOwnerAccountActivity host;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        host = (CarOwnerAccountActivity) getActivity();

        contentView = inflater.inflate(R.layout.car_info_fragment, container, false);

        car_typeSelector = contentView.findViewById(R.id.car_typeSelector);

        car_gbEdit = contentView.findViewById(R.id.car_gbEdit);

        car_tonSelector = contentView.findViewById(R.id.car_tonSelector);

        load_typeEdit = contentView.findViewById(R.id.load_typeEdit);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    void init() {
        try {
            initSelector();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSelector() throws Exception {
        if (host.accountInfo == null)
            return;

        // 차종
        if (host.carTypeList != null) {
            String[] entries = new String[host.carTypeList.size()];
            for (int i = 0; i < entries.length; i++) {
                entries[i] = host.carTypeList.get(i).getString("NAME");
            }
            car_typeSelector.setEntries(entries, null, getItemIndexOfCode(host.carTypeList, host.accountInfo.getString("CAR_TYPE")));
        }

        // 차량구분
        car_gbEdit.setText((host.accountInfo.getString("CAR_GB")));
        car_gbEdit.setSelection(car_gbEdit.getText().length());

        // 톤수
        if (host.carTonList != null) {
            String[] entries = new String[host.carTonList.size()];
            for (int i = 0; i < entries.length; i++) {
                entries[i] = host.carTonList.get(i).getString("NAME");
            }
            car_tonSelector.setEntries(entries, null, getItemIndexOfCode(host.carTonList, host.accountInfo.getString("CAR_TON")));
        }

        // 적재함길이
        load_typeEdit.setText((host.accountInfo.getString("LOAD_TYPE")));
        load_typeEdit.setSelection(load_typeEdit.getText().length());
    }

    public int getItemIndexOfCode(ArrayList<Bundle> items, String CODE) {
        if (items == null || items.isEmpty() || CODE == null || CODE.isEmpty())
            return -1;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getString("CODE", "").equals(CODE))
                return i;
        }

        return -1;
    }

    String getCarType() {
        if (host.carTypeList == null)
            return null;

        String value = null;

        try {
            value = host.carTypeList.get(car_typeSelector.getSelectedIndex()).getString("CODE");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    String getCarGb() {
        return car_gbEdit.getText().toString().trim();
    }

    String getCarTon() {
        if (host.carTonList == null)
            return null;

        String value = null;

        try {
            value = host.carTonList.get(car_tonSelector.getSelectedIndex()).getString("CODE");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    String getLoadType() {
        return load_typeEdit.getText().toString().trim();
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

        switch (v.getId()) {
            //case R.id.:
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}