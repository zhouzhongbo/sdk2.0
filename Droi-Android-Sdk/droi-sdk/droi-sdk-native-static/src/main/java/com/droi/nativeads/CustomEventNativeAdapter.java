package com.droi.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;

import com.droi.common.DataKeys;
import com.droi.common.event.EventDetails;
import com.droi.common.logging.DroiLog;
import com.droi.nativeads.factories.CustomEventNativeFactory;
import com.droi.network.AdResponse;

import java.util.Map;

final class CustomEventNativeAdapter {
    private CustomEventNativeAdapter() {}

    public static void loadNativeAd(@NonNull final Context context,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final AdResponse adResponse,
            @NonNull final CustomEventNative.CustomEventNativeListener customEventNativeListener) {

        final CustomEventNative customEventNative;
        String customEventNativeClassName = adResponse.getCustomEventClassName();

        DroiLog.d("Attempting to invoke custom event: " + customEventNativeClassName);
        try {
            customEventNative = CustomEventNativeFactory.create(customEventNativeClassName);
        } catch (Exception e) {
            DroiLog.w("Failed to load Custom Event Native class: " + customEventNativeClassName);
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
            return;
        }
        if (adResponse.hasJson()) {
            localExtras.put(DataKeys.JSON_BODY_KEY, adResponse.getJsonBody());
        }

        final EventDetails eventDetails = adResponse.getEventDetails();
        if (eventDetails != null) {
            localExtras.put(DataKeys.EVENT_DETAILS, eventDetails);
        }

        localExtras.put(DataKeys.CLICK_TRACKING_URL_KEY, adResponse.getClickTrackingUrl());

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            customEventNative.loadNativeAd(
                    context,
                    customEventNativeListener,
                    localExtras,
                    adResponse.getServerExtras()
            );
        } catch (Exception e) {
            DroiLog.w("Loading custom event native threw an error.", e);
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
        }
    }
}
