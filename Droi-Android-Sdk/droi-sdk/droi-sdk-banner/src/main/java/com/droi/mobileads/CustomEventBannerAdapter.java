package com.droi.mobileads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.droi.common.AdReport;
import com.droi.common.Constants;
import com.droi.common.Droi;
import com.droi.common.Preconditions;
import com.droi.common.logging.DroiLog;
import com.droi.mobileads.CustomEventBanner.CustomEventBannerListener;
import com.droi.mobileads.factories.CustomEventBannerFactory;

import java.util.Map;
import java.util.TreeMap;

import static com.droi.common.DataKeys.AD_HEIGHT;
import static com.droi.common.DataKeys.AD_REPORT_KEY;
import static com.droi.common.DataKeys.AD_WIDTH;
import static com.droi.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.droi.mobileads.DroiErrorCode.ADAPTER_NOT_FOUND;
import static com.droi.mobileads.DroiErrorCode.NETWORK_TIMEOUT;
import static com.droi.mobileads.DroiErrorCode.UNSPECIFIED;

public class CustomEventBannerAdapter implements CustomEventBannerListener {
    public static final int DEFAULT_BANNER_TIMEOUT_DELAY = Constants.TEN_SECONDS_MILLIS;
    private boolean mInvalidated;
    private DroiView mdroiView;
    private Context mContext;
    private CustomEventBanner mCustomEventBanner;
    private Map<String, Object> mLocalExtras;
    private Map<String, String> mServerExtras;

    private final Handler mHandler;
    private final Runnable mTimeout;
    private boolean mStoredAutorefresh;

    public CustomEventBannerAdapter(@NonNull DroiView droiView,
            @NonNull String className,
            @NonNull Map<String, String> serverExtras,
            long broadcastIdentifier,
            @Nullable AdReport adReport) {
        Preconditions.checkNotNull(serverExtras);
        mHandler = new Handler();
        mdroiView = droiView;
        mContext = droiView.getContext();
        mTimeout = new Runnable() {
            @Override
            public void run() {
                DroiLog.d("Third-party network timed out.");
                onBannerFailed(NETWORK_TIMEOUT);
                invalidate();
            }
        };

        DroiLog.d("Attempting to invoke custom event: " + className);
        try {
            mCustomEventBanner = CustomEventBannerFactory.create(className);
        } catch (Exception exception) {
            DroiLog.d("Couldn't locate or instantiate custom event: " + className + ".");
            mdroiView.loadFailUrl(ADAPTER_NOT_FOUND);
            return;
        }

        // Attempt to load the JSON extras into mServerExtras.
        mServerExtras = new TreeMap<String, String>(serverExtras);

        mLocalExtras = mdroiView.getLocalExtras();
        if (mdroiView.getLocation() != null) {
            mLocalExtras.put("location", mdroiView.getLocation());
        }
        mLocalExtras.put(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        mLocalExtras.put(AD_REPORT_KEY, adReport);
        mLocalExtras.put(AD_WIDTH, mdroiView.getAdWidth());
        mLocalExtras.put(AD_HEIGHT, mdroiView.getAdHeight());
    }

    void loadAd() {
        if (isInvalidated() || mCustomEventBanner == null) {
            return;
        }

        mHandler.postDelayed(mTimeout, getTimeoutDelayMilliseconds());

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            mCustomEventBanner.loadBanner(mContext, this, mLocalExtras, mServerExtras);
        } catch (Exception e) {
            DroiLog.d("Loading a custom event banner threw an exception.", e);
            onBannerFailed(DroiErrorCode.INTERNAL_ERROR);
        }
    }

    void invalidate() {
        if (mCustomEventBanner != null) {
            // Custom event classes can be developed by any third party and may not be tested.
            // We catch all exceptions here to prevent crashes from untested code.
            try {
                mCustomEventBanner.onInvalidate();
            } catch (Exception e) {
                DroiLog.d("Invalidating a custom event banner threw an exception", e);
            }
        }
        mContext = null;
        mCustomEventBanner = null;
        mLocalExtras = null;
        mServerExtras = null;
        mInvalidated = true;
    }

    boolean isInvalidated() {
        return mInvalidated;
    }

    private void cancelTimeout() {
        mHandler.removeCallbacks(mTimeout);
    }

    private int getTimeoutDelayMilliseconds() {
        if (mdroiView == null
                || mdroiView.getAdTimeoutDelay() == null
                || mdroiView.getAdTimeoutDelay() < 0) {
            return DEFAULT_BANNER_TIMEOUT_DELAY;
        }

        return mdroiView.getAdTimeoutDelay() * 1000;
    }

    /*
     * CustomEventBanner.Listener implementation
     */
    @Override
    public void onBannerLoaded(View bannerView) {
        if (isInvalidated()) {
            return;
        }

        cancelTimeout();

        if (mdroiView != null) {
            mdroiView.nativeAdLoaded();
            mdroiView.setAdContentView(bannerView);
            if (!(bannerView instanceof HtmlBannerWebView)) {
                mdroiView.trackNativeImpression();
            }
        }
    }

    @Override
    public void onBannerFailed(DroiErrorCode errorCode) {
        if (isInvalidated()) {
            return;
        }

        if (mdroiView != null) {
            if (errorCode == null) {
                errorCode = UNSPECIFIED;
            }
            cancelTimeout();
            mdroiView.loadFailUrl(errorCode);
        }
    }

    @Override
    public void onBannerExpanded() {
        if (isInvalidated()) {
            return;
        }

        mStoredAutorefresh = mdroiView.getAutorefreshEnabled();
        mdroiView.setAutorefreshEnabled(false);
        mdroiView.adPresentedOverlay();
    }

    @Override
    public void onBannerCollapsed() {
        if (isInvalidated()) {
            return;
        }

        mdroiView.setAutorefreshEnabled(mStoredAutorefresh);
        mdroiView.adClosed();
    }

    @Override
    public void onBannerClicked() {
        if (isInvalidated()) {
            return;
        }

        if (mdroiView != null) {
            mdroiView.registerClick();
        }
    }

    @Override
    public void onLeaveApplication() {
        onBannerClicked();
    }
}
