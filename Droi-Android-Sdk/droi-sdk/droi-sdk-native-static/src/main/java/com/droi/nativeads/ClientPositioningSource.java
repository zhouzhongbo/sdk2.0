package com.droi.nativeads;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.droi.nativeads.DroiNativeAdPositioning.DroiClientPositioning;

/**
 * Returns a preset client positioning object.
 */
class ClientPositioningSource implements PositioningSource {
    @NonNull private final Handler mHandler = new Handler();
    @NonNull private final DroiClientPositioning mPositioning;

    ClientPositioningSource(@NonNull DroiClientPositioning positioning) {
        mPositioning = DroiNativeAdPositioning.clone(positioning);
    }

    @Override
    public void loadPositions(@NonNull final String adUnitId,
            @NonNull final PositioningListener listener) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onLoad(mPositioning);
            }
        });
    }
}
