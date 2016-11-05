package com.droi.nativeads;

import android.support.annotation.NonNull;

/**
 * An object that represents placed ads in a {@link com.droi.nativeads.DroiStreamAdPlacer}
 */
class NativeAdData {
    @NonNull private final String adUnitId;
    @NonNull private final DroiAdRenderer adRenderer;
    @NonNull private final NativeAd adResponse;

    NativeAdData(@NonNull final String adUnitId,
            @NonNull final DroiAdRenderer adRenderer,
            @NonNull final NativeAd adResponse) {
        this.adUnitId = adUnitId;
        this.adRenderer = adRenderer;
        this.adResponse = adResponse;
    }

    @NonNull
    String getAdUnitId() {
        return adUnitId;
    }

    @NonNull
    DroiAdRenderer getAdRenderer() {
        return adRenderer;
    }

    @NonNull
    NativeAd getAd() {
        return adResponse;
    }
}