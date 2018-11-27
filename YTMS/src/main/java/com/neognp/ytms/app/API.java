package com.neognp.ytms.app;

public class API {

    public static final String DEFAULT_IP = "128.134.178.76";
    public static final int DEFAULT_PORT = 8000;

    public static final String URL_ = "";
    public static final String URL_LOGIN = "/common/login";
    public static final String URL_GPS_SEND = "/common/gps";
    public static final String URL_NOTICE_NEW_COUNT = "/common/notice/count";
    public static final String URL_NOTICE = "/common/notice/list";
    public static final String URL_NOTICE_READ = "/common/notice/log";
    public static final String URL_PERSONAL_DAY_LIST = "/common/select/holiday";
    public static final String URL_PERSONAL_DAY_ADD = "/common/insert/holiday";
    public static final String URL_PERSONAL_DAY_DELETE = "/common/delete/holiday";
    public static final String URL_LOCATION = "/common/location";
    public static final String URL_WEB_FORK_LIFT_CHECK = "/view/fork_lift_check.html";

    public static final String URL_SHIPPER_PWD_CHANGE = "/cust/update/pwd";
    public static final String URL_SHIPPER_ALLOC_CNT = "/cust/dispatch/count";
    public static final String URL_SHIPPER_CAR_REQUEST_CNT = "/cust/select/car";
    public static final String URL_SHIPPER_CAR_REQUEST_CNT_SAVE = "/cust/request/car";
    public static final String URL_SHIPPER_CAR_REQUEST_HISTORY = "/cust/list/car";
    public static final String URL_SHIPPER_ALLOC_LIST = "/cust/dispatch/list";
    public static final String URL_SHIPPER_PALLETS_REQUESTED = "/cust/select/pallet";
    public static final String URL_SHIPPER_PALLETS_REQUEST = "/cust/request/pallet";
    public static final String URL_SHIPPER_PALLETS_REQUEST_HISTORY = "/cust/list/pallet";

    public static final String URL_CAR_ACCOUNT_INFO = "/car/select/info";
    public static final String URL_CAR_ACCOUNT_INFO_SAVE = "/car/update/info";
    public static final String URL_CAR_ALLOC_CNT = "/car/dispatch/count";
    public static final String URL_CAR_ALLOC_LIST = "/car/dispatch/list";
    public static final String URL_CAR_PALLETS_INPUT = "/car/input/pallet";
    public static final String URL_CAR_RECEIPT_SAVE = "/car/send/receipt";
    public static final String URL_CAR_FREIGHT_CHARGE_REQUEST = "/car/finish/delivery";
    public static final String URL_CAR_FORK_LIFT_CHECK = "/car/request/arrive";
    public static final String URL_CAR_FORK_LIFT_ORDER = "/car/select/arrive";
    public static final String URL_CAR_RECEIPT_DISPATCH_HISTORY = "/car/receipt/list";
    public static final String URL_CAR_FREIGHT_CHARGE_HISTORY = "/car/payment/list";

    public static final String URL_DELIVERY_ALLOC_CNT = "/dc/dispatch/count";
    public static final String URL_DELIVERY_ALLOC_LIST = "/dc/dispatch/list";
    public static final String URL_DELIVERY_DIRECT_LIST = "/dc/center/list";
    public static final String URL_DELIVERY_DIRECT_FAVORITE_LIST = "/dc/favorite/list";
    public static final String URL_DELIVERY_DIRECT_SET_FAVORITE = "/dc/update/favorite";
    public static final String URL_DELIVERY_DIRECT_ALLOC_LIST = "/dc/dispatch/center";
    public static final String URL_DELIVERY_PALLETS_RECEIPT_HISTORY = "/dc/list/request";
    public static final String URL_DELIVERY_PALLETS_RECEIPT_DELETE = "/dc/delete/request";
    public static final String URL_DELIVERY_PALLETS_DISPATCH = "/dc/send/pallet";
    public static final String URL_DELIVERY_PALLETS_DISPATCH_HISTORY = "/dc/list/send";

    public static final String URL_THIRDPARTY_USER_INFO = "/tpl/select/info";
    public static final String URL_THIRDPARTY_USER_INFO_TPL = "/tpl/select/tpl";
    public static final String URL_THIRDPARTY_USER_INFO_SAVE = "/tpl/update/tpl";
    public static final String URL_THIRDPARTY_ALLOC_CNT = "/tpl/dispatch/count";
    public static final String URL_THIRDPARTY_ALLOC_LIST = "/tpl/dispatch/list";
    public static final String URL_THIRDPARTY_CAR_REQUEST = "/tpl/select/car";
    public static final String URL_THIRDPARTY_CAR_REQUEST_SAVE = "/tpl/request/car";
    public static final String URL_THIRDPARTY_CAR_REQUEST_HISTORY = "/tpl/list/car";
}
