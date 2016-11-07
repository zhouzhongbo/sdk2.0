package com.droi.common;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.droi.common.logging.DroiLog;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by zhouzhongbo on 2016/11/7.
 */

public class DroiMetadata {


    private static DroiMetadata droiMetadata;
    private static Context mAppContext;

    private static final int os = 1;
    private static String mImei;
    private static String mImsi;
    private static String mMac;
    private static String mMac1;
    private static String mAndroidid;
    private static String mAppid ="";
    private static String mChannel ="";

    private static String mCustomer = "";
    private static String mProject = "";
    private static String mBrand = "";
    private static String mHardware = "";
    private static String mOsversion = "";


    public DroiMetadata(Context context) {
        mImei = initializeImei(context);
        mImsi = initializeImsi(context);
        mMac = getLocalMacAddress(context);
        mMac1 = list2Json(getWIFI(context)).toString();
        mAndroidid = initializeAndroidId(context);
    }


    public String initializeImei(Context paramContext)
    {
        TelephonyManager localTelephonyManager = (TelephonyManager)paramContext.getSystemService(Context.TELEPHONY_SERVICE);
        StringBuffer localStringBuffer = new StringBuffer();
        try
        {
            if (isPermission(paramContext, "android.permission.READ_PHONE_STATE"))
                localStringBuffer.append(localTelephonyManager.getDeviceId());
            while (localStringBuffer.length() < 15)
                localStringBuffer.append("0");
        }
        catch (Exception localException)
        {
            DroiLog.d("Failed to as IMEI");
        }

        return (mImei = localStringBuffer.toString().replace("null", "0000"));
    }

    public String initializeImsi(Context paramContext)
    {

        TelephonyManager localTelephonyManager = (TelephonyManager)paramContext.getSystemService(Context.TELEPHONY_SERVICE);
        StringBuilder localStringBuffer = new StringBuilder();
        try
        {
            if (isPermission(paramContext, "android.permission.READ_PHONE_STATE")){
                localStringBuffer.append(localTelephonyManager.getSubscriberId() == null ? "" : localTelephonyManager.getSubscriberId());
            }
            while (localStringBuffer.length() < 15)
                localStringBuffer.append("0");
        }
        catch (Exception localException)
        {
            localException.printStackTrace();
            DroiLog.w( "Failed Get IMSI");
        }
        return (mImsi = localStringBuffer.toString());
    }

    public static String initializeAndroidId(Context paramContext){
        TelephonyManager localTelephonyManager = (TelephonyManager)paramContext.getSystemService(Context.TELEPHONY_SERVICE);
        String android_id = localTelephonyManager.getDeviceId();
        return android_id;
    }


    public boolean isPermission(Context paramContext, String paramString)
    {
        PackageManager localPackageManager;
        return (localPackageManager = paramContext.getPackageManager()).checkPermission(paramString, paramContext.getPackageName()) == 0;
    }

    public String getLocalMacAddress(Context context) {
        if (isPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE)) {
            try {
                WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifi.getConnectionInfo();
                DroiLog.d(info.getMacAddress());
                return info.getMacAddress();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    public static JSONArray list2Json(List<String[]> list) {
        JSONArray json = new JSONArray();
        try {
            for (int i = 0; i < list.size(); ++i) {
                JSONArray itemJson = new JSONArray();
                for (int j = 0; j < list.get(i).length; ++j) {
                    itemJson.put(list.get(i)[j]);
                }
                json.put(itemJson);
            }
        } catch (Exception e) {
            DroiLog.d(e.toString());
        }
        return json;
    }

    private static final int MAX_WIFI = 5;
    public List<String[]> getWIFI(Context context) {
        List<String[]> wifis = new ArrayList<String[]>();
        try {// wifi location
            if (isPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE)) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (wifiManager.isWifiEnabled()) {
                    List<ScanResult> scans = wifiManager.getScanResults();
                    // Log.w("[d]", scans);
                    Collections.sort(scans, new Comparator<ScanResult>() {
                        @Override
                        public int compare(ScanResult o1, ScanResult o2) {
                            return o2.level - o1.level;
                        }
                    });

                    for (int i = 0; i < scans.size() && i < MAX_WIFI; ++i) {
                        ScanResult res = scans.get(i);
                        String mac = res.BSSID.replace(":", "").toLowerCase(); // 123456789012
                        String[] wifi = new String[2];
                        wifi[0] = mac;
                        wifi[1] = Math.abs(res.level) + "";
                        wifis.add(wifi);
                    }
                }
            }

        } catch (Exception e) {
            DroiLog.d(e.toString());
        }

        return wifis;
    }

    public static void setAppID(String appId){
        mAppid = appId;
    }

    public static String getAppID(){
        return mAppid;
    }

    public static void setChannel(String channel){
        mChannel = channel;
    }

    public static String getChannel(){
        return mChannel;
    }

    public static void setCustomer(String customer){
        mCustomer = customer;
    }

    public static String getCustomer(){
        return mCustomer;
    }

    public static void setBrands(String brands){
        mBrand = brands;
    }

    public static String getBrands(){
        return mBrand;
    }


    public static void setProject(String project){
        mProject = project;
    }

    public static String getProject(){
        return mProject;
    }

    public static void setCpu(String cpu){
        mHardware = cpu;
    }

    public static String getCpu(){
        return mHardware;
    }

    public static void setOsVersion(String osversion){
        mOsversion = osversion;
    }

    public static String getOsVersion(){
        return mOsversion;
    }

}
