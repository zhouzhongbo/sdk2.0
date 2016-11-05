package com.droi.mobileads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.droi.common.AdFormat;
import com.droi.common.Preconditions;
import com.droi.common.VisibleForTesting;
import com.droi.common.logging.DroiLog;
import com.droi.mobileads.factories.CustomEventInterstitialAdapterFactory;

import java.util.Map;

import static com.droi.mobileads.DroiErrorCode.ADAPTER_NOT_FOUND;
import static com.droi.mobileads.DroiInterstitial.InterstitialState.IDLE;
import static com.droi.mobileads.DroiInterstitial.InterstitialState.LOADING;
import static com.droi.mobileads.DroiInterstitial.InterstitialState.READY;
import static com.droi.mobileads.DroiInterstitial.InterstitialState.SHOWING;
import static com.droi.mobileads.DroiInterstitial.InterstitialState.DESTROYED;

public class DroiInterstitial implements CustomEventInterstitialAdapter.CustomEventInterstitialAdapterListener {
    @VisibleForTesting
    enum InterstitialState {
        /**
         * Waiting to something to happen. There is no interstitial currently loaded.
         */
        IDLE,

        /**
         * Loading an interstitial.
         */
        LOADING,

        /**
         * Loaded and ready to be shown.
         */
        READY,

        /**
         * The interstitial is showing.
         */
        SHOWING,

        /**
         * No longer able to accept events as the internal InterstitialView has been destroyed.
         */
        DESTROYED
    }

    @NonNull private DroiInterstitialView mInterstitialView;
    @Nullable private CustomEventInterstitialAdapter mCustomEventInterstitialAdapter;
    @Nullable private InterstitialAdListener mInterstitialAdListener;
    @NonNull private Activity mActivity;
    @NonNull private InterstitialState mCurrentInterstitialState;

    public interface InterstitialAdListener {
        void onInterstitialLoaded(DroiInterstitial interstitial);
        void onInterstitialFailed(DroiInterstitial interstitial, DroiErrorCode errorCode);
        void onInterstitialShown(DroiInterstitial interstitial);
        void onInterstitialClicked(DroiInterstitial interstitial);
        void onInterstitialDismissed(DroiInterstitial interstitial);
    }

    public DroiInterstitial(@NonNull final Activity activity, @NonNull final String adUnitId) {
        mActivity = activity;

        mInterstitialView = new DroiInterstitialView(mActivity);
        mInterstitialView.setAdUnitId(adUnitId);

        mCurrentInterstitialState = IDLE;
    }

    private boolean attemptStateTransition(@NonNull final InterstitialState endState) {
        return attemptStateTransition(endState, false);
    }

