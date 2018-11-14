package com.neognp.ytms.delivery.direct;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.neognp.ytms.R;
import com.neognp.ytms.app.API;
import com.neognp.ytms.app.Key;
import com.neognp.ytms.app.MyApp;
import com.neognp.ytms.http.YTMSRestRequestor;
import com.trevor.library.template.BasicActivity;
import com.trevor.library.util.AppUtil;
import com.trevor.library.util.DisplayUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class DirectDeliveryActivity extends BasicActivity {

    private boolean onReq;

    private Calendar curCal;

    private ArrayList<Bundle> listItems = new ArrayList<Bundle>();
    private ListAdapter listAdapter;

    private View contentView;

    private ViewGroup headerView;

    private View titlebar;
    private TextView titleTxt;
    private TextView btmSlideTitleTxt;

    private Button curDateBtn;
    private EditText centerNameEdit;

    private RecyclerView searchResultList;

    private View bottomSlide;
    private ImageButton slideHandlerBtn;
    private BottomSheetBehavior mBottomSheetBehavior;
    private DirectDeliveryFavoriteFragment favoriteFragment;

    private View bottomCenterInfoView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.direct_delivery_activity);

        curCal = Calendar.getInstance();
        curCal.set(Calendar.HOUR_OF_DAY, 0);
        curCal.set(Calendar.MINUTE, 0);
        curCal.set(Calendar.SECOND, 0);
        curCal.set(Calendar.MILLISECOND, 0);

        contentView = findViewById(R.id.contentView);

        titlebar = findViewById(R.id.titlebar);
        titleTxt = (TextView) findViewById(R.id.titleTxt);
        setTitleBar("직송 조회", R.drawable.selector_button_back, 0, R.drawable.selector_button_refresh);

        btmSlideTitleTxt = (TextView) findViewById(R.id.btmSlideTitleTxt);
        btmSlideTitleTxt.setAlpha(0);

        headerView = (ViewGroup) findViewById(R.id.headerView);

        curDateBtn = findViewById(R.id.curDateBtn);
        curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));

        centerNameEdit = findViewById(R.id.centerNameEdit);

        bottomSlide = (ViewGroup) findViewById(R.id.bottomSlide);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSlide);
        mBottomSheetBehavior.setBottomSheetCallback(slideCallback);

        slideHandlerBtn = (ImageButton) findViewById(R.id.slideHandlerBtn);
        slideHandlerBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        searchResultList = (RecyclerView) findViewById(R.id.searchResultList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        searchResultList.setLayoutManager(layoutManager);
        listAdapter = new ListAdapter();
        searchResultList.setAdapter(listAdapter);

        favoriteFragment = (DirectDeliveryFavoriteFragment) getSupportFragmentManager().findFragmentById(R.id.favoriteFragment);

        bottomCenterInfoView = findViewById(R.id.bottomCenterInfoView);
        ((TextView) findViewById(R.id.callCenterTxt)).setText("운송전략팀 연결");
        ((TextView) findViewById(R.id.callCenterPhoneNoTxt)).setText(getString(R.string.delivery_call_center_phone_no));

        contentView.getViewTreeObserver().addOnGlobalLayoutListener(contentLayoutListener);

        if (MyApp.onTest) {
            centerNameEdit.setText("김포");
            centerNameEdit.setSelection(centerNameEdit.getText().length());
            //findViewById(R.id.searchBtn).performClick();
        }
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    private void init() {
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ViewTreeObserver.OnGlobalLayoutListener contentLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // 1회만 호출되도록 리스너 제거
            contentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            Log.e(TAG, "+ Screen h=" + size.y);
            Log.e(TAG, "+ Statusbar h=" + DisplayUtil.getStatusBarHeight(getWindow()));
            Log.e(TAG, "+ titlebar h=" + titlebar.getHeight());

            // BottomSheet peek 높이 지정
            int peekH = size.y - DisplayUtil.getStatusBarHeight(getWindow()) - titlebar.getHeight() - headerView.getHeight() - bottomCenterInfoView.getHeight();
            mBottomSheetBehavior.setPeekHeight(peekH);

            // BottomSheet 확장 높이 설정:  Statusbar, titlebar, callcenter 높이를 제외한 스크린 높이 + bottomSheet handler 높이(타이틀바 하단과 살짝 겹치게)
            int bottomSlideH = size.y - titlebar.getHeight() - DisplayUtil.getStatusBarHeight(getWindow()) - bottomCenterInfoView.getHeight() + DisplayUtil.DpToPx(18f);
            Log.e(TAG, "+ bottomSlideH=" + bottomSlideH);

            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bottomSlide.getLayoutParams();
            params.height = bottomSlideH;
            bottomSlide.setLayoutParams(params);

            params = (CoordinatorLayout.LayoutParams) bottomSlide.getLayoutParams();
            params.height = bottomSlideH;
            bottomSlide.setLayoutParams(params);

            titlebar.bringToFront();
        }
    };

    private BottomSheetBehavior.BottomSheetCallback slideCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                Log.e(TAG, "+ onStateChanged(): STATE_COLLAPSED");
                findViewById(R.id.slideHandlerArw).startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotation_to_left_180));
            } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                Log.e(TAG, "+ onStateChanged(): STATE_EXPANDED");
                findViewById(R.id.slideHandlerArw).startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotation_to_right_180));
            } else if (newState == BottomSheetBehavior.STATE_SETTLING) {
                Log.e(TAG, "+ onStateChanged(): STATE_SETTLING");
            } else if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                Log.e(TAG, "+ onStateChanged(): STATE_DRAGGING");
            } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                Log.e(TAG, "+ onStateChanged(): STATE_HIDDEN");
            }
        }

        // slideOffset: 0.0 ~ 1.0(슬라이드 다 올라갔을 때)
        @Override
        public void onSlide(View bottomSheet, float slideOffset) {
            //Log.e(TAG, "+ onSlide(): slideOffset=" + slideOffset);
            updateMainContentViewTransparency(slideOffset);
        }
    };

    void updateMainContentViewTransparency(float scrollRatio) {
        if (scrollRatio > 1.0f)
            scrollRatio = 1.0f;

        btmSlideTitleTxt.setAlpha(scrollRatio); // 점점 불투명하게 처리
        titleTxt.setAlpha(Math.abs(scrollRatio - 1.0f)); // 점점 불투명하게 처리
        headerView.setAlpha(Math.abs(scrollRatio - 1.0f));  // header 영역 점점 투명하게 처리
    }

    public void onClick(View v) {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        switch (v.getId()) {
            case R.id.titleLeftBtn0:
                finish();
                break;
            case R.id.titleRightBtn1:
                requestSearchList(true);
                break;
            case R.id.curDateBtn:
                showCalendar();
                break;
            case R.id.searchBtn:
                requestSearchList(true);
                break;
            case R.id.bottomCenterInfoView:
                AppUtil.runCallApp(getString(R.string.delivery_call_center_phone_no), true);
                break;
        }
    }

    private void showCalendar() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.e(TAG, "+ onDateSet(): year-month-day=" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);

                curCal.set(Calendar.YEAR, year);
                curCal.set(Calendar.MONTH, monthOfYear);
                curCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                curDateBtn.setText(Key.SDF_CAL_WEEKDAY.format(curCal.getTime()));
            }
        }, curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH), curCal.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    @SuppressLint ("StaticFieldLeak")
    synchronized void requestSetFavorite(Bundle item, boolean add) {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            if (item == null)
                return;

            new AsyncTask<Void, Void, Bundle>() {
                protected void onPreExecute() {
                    onReq = true;
                    showLoadingDialog(null, true);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("deliveryCd", Key.getUserInfo().getString("CLIENT_CD"));
                        payloadJson.put("centerCd", item.getString("CENTER_CD"));
                        if (add)
                            payloadJson.put("updateGb", "A");
                        else
                            payloadJson.put("updateGb", "R");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return YTMSRestRequestor.requestPost(API.URL_DELIVERY_DIRECT_SET_FAVORITE, false, payloadJson, true, false);
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            requestSearchList(false);
                        } else {
                            dismissLoadingDialog();
                            showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    } catch (Exception e) {
                        dismissLoadingDialog();
                        e.printStackTrace();
                        showToast(e.getMessage(), false);
                    }
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint ("StaticFieldLeak")
    private synchronized void requestSearchList(boolean showProgress) {
        if (onReq)
            return;

        try {
            if (Key.getUserInfo() == null)
                return;

            final String dispatchDt = Key.SDF_PAYLOAD.format(curCal.getTime());

            final String centerNm = centerNameEdit.getText().toString().trim();
            if (centerNm.isEmpty()) {
                showToast("거래처명을 입력해 주십시요.", true);
                return;
            }

            new AsyncTask<Void, Void, Bundle>() {
                Bundle response = null;
                ArrayList<Bundle> searchData = null;
                ArrayList<Bundle> favoriteData = null;

                protected void onPreExecute() {
                    onReq = true;
                    if (showProgress)
                        showLoadingDialog(null, true);
                }

                protected Bundle doInBackground(Void... arg0) {
                    JSONObject payloadJson = null;
                    try {
                        // 직송하차지 조회
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("deliveryCd", Key.getUserInfo().getString("CLIENT_CD"));
                        payloadJson.put("dispatchDt", dispatchDt);
                        payloadJson.put("centerNm", centerNm);
                        response = YTMSRestRequestor.requestPost(API.URL_DELIVERY_DIRECT_LIST, false, payloadJson, true, false);
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        if (result_code.equals("200")) {
                            searchData = resBody.getParcelableArrayList("data");
                        } else {
                            return response;
                        }

                        // 직송즐겨찾기 조회
                        payloadJson = YTMSRestRequestor.buildPayload();
                        payloadJson.put("userCd", Key.getUserInfo().getString("USER_CD"));
                        payloadJson.put("deliveryCd", Key.getUserInfo().getString("CLIENT_CD"));
                        response = YTMSRestRequestor.requestPost(API.URL_DELIVERY_DIRECT_FAVORITE_LIST, false, payloadJson, true, false);
                        resBody = response.getBundle(Key.resBody);
                        result_code = resBody.getString(Key.result_code);
                        if (result_code.equals("200")) {
                            favoriteData = resBody.getParcelableArrayList("data");
                        } else {
                            return response;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return response;
                }

                protected void onPostExecute(Bundle response) {
                    onReq = false;
                    dismissLoadingDialog();

                    try {
                        Bundle resBody = response.getBundle(Key.resBody);
                        String result_code = resBody.getString(Key.result_code);
                        String result_msg = resBody.getString(Key.result_msg);

                        if (result_code.equals("200")) {
                            addSearchListItems(searchData);
                            favoriteFragment.addFavoriteListItems(favoriteData);
                        } else {
                            showToast(result_msg + "(result_code:" + result_msg + ")", true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast(e.getMessage(), false);
                    }

                    //// TEST
                    //{
                    //    searchData = new ArrayList<Bundle>();
                    //    for (int i = 0; i < 20; i++) {
                    //        Bundle item = new Bundle();
                    //        item.putString("CENTER_NM", "CENTER_NM");
                    //        item.putString("FAVORITE_YN", "Y");
                    //        searchData.add(item);
                    //    }
                    //    addSearchListItems(searchData);
                    //
                    //    favoriteData = new ArrayList<Bundle>();
                    //    for (int i = 0; i < 20; i++) {
                    //        Bundle item = new Bundle();
                    //        item.putString("CENTER_NM", "CENTER_NM");
                    //        favoriteData.add(item);
                    //    }
                    //    favoriteFragment.addFavoriteListItems(favoriteData);
                    //}
                }
            }.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized void addSearchListItems(ArrayList<Bundle> items) {
        if (items == null)
            return;

        try {
            listItems.clear();
            listItems.addAll(items);
            listAdapter.notifyDataSetChanged();

            ((TextView) findViewById(R.id.column0Txt)).setText("조회결과 " + items.size() + "건");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        static final int TYPE_LIST_ITEM = 0;

        public int getItemViewType(int position) {
            return listItems.get(position).getInt("viewType", TYPE_LIST_ITEM);
        }

        public int getItemCount() {
            return listItems.size();
        }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder holder = null;

            if (viewType == TYPE_LIST_ITEM) {
                View v = View.inflate(getContext(), R.layout.direct_delivery_search_item, null);
                v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                holder = new ListItemView(v);
            }

            return holder;
        }

        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            ((ItemViewHolder) holder).onBindViewData(listItems.get(position));
        }

        abstract class ItemViewHolder extends RecyclerView.ViewHolder {
            public ItemViewHolder(View itemView) {
                super(itemView);
            }

            public abstract void onBindViewData(final Bundle item);
        }

        class ListItemView extends ItemViewHolder {
            public ListItemView(View itemView) {
                super(itemView);
            }

            public void onBindViewData(final Bundle item) {
                try {
                    ((TextView) itemView.findViewById(R.id.centerNameTxt)).setText(item.getString("CENTER_NM", ""));

                    ImageView starImg = itemView.findViewById(R.id.starImg);
                    String FAVORITE_YN = item.getString("FAVORITE_YN", "N");
                    if (FAVORITE_YN.equalsIgnoreCase("Y"))
                        starImg.setImageResource(R.drawable.star_on);
                    else
                        starImg.setImageResource(R.drawable.star_off);

                    starImg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (item.getString("FAVORITE_YN", "N").equalsIgnoreCase("N"))
                                requestSetFavorite(item, true);
                            else
                                requestSetFavorite(item, false);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}