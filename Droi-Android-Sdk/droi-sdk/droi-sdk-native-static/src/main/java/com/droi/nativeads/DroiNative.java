package com.droi.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.droi.common.AdFormat;
import com.droi.common.Constants;
import com.droi.common.Preconditions;
import com.droi.common.VisibleForTesting;
import com.droi.common.logging.DroiLog;
import com.droi.common.util.DeviceUtils;
import com.droi.common.util.ManifestUtils;
import com.droi.mobileads.DroiErrorCode;
import com.droi.network.AdRequest;
import com.droi.network.AdResponse;
import com.droi.network.DroiNetworkError;
import com.droi.network.Networking;
import com.droi.volley.NetworkResponse;
import com.droi.volley.RequestQueue;
import com.droi.volley.VolleyError;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.TreeMap;

import static com.droi.common.GpsHelper.fetchAdvertisingInfoAsync;
import static com.droi.nativeads.CustomEventNative.CustomEventNativeListener;
import static com.droi.nativeads.NativeErrorCode.CONNECTION_ERROR;
import static com.droi.nativeads.NativeErrorCode.EMPTY_AD_RESPONSE;
import static com.droi.nativeads.NativeErrorCode.INVALID_REQUEST_URL;
import static com.droi.nativeads.NativeErrorCode.INVALID_RESPONSE;
import static com.droi.nativeads.NativeErrorCode.NATIVE_RENDERER_CONFIGURATION_ERROR;
import static com.droi.nativeads.NativeErrorCode.SERVER_ERROR_RESPONSE_CODE;
import static com.droi.nativeads.NativeErrorCode.UNSPECIFIED;

public class DroiNative {

    public interface DroiNativeNetworkListener {
        void onNativeLoad(final NativeAd nativeAd);
        void onNativeFail(final NativeErrorCode errorCode);
    }

    static final DroiNativeNetworkListener EMPTY_NETWORK_LISTENER =
            new DroiNativeNetworkListener() {
        @Override
        public void onNativeLoad(@NonNull final NativeAd nativeAd) {
            // If this listener is invoked, it means that DroiNative instance has been destroyed
            // so destroy any leftover incoming NativeAds
            nativeAd.destroy();
        }
        @Override
        public void onNativeFail(final NativeErrorCode errorCode) {
        }
    };

    // Highly recommended to be an Activity since 3rd party networks need it
    @NonNull private final WeakReference<Context> mContext;
    @NonNull private final String mAdUnitId;
    @NonNull private DroiNativeNetworkListener mDroiNativeNetworkListener;

    // For small sets TreeMap, takes up less memory than HashMap
    @NonNull private Map<String, Object> mLocalExtras = new TreeMap<String, Object>();
    @NonNull private final AdRequest.Listener mVolleyListener;
    @Nullable private AdRequest mNativeRequest;
    @NonNull AdRendererRegistry mAdRendererRegistry;

    public DroiNative(@NonNull final Context context,
                      @NonNull final String adUnitId,
                      @NonNull final DroiNativeNetworkListener mDroiNativeNetworkListener) {
        this(context, adUnitId, new AdRendererRegistry(), mDroiNativeNetworkListener);
    }

    @VisibleForTesting
    public DroiNative(@NonNull final Context context,
                      @NonNull final String adUnitId,
                      @NonNull AdRendererRegistry adRendererRegistry,
                      @NonNull final DroiNativeNetworkListener droiNativeNetworkListener) {
        Preconditions.checkNotNull(context, "context may not be null.");
        Preconditions.checkNotNull(adUnitId, "AdUnitId may not be null.");
        Preconditions.checkNotNull(adRendererRegistry, "AdRendererRegistry may not be null.");
        Preconditions.checkNotNull(mDroiNativeNetworkListener, "DroiNativeNetworkListener may not be null.");

        ManifestUtils.checkNativeActivitiesDeclared(context);

        mContext = new WeakReference<Context>(context);
        mAdUnitId = adUnitId;
        mDroiNativeNetworkListener = droiNativeNetworkListener;
        mAdRendererRegistry = adRendererRegistry;
        mVolleyListener = new AdRequest.Listener() {
            @Override
            public void onSuccess(@NonNull final AdResponse response) {
                onAdLoad(response);
            }

            @Override
            public void onErrorResponse(@NonNull final VolleyError volleyError) {
                onAdError(volleyError);
            }
        };

        // warm up cache for google play services info
        fetchAdvertisingInfoAsync(context, null);
    }

    /**
     * Registers an ad renderer for rendering a specific native ad format.
     * Note that if multiple ad renderers support a specific native ad format, the first
     * one registered will be used.
     */
    public void registerAdRenderer(DroiAdRenderer mDroiAdRenderer) {
        mAdRendererRegistry.registerAdRenderer(mDroiAdRenderer);
    }

    public void destroy() {
        mContext.clear();
        if (mNativeRequest != null) {
            mNativeRequest.cancel();
            mNativeRequest = null;
        }
        mDroiNativeNetworkListener = EMPTY_NETWORK_LISTENER;
    }

    public void setLocalExtras(@Nullable final Map<String, Object> localExtras) {
        if (localExtras == null) {
            mLocalExtras = new TreeMap<String, Object>();
        } else {
            mLocalExtras = new TreeMap<String, Object>(localExtras);
        }
    }

