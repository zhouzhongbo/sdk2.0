package com.droi.mobileads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.droi.common.AdReport;
import com.droi.common.Constants;
import com.droi.common.Preconditions;
import com.droi.common.logging.DroiLog;
import com.droi.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import com.droi.mobileads.factories.CustomEventInterstitialFactory;

import java.util.Map;
import java.util.TreeMap;

import static com.droi.common.DataKeys.AD_REPORT_KEY;
import static com.droi.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.droi.mobileads.DroiErrorCode.ADAPTER_NOT_FOUND;
import static com.droi.mobileads.DroiErrorCode.NETWORK_TIMEOUT;
import static com.droi.mobileads.DroiErrorCode.UNSPECIFIED;

public class CustomEventInterstitialAdapter implements CustomEventInterstitialListener {
    public static final int DEFAULT_INTERSTITIAL_TIMEOUT_DELAY = Constants.THIRTY_SECONDS_MILLIS;

    private final DroiInterstitial mDroiInterstitial;
    private boolean mInvalidated;
    private CustomEventInterstitialAdapterListener mCustomEventInterstitialAdapterListener;
    private CustomEventInterstitial mCustomEventInterstitial;
    private Context mContext;
    private Map<String, Object> mLocalExtras;
    private Map<String, String> mServerExtras;
    private final Handler mHandler;
    private final Runnable mTimeout;

    public CustomEventInterstitialAdapter(@NonNull final DroiInterstitial droiInterstitial,
            @NonNull final String className,
            @NonNull final Map<String, String> serverExtras,
            long broadcastIdentifier,
            @Nullable AdReport adReport) {
        Preconditions.checkNotNull(serverExtras);
        mHandler = new Handler();
        mDroiInterstitial = droiInterstitial;
        mContext = mDroiInterstitial.getActivity();
        mTimeout = new Runnable() {
            @Override
            public void run() {
                DroiLog.d("Third-party network timed out.");
                onInterstitialFailed(NETWORK_TIMEOUT);
                invalidate();
            }
        };

        DroiLog.d("Attempting to invoke custom event: " + className);
        try {
            mCustomEventInterstitial = CustomEventInterstitialFactory.create(className);
        } catch (Exception exception) {
            DroiLog.d("Couldn't locate or instantiate custom event: " + className + ".");
            mDroiInterstitial.onCustomEventInterstitialFailed(ADAPTER_NOT_FOUND);
            return;
        }

        mServerExtras = new TreeMap<String, String>(serverExtras);
        mLocalExtras = mDroiInterstitial.getLocalExtras();
        if (mDroiInterstitial.getLocation() != null) {
            mLocalExtras.put("location", mDroiInterstitial.getLocation());
        }
        mLocalExtras.put(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        mLocalExtras.put(AD_REPORT_KEY, adReport);
    }

    void loadInterstitial() {
        if (isInvalidated() || mCustomEventInterstitial == null) {
            return;
        }

        mHandler.postDelayed(mTimeout, getTimeoutDelayMilliseconds());

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            mCustomEventInterstitial.loadInterstitial(mContext, this, mLocalExtras, mServerExtras);
        } catch (Exception e) {
            DroiLog.d("Loading a custom event interstitial threw an exception.", e);
            onInterstitialFailed(DroiErrorCode.INTERNAL_ERROR);
        }
    }

    void showInterstitial() {
        if (isInvalidated() || mCustomEventInterstitial == null) {
            return;
        }

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            mCustomEventInterstitial.showInterstitial();
        } catch (Exception e) {
            DroiLog.d("Showing a custom event interstitial threw an exception.", e);
            onInterstitialFailed(DroiErrorCode.INTERNAL_ERROR);
        }
    }

    void invalidate() {
        if (mCustomEventInterstitial != null) {

            // Custom event classes can be developed by any third party and may not be tested.
            // We catch all exceptions here to prevent crashes from untested code.
            try {
                mCustomEventInterstitial.onInvalidate();
            } catch (Exception e) {
                DroiLog.d("Invalidating a custom event interstitial threw an exception.", e);
            }
        }
        mCustomEventInterstitial = null;
        mContext = null;
        mServerExtras = null;
        mLocalExtras = null;
        mCustomEventInterstitialAdapterListener = null;
        mInvalidated = true;
    }

    boolean isInvalidated() {
        return mInvalidated;
    }

    void setAdapterListener(CustomEventInterstitialAdapterListener listener) {
        mCustomEventInterstitialAdapterListener = listener;
    }

    private void cancelTimeout() {
        mHandler.removeCallbacks(mTimeout);
    }

    private int getTimeoutDelayMilliseconds() {
        if (mDroiInterstitial == null
                || mDroiInterstitial.getAdTimeoutDelay() == null
                || mDroiInterstitial.getAdTimeoutDelay() < 0) {
            return DEFAULT_INTERSTITIAL_TIMEOUT_DELAY;
        }

        return mDroiInterstitial.getAdTimeoutDelay() * 1000;
    }

    interface CustomEventInterstitialAdapterListener {
        void onCustomEventInterstitialLoaded();
        void onCustomEventInterstitialFailed(DroiErrorCode errorCode);
        void onCustomEventInterstitialShown();
        void onCustomEventInterstitialClicked();
        void onCustomEventInterstitialDismissed();
    }

    /*
     * CustomEventInterstitial.Listener implementation
     */
    @Override
    public void onInterstitialLoaded() {
        if (isInvalidated()) {
            return;
        }

        cancelTimeout();

        if (mCustomEventInterstitialAdapterListener != null) {
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialLoaded();
        }
    }

    @Override
    public void onInterstitialFailed(DroiErrorCode errorCode) {
        if (isInvalidated()) {
            return;
        }

        if (mCustomEventInterstitialAdapterListener != null) {
            if (errorCode == null) {
                errorCode = UNSPECIFIED;
            }
            cancelTimeout();
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialFailed(errorCode);
        }
    }

    @Override
    public void onInterstitialShown() {
        if (isInvalidated()) {
            return;
        }

        if (mCustomEventInterstitialAdapterListener != null) {
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialShown();
        }
    }

    @Override
    public void onInterstitialClicked() {
        if (isInvalidated()) {
            return;
        }

        if (mCustomEventInterstitialAdapterListener != null) {
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialClicked();
        }
    }

    @Override
    public void onLeaveApplication() {
        onInterstitialClicked();
    }

    @Override
    public void onInterstitialDismissed() {
        if (isInvalidated()) {
            return;
        }

        if (mCustomEventInterstitialAdapterListener != null) {
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialDismissed();
        }
    }

    @Deprecated
    void setCustomEventInterstitial(CustomEventInterstitial interstitial) {
        mCustomEventInterstitial = interstitial;
    }
}
