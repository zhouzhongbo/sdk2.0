package com.droi.mobileads.factories;

import android.content.Context;

import com.droi.mobileads.AdViewController;
import com.droi.mobileads.DroiView;

public class AdViewControllerFactory {
    protected static AdViewControllerFactory instance = new AdViewControllerFactory();

    @Deprecated // for testing
    public static void setInstance(AdViewControllerFactory factory) {
        instance = factory;
    }

    public static AdViewController create(Context context, DroiView mDroiView) {
        return instance.internalCreate(context, mDroiView);
    }

    protected AdViewController internalCreate(Context context, DroiView mDroiView) {
        return new AdViewController(context, mDroiView);
    }
}
