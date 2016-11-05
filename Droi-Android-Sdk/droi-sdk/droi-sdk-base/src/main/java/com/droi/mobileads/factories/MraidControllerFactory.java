package com.droi.mobileads.factories;

import android.content.Context;
import android.support.annotation.NonNull;

import com.droi.common.AdReport;
import com.droi.common.VisibleForTesting;
import com.droi.mraid.MraidController;
import com.droi.mraid.PlacementType;

public class MraidControllerFactory {
    protected static MraidControllerFactory instance = new MraidControllerFactory();

    @VisibleForTesting
    public static void setInstance(MraidControllerFactory factory) {
        instance = factory;
    }

    public static MraidController create(@NonNull final Context context, 
            @NonNull final AdReport adReport, 
            @NonNull final PlacementType placementType) {
        return instance.internalCreate(context, adReport, placementType);
    }

    protected MraidController internalCreate(@NonNull final Context context, 
            @NonNull final AdReport adReport, 
            @NonNull final PlacementType placementType) {
        return new MraidController(context, adReport, placementType);
    }
}