    public void makeRequest() {
        makeRequest((RequestParameters)null);
    }

    public void makeRequest(@Nullable final RequestParameters requestParameters) {
        makeRequest(requestParameters, null);
    }

    public void makeRequest(@Nullable final RequestParameters requestParameters,
            @Nullable Integer sequenceNumber) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        if (!DeviceUtils.isNetworkAvailable(context)) {
            mDroiNativeNetworkListener.onNativeFail(CONNECTION_ERROR);
            return;
        }

        loadNativeAd(requestParameters, sequenceNumber);
    }

    private void loadNativeAd(
            @Nullable final RequestParameters requestParameters,
            @Nullable final Integer sequenceNumber) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        final NativeUrlGenerator generator = new NativeUrlGenerator(context)
                .withAdUnitId(mAdUnitId)
                .withRequest(requestParameters);

        if (sequenceNumber != null) {
            generator.withSequenceNumber(sequenceNumber);
        }

        final String endpointUrl = generator.generateUrlString(Constants.HOST);

        if (endpointUrl != null) {
            DroiLog.d("Loading ad from: " + endpointUrl);
        }

        requestNativeAd(endpointUrl);
    }

    void requestNativeAd(@Nullable final String endpointUrl) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        if (endpointUrl == null) {
            mDroiNativeNetworkListener.onNativeFail(INVALID_REQUEST_URL);
            return;
        }

        mNativeRequest = new AdRequest(endpointUrl, AdFormat.NATIVE, mAdUnitId, context, mVolleyListener);
        RequestQueue requestQueue = Networking.getRequestQueue(context);
        requestQueue.add(mNativeRequest);
    }

    private void onAdLoad(@NonNull final AdResponse response) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }
        final CustomEventNativeListener customEventNativeListener =
                new CustomEventNativeListener() {
                    @Override
                    public void onNativeAdLoaded(@NonNull final BaseNativeAd nativeAd) {
                        final Context context = getContextOrDestroy();
                        if (context == null) {
                            return;
                        }

                        DroiAdRenderer renderer = mAdRendererRegistry.getRendererForAd(nativeAd);
                        if (renderer == null) {
                            onNativeAdFailed(NATIVE_RENDERER_CONFIGURATION_ERROR);
                            return;
                        }

                        mDroiNativeNetworkListener.onNativeLoad(new NativeAd(context,
                                        response.getImpressionTrackingUrl(),
                                        response.getClickTrackingUrl(),
                                        mAdUnitId,
                                        nativeAd,
                                        renderer)
                        );
                    }

                    @Override
                    public void onNativeAdFailed(final NativeErrorCode errorCode) {
                        DroiLog.v(String.format("Native Ad failed to load with error: %s.", errorCode));
                        requestNativeAd(response.getFailoverUrl());
                    }
                };

        CustomEventNativeAdapter.loadNativeAd(
                context,
                mLocalExtras,
                response,
                customEventNativeListener
        );
    }

    @VisibleForTesting
    void onAdError(@NonNull final VolleyError volleyError) {
        DroiLog.d("Native ad request failed.", volleyError);
        if (volleyError instanceof DroiNetworkError) {
            DroiNetworkError error = (DroiNetworkError) volleyError;
            switch (error.getReason()) {
                case BAD_BODY:
                    mDroiNativeNetworkListener.onNativeFail(INVALID_RESPONSE);
                    return;
                case BAD_HEADER_DATA:
                    mDroiNativeNetworkListener.onNativeFail(INVALID_RESPONSE);
                    return;
                case WARMING_UP:
                    // Used for the sample app to signal a toast.
                    // This is not customer-facing except in the sample app.
                    DroiLog.c(DroiErrorCode.WARMUP.toString());
                    mDroiNativeNetworkListener.onNativeFail(EMPTY_AD_RESPONSE);
                    return;
                case NO_FILL:
                    mDroiNativeNetworkListener.onNativeFail(EMPTY_AD_RESPONSE);
                    return;
                case UNSPECIFIED:
                default:
                    mDroiNativeNetworkListener.onNativeFail(UNSPECIFIED);
                    return;
            }
        } else {
            // Process our other status code errors.
            NetworkResponse response = volleyError.networkResponse;
            if (response != null && response.statusCode >= 500 && response.statusCode < 600) {
                mDroiNativeNetworkListener.onNativeFail(SERVER_ERROR_RESPONSE_CODE);
            } else if (response == null && !DeviceUtils.isNetworkAvailable(mContext.get())) {
                DroiLog.c(String.valueOf(DroiErrorCode.NO_CONNECTION.toString()));
                mDroiNativeNetworkListener.onNativeFail(CONNECTION_ERROR);
            } else {
                mDroiNativeNetworkListener.onNativeFail(UNSPECIFIED);
            }
        }
    }

    @VisibleForTesting
    @Nullable
    Context getContextOrDestroy() {
        final Context context = mContext.get();
        if (context == null) {
            destroy();
            DroiLog.d("Weak reference to Context in DroiNative became null. This instance" +
                    " of DroiNative is destroyed and No more requests will be processed.");
        }
        return context;
    }

    @VisibleForTesting
    @Deprecated
    @NonNull
    DroiNativeNetworkListener getDroiNativeNetworkListener() {
        return mDroiNativeNetworkListener;
    }
}
