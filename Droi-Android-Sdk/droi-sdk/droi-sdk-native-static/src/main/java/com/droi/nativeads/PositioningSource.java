package com.droi.nativeads;

import android.support.annotation.NonNull;

import com.droi.nativeads.DroiNativeAdPositioning.DroiClientPositioning;

/**
 * Allows asynchronously requesting positioning information.
 */
interface PositioningSource {

    interface PositioningListener {
        void onLoad(@NonNull DroiClientPositioning positioning);

        void onFailed();
    }

    void loadPositions(@NonNull String adUnitId, @NonNull PositioningListener listener);

}
