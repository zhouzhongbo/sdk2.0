package com.droi.mobileads.factories;

import android.support.annotation.NonNull;

import com.droi.common.AdReport;
import com.droi.mobileads.CustomEventBannerAdapter;
import com.droi.mobileads.DroiView;

import java.util.Map;

public class CustomEventBannerAdapterFactory {
    protected static CustomEventBannerAdapterFactory instance = new CustomEventBannerAdapterFactory();

    @Deprecated // for testing
    public static void setInstance(CustomEventBannerAdapterFactory factory) {
        instance = factory;
    }

    public static CustomEventBannerAdapter create(@NonNull DroiView droiView,
            @NonNull String className,
            @NonNull Map<String, String> serverExtras,
            long broadcastIdentifier,
            @NonNull AdReport adReport) {
        return instance.internalCreate(droiView, className, serverExtras, broadcastIdentifier, adReport);
    }

    protected CustomEventBannerAdapter internalCreate(@NonNull DroiView droiView,
            @NonNull String className,
            @NonNull Map<String, String> serverExtras,
            long broadcastIdentifier,
            @NonNull AdReport adReport) {
        return new CustomEventBannerAdapter(droiView, className, serverExtras, broadcastIdentifier, adReport);
    }
}
