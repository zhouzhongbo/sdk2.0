package com.droi.mobileads.factories;

import com.droi.common.AdReport;
import com.droi.mobileads.CustomEventInterstitialAdapter;
import com.droi.mobileads.DroiInterstitial;

import java.util.Map;

public class CustomEventInterstitialAdapterFactory {
    protected static CustomEventInterstitialAdapterFactory instance = new CustomEventInterstitialAdapterFactory();

    @Deprecated // for testing
    public static void setInstance(CustomEventInterstitialAdapterFactory factory) {
        instance = factory;
    }

    public static CustomEventInterstitialAdapter create(DroiInterstitial mDroiInterstitial, String className, Map<String, String> serverExtras, long broadcastIdentifier, AdReport adReport) {
        return instance.internalCreate(mDroiInterstitial, className, serverExtras, broadcastIdentifier, adReport);
    }

    protected CustomEventInterstitialAdapter internalCreate(DroiInterstitial mDroiInterstitial, String className, Map<String, String> serverExtras, long broadcastIdentifier, AdReport adReport) {
        return new CustomEventInterstitialAdapter(mDroiInterstitial, className, serverExtras, broadcastIdentifier, adReport);
    }
}
