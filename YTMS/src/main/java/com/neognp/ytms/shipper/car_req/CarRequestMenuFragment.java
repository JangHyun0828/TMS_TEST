package com.neognp.ytms.shipper.car_req;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.neognp.ytms.R;
import com.trevor.library.template.BasicFragment;

public class CarRequestMenuFragment extends BasicFragment implements View.OnClickListener {

    private View contentView;

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
        contentView = inflater.inflate(R.layout.car_request_menu_fragment, container, false);

        contentView.findViewById(R.id.carIncreaseBtn).setOnClickListener(this);
        contentView.findViewById(R.id.carCountFixBtn).setOnClickListener(this);

        return contentView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(contentView.getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.carIncreaseBtn:
                ((CarRequestActivity) getActivity()).addFlexibleInputFragment();
                break;
            case R.id.carCountFixBtn:
                ((CarRequestActivity) getActivity()).addFixedInputFragment();
                break;
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}