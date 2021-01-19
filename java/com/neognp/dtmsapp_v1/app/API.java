package com.neognp.dtmsapp_v1.app;

public class API {

    public static final String DEFAULT_IP = "61.35.212.25";
    public static final int DEFAULT_PORT =  80;

    //local - Hyun
//    public static final String DEFAULT_IP = "192.168.70.208";
//    public static final int DEFAULT_PORT =  80;

    /* 공통 */
    public static final String URL_ = "";
    public static final String URL_LOGIN = "/common/login";
    public static final String URL_GPS_SEND = "/common/send/gps";
    public static final String URL_PUSH_REG= "/common/register";
    public static final String URL_MAP_VIEW= "/common/register";
    public static final String URL_POP_CLIENT= "/popup/select/client";
    public static final String URL_POP_DEPT= "/popup/select/dept";
    public static final String URL_POP_PALLET= "/popup/select/pallet";
    public static final String URL_POP_ITEM= "/popup/select/item";

    /* 차주*/
    public static final String URL_CHECK_DISPATCH = "/car/check/dispatch";
    public static final String URL_REQUEST_DISPATCH = "/car/request/dispatch";
    public static final String URL_CAR_DISPATCH = "/car/list/dispatch";
    public static final String URL_LOAD_LIST = "/car/list/load";
    public static final String URL_NOTICE_LIST = "/car/list/notice";
    public static final String URL_CHANGE_PWD = "/car/change/pw";
    public static final String URL_IN_PALLET_LIST= "/car/list/in/pallet";
    public static final String URL_PALLET_SAVE= "/car/save/pallet";
    public static final String URL_CAR_RECORD = "/car/list/record";
    public static final String URL_CAR_RECORD_DETAIL = "/car/list/record/detail";
    public static final String URL_CAR_LOCATION = "/car/list/location";
    public static final String URL_CAR_RECEIPT_SAVE = "/car/upload/receipt";
    public static final String URL_CAR_UPLOAD_IN_PALLET = "/car/upload/in/pallet";
    public static final String URL_CAR_UPLOAD_OUT_PALLET = "/car/upload/out/pallet";

    /* 관리자 */
    public static final String URL_ADMIN_DISPATCH = "/admin/list/dispatch";
    public static final String URL_ADMIN_LOCATION = "/admin/list/location";

    /* 고객 */
    public static final String URL_CUST_DISPATCH = "/cust/list/dispatch";
    public static final String URL_CUST_LOCATION = "/cust/list/location";
    public static final String URL_ORDER_LIST= "/cust/list/order";
    public static final String URL_ORDER_SAVE = "/cust/save/order";
}
