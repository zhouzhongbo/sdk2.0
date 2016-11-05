package com.droi.mobileads.factories;

import android.content.Context;

import com.droi.common.VisibleForTesting;
import com.droi.mobileads.DroiView;

public class DroiViewFactory {
    protected static DroiViewFactory instance = new DroiViewFactory();

    @VisibleForTesting
    @Deprecated
    public static void setInstance(DroiViewFactory factory) {
        instance = factory;
    }

    public static DroiView create(Context context) {
        return instance.internalCreate(context);
    }

    protected DroiView internalCreate(Context context) {
        return new DroiView(context);
    }
}