    /**
     * Attempts to transition to the new state. All state transitions should go through this method.
     * Other methods should not be modifying mCurrentInterstitialState.
     *
     * @param endState     The desired end state.
     * @param forceRefresh Whether or not this is part of a forceRefresh transition. Force
     *                     refresh can happen from IDLE, LOADING, or READY. It will ignore
     *                     the currently loading or loaded ad and attempt to load another.
     * @return {@code true} if a state change happened, {@code false} if no state change happened.
     */
    @VisibleForTesting
    boolean attemptStateTransition(@NonNull final InterstitialState endState,
            boolean forceRefresh) {
        Preconditions.checkNotNull(endState);

        final InterstitialState startState = mCurrentInterstitialState;

        /**
         * There are 50 potential cases. Any combination that is a no op will not be enumerated
         * and returns false. The usual case goes IDLE -> LOADING -> READY -> SHOWING -> IDLE. At
         * most points, having the force refresh flag into IDLE resets DroiInterstitial and clears
         * the interstitial adapter. This cannot happen while an interstitial is showing. Also,
         * DroiInterstitial can be destroyed arbitrarily, and once this is destroyed, it no longer
         * can perform any state transitions.
         */
        switch (startState) {
            case IDLE:
                switch(endState) {
                    case LOADING:
                        // Going from IDLE to LOADING is the usual load case
                        invalidateInterstitialAdapter();
                        mCurrentInterstitialState = LOADING;
                        if (forceRefresh) {
                            // Force-load means a pub-initiated force refresh.
                            mInterstitialView.forceRefresh();
                        } else {
                            // Otherwise, do a normal load
                            mInterstitialView.loadAd();
                        }
                        return true;
                    case SHOWING:
                        DroiLog.d("No interstitial loading or loaded.");
                        return false;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case LOADING:
                switch (endState) {
                    case IDLE:
                        // Being forced back into idle while loading resets DroiInterstitial while
                        // not forced just means the load failed. Either way, it should reset the
                        // state back into IDLE.
                        invalidateInterstitialAdapter();
                        mCurrentInterstitialState = IDLE;
                        return true;
                    case LOADING:
                        if (!forceRefresh) {
                            // Cannot load more than one interstitial at a time
                            DroiLog.d("Already loading an interstitial.");
                        }
                        return false;
                    case READY:
                        // This is the usual load finished transition
                        mCurrentInterstitialState = READY;
                        return true;
                    case SHOWING:
                        DroiLog.d("Interstitial is not ready to be shown yet.");
                        return false;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case READY:
                switch (endState) {
                    case IDLE:
                        if (forceRefresh) {
                            // This happens on a force refresh
                            invalidateInterstitialAdapter();
                            mCurrentInterstitialState = IDLE;
                            return true;
                        }
                        return false;
                    case LOADING:
                        // This is to prevent loading another interstitial while one is loaded.
                        DroiLog.d("Interstitial already loaded. Not loading another.");
                        // Let the ad listener know that there's already an ad loaded
                        if (mInterstitialAdListener != null) {
                            mInterstitialAdListener.onInterstitialLoaded(this);
                        }
                        return false;
                    case SHOWING:
                        // This is the usual transition from ready to showing
                        showCustomEventInterstitial();
                        mCurrentInterstitialState = SHOWING;
                        return true;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case SHOWING:
                switch(endState) {
                    case IDLE:
                        if (forceRefresh) {
                            DroiLog.d("Cannot force refresh while showing an interstitial.");
                            return false;
                        }
                        // This is the usual transition when done showing this interstitial
                        invalidateInterstitialAdapter();
                        mCurrentInterstitialState = IDLE;
                        return true;
                    case LOADING:
                        if (!forceRefresh) {
                            DroiLog.d("Interstitial already showing. Not loading another.");
                        }
                        return false;
                    case SHOWING:
                        DroiLog.d("Already showing an interstitial. Cannot show it again.");
                        return false;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case DESTROYED:
                // Once destroyed, DroiInterstitial is no longer functional.
                DroiLog.d("DroiInterstitial destroyed. Ignoring all requests.");
                return false;
            default:
                return false;
        }
    }

    /**
     * Sets DroiInterstitial to be destroyed. This should only be called by attemptStateTransition.
     */
    private void setInterstitialStateDestroyed() {
        invalidateInterstitialAdapter();
        mInterstitialView.setBannerAdListener(null);
        mInterstitialView.destroy();
        mCurrentInterstitialState = DESTROYED;
    }

    public void load() {
        attemptStateTransition(LOADING);
    }

    public boolean show() {
        return attemptStateTransition(SHOWING);
    }

    public void forceRefresh() {
        attemptStateTransition(IDLE, true);
        attemptStateTransition(LOADING, true);
    }

    public boolean isReady() {
        return mCurrentInterstitialState == READY;
    }

    boolean isDestroyed() {
        return mCurrentInterstitialState == DESTROYED;
    }

    Integer getAdTimeoutDelay() {
        return mInterstitialView.getAdTimeoutDelay();
    }

    @NonNull
    DroiInterstitialView getDroiInterstitialView() {
        return mInterstitialView;
    }

    private void showCustomEventInterstitial() {
        if (mCustomEventInterstitialAdapter != null) {
            mCustomEventInterstitialAdapter.showInterstitial();
        }
    }

    private void invalidateInterstitialAdapter() {
        if (mCustomEventInterstitialAdapter != null) {
            mCustomEventInterstitialAdapter.invalidate();
            mCustomEventInterstitialAdapter = null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setKeywords(@Nullable final String keywords) {
        mInterstitialView.setKeywords(keywords);
    }

    @Nullable
    public String getKeywords() {
        return mInterstitialView.getKeywords();
    }

    @NonNull
    public Activity getActivity() {
        return mActivity;
    }

    @Nullable
    public Location getLocation() {
        return mInterstitialView.getLocation();
    }

    public void destroy() {
        attemptStateTransition(DESTROYED);
    }

    public void setInterstitialAdListener(@Nullable final InterstitialAdListener listener) {
        mInterstitialAdListener = listener;
    }

    @Nullable
    public InterstitialAdListener getInterstitialAdListener() {
        return mInterstitialAdListener;
    }

    public void setTesting(boolean testing) {
        mInterstitialView.setTesting(testing);
    }

    public boolean getTesting() {
        return mInterstitialView.getTesting();
    }

    public void setLocalExtras(Map<String, Object> extras) {
        mInterstitialView.setLocalExtras(extras);
    }

    @NonNull
    public Map<String, Object> getLocalExtras() {
        return mInterstitialView.getLocalExtras();
    }

    /*
     * Implements CustomEventInterstitialAdapter.CustomEventInterstitialListener
     * Note: All callbacks should be no-ops if the interstitial has been destroyed
     */

    @Override
    public void onCustomEventInterstitialLoaded() {
        if (isDestroyed()) {
            return;
        }

        attemptStateTransition(READY);

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialLoaded(this);
        }
    }

    @Override
    public void onCustomEventInterstitialFailed(@NonNull final DroiErrorCode errorCode) {
        if (isDestroyed()) {
            return;
        }

        if (!mInterstitialView.loadFailUrl(errorCode)) {
            attemptStateTransition(IDLE);
        }
    }

    @Override
    public void onCustomEventInterstitialShown() {
        if (isDestroyed()) {
            return;
        }

        mInterstitialView.trackImpression();

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialShown(this);
        }
    }

    @Override
    public void onCustomEventInterstitialClicked() {
        if (isDestroyed()) {
            return;
        }

        mInterstitialView.registerClick();

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialClicked(this);
        }
    }

    @Override
    public void onCustomEventInterstitialDismissed() {
        if (isDestroyed()) {
            return;
        }

        attemptStateTransition(IDLE);

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialDismissed(this);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public class DroiInterstitialView extends DroiView {
        public DroiInterstitialView(Context context) {
            super(context);
            setAutorefreshEnabled(false);
        }

        @Override
        public AdFormat getAdFormat() {
            return AdFormat.INTERSTITIAL;
        }

        @Override
        protected void loadCustomEvent(String customEventClassName, Map<String, String> serverExtras) {
            if (mAdViewController == null) {
                return;
            }

            if (TextUtils.isEmpty(customEventClassName)) {
                DroiLog.d("Couldn't invoke custom event because the server did not specify one.");
                loadFailUrl(ADAPTER_NOT_FOUND);
                return;
            }

            if (mCustomEventInterstitialAdapter != null) {
                mCustomEventInterstitialAdapter.invalidate();
            }

            DroiLog.d("Loading custom event interstitial adapter.");

            mCustomEventInterstitialAdapter = CustomEventInterstitialAdapterFactory.create(
                    DroiInterstitial.this,
                    customEventClassName,
                    serverExtras,
                    mAdViewController.getBroadcastIdentifier(),
                    mAdViewController.getAdReport());
            mCustomEventInterstitialAdapter.setAdapterListener(DroiInterstitial.this);
            mCustomEventInterstitialAdapter.loadInterstitial();
        }

        protected void trackImpression() {
            DroiLog.d("Tracking impression for interstitial.");
            if (mAdViewController != null) mAdViewController.trackImpression();
        }

        @Override
        protected void adFailed(DroiErrorCode errorCode) {
            attemptStateTransition(IDLE);
            if (mInterstitialAdListener != null) {
                mInterstitialAdListener.onInterstitialFailed(DroiInterstitial.this, errorCode);
            }
        }
    }

    @VisibleForTesting
    @Deprecated
    void setInterstitialView(@NonNull DroiInterstitialView interstitialView) {
        mInterstitialView = interstitialView;
    }

    @VisibleForTesting
    @Deprecated
    void setCurrentInterstitialState(@NonNull final InterstitialState interstitialState) {
        mCurrentInterstitialState = interstitialState;
    }

    @VisibleForTesting
    @Deprecated
    @NonNull
    InterstitialState getCurrentInterstitialState() {
        return mCurrentInterstitialState;
    }

    @VisibleForTesting
    @Deprecated
    void setCustomEventInterstitialAdapter(@NonNull final CustomEventInterstitialAdapter
            customEventInterstitialAdapter) {
        mCustomEventInterstitialAdapter = customEventInterstitialAdapter;
    }
}
