package com.droi.common;

public class Constants {

    private Constants() {}

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String INTENT_SCHEME = "intent";

//    full test url as below:
//    http://49.213.8.162:2010/adverMobile/adverInfo

    public static final String HOST = "49.213.8.162:2010";  //now is in test
    public static final String AD_HANDLER = "/adverMobile/adverInfo"; //last value is "/m/ad";
    public static final String CONVERSION_TRACKING_HANDLER ="/adverMobile/adverInfo";          //"/m/open";
    public static final String POSITIONING_HANDLER =  "/adverMobile/adverInfo";    //"/m/pos";


    public static final int TEN_SECONDS_MILLIS = 10 * 1000;
    public static final int THIRTY_SECONDS_MILLIS = 30 * 1000;

    public static final int TEN_MB = 10 * 1024 * 1024;

    public static final int UNUSED_REQUEST_CODE = 255;  // Acceptable range is [0, 255]

    public static final String NATIVE_VIDEO_ID = "native_video_id";
    public static final String NATIVE_VAST_VIDEO_CONFIG = "native_vast_video_config";
}
