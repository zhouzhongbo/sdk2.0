package com.droi.mobileads;

import android.app.Application;
import android.content.Context;

import com.droi.common.DroiMetadata;
import com.droi.common.util.Utils;

/**
 * Created by zhouzhongbo on 2016/11/7.
 */

public class AdControl {

    private static AdControl madControl;
    private static Context appContext;
    public static final String MD_APPID = "DROI_APPID";
    public static final String MD_CHANNEL = "DROI_CHANNEL";

    /**
     * @param context context of application.
     */
    public static AdControl initialize(Application context) throws Exception{
        madControl = getInstance(context);
        appContext = context;
        maniFestDataInit(context);
        return madControl;
    }

    private static AdControl getInstance(Context context) {
        if (madControl == null)
            madControl = new AdControl();
        return madControl;
    }

    private static void maniFestDataInit(Context mcontext) throws Exception {
        String appid = Utils.getKeyValue(mcontext,MD_APPID);
        if(appid == null || appid.equals("")){
            throw new Exception("Can't find DROI_APPID setting in your Manifest.xml!");
        }else{
            DroiMetadata.setAppID(appid);
        }

        String channel = Utils.getKeyValue(mcontext,MD_CHANNEL);
        if(channel == null || channel.equals("")){
            throw new Exception("Can't find DROI_CHANNEL setting in your Manifest.xml!");
        }else{
            DroiMetadata.setChannel(channel);
        }
    }

    public static AdControl setCustomer(String customer) throws Exception {
        if (appContext == null) {
            throw new Exception("Please Call Init method at first!");
        }

        if (madControl == null) {
            madControl = AdControl.getInstance(appContext);
        }
        DroiMetadata.setCustomer(customer);
        return madControl;
    }

    public static AdControl setBrand(String brand) throws Exception {
        if (appContext == null) {
            throw new Exception("Please Call Init method at first!");
        }

        if (madControl == null) {
            madControl = AdControl.getInstance(appContext);
        }
        DroiMetadata.setBrands( brand);
        return madControl;
    }

    public static AdControl setProject(String project) throws Exception {
        if (appContext == null) {
            throw new Exception("Please Call Init method at first!");
        }

        if (madControl == null) {
            madControl = AdControl.getInstance(appContext);
        }
        DroiMetadata.setProject( project);
        return madControl;
    }

    public static AdControl setCpu(String cpu) throws Exception {
        if (appContext == null) {
            throw new Exception("Please Call Init method at first!");
        }

        if (madControl == null) {
            madControl = AdControl.getInstance(appContext);
        }
        DroiMetadata.setCpu( cpu);
        return madControl;
    }

    public static AdControl setOSVersion(String version) throws Exception {
        if (appContext == null) {
            throw new Exception("Please Call Init method at first!");
        }

        if (madControl == null) {
            madControl = AdControl.getInstance(appContext);
        }
        DroiMetadata.setOsVersion( version);
        return madControl;
    }
}
