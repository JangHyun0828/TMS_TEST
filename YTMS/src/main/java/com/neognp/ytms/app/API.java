package com.neognp.ytms.app;

public class API {

    public static final String DEFAULT_IP = "128.134.178.76";
    public static final int DEFAULT_PORT = 8000;

    public static final String URL_ = "";
    public static final String URL_LOGIN = "/common/login";
    public static final String URL_NOTICE_NEW_COUNT = "/common/notice/count";
    public static final String URL_NOTICE = "/common/notice/list";
    public static final String URL_NOTICE_READ = "/common/notice/log";
    public static final String URL_LOCATION = "/common/location";

    public static final String URL_SHIPPER_ALLOC_CNT = "/cust/dispatch/count";
    public static final String URL_SHIPPER_CAR_REQUEST_CNT = "/cust/select/car";
    public static final String URL_SHIPPER_CAR_REQUEST_CNT_SAVE = "/cust/request/car";
    public static final String URL_SHIPPER_CAR_REQUEST_HISTORY = "/cust/list/car";
    public static final String URL_SHIPPER_ALLOC_LIST = "/cust/dispatch/list";
    public static final String URL_SHIPPER_PALLETS_REQUESTED = "/cust/select/pallet";
    public static final String URL_SHIPPER_PALLETS_REQUEST = "/cust/request/pallet";
    public static final String URL_SHIPPER_PALLETS_REQUEST_HISTORY = "/cust/list/pallet";

    public static final String URL_CAR_USER_INFO = "/car/select/info";
    public static final String URL_CAR_ALLOC_CNT = "/car/dispatch/count";
    public static final String URL_CAR_ALLOC_LIST = "/car/dispatch/list";
    public static final String URL_CAR_PALLETS_INPUT = "/car/input/pallet";
    public static final String URL_CAR_RECEIPT_SAVE = "/car/send/receipt";

    public static final String URL_DELIVERY_ALLOC_CNT = "/dc/dispatch/count";
    public static final String URL_DELIVERY_ALLOC_LIST = "/dc/dispatch/list";
    public static final String URL_DELIVERY_DIRECT_LIST = "/dc/center/list";
    public static final String URL_DELIVERY_DIRECT_FAVORITE_LIST = "/dc/favorite/list";
    public static final String URL_DELIVERY_DIRECT_SET_FAVORITE = "/dc/update/favorite";
    public static final String URL_DELIVERY_DIRECT_ALLOC_LIST = "/dc/dispatch/center";

}
