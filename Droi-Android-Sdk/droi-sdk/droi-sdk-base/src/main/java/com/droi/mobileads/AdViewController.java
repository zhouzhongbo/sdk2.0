package com.droi.mobileads;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.droi.common.AdReport;
import com.droi.common.ClientMetadata;
import com.droi.common.Constants;
import com.droi.common.Droi;
import com.droi.common.Preconditions;
import com.droi.common.VisibleForTesting;
import com.droi.common.event.BaseEvent;
import com.droi.common.logging.DroiLog;
import com.droi.common.util.DeviceUtils;
import com.droi.common.util.Dips;
import com.droi.common.util.Utils;
import com.droi.mraid.MraidNativeCommandHandler;
import com.droi.network.AdRequest;
import com.droi.network.AdResponse;
import com.droi.network.DroiNetworkError;
import com.droi.network.Networking;
import com.droi.network.TrackingRequest;
import com.droi.volley.NetworkResponse;
import com.droi.volley.RequestQueue;
import com.droi.volley.VolleyError;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;

public class AdViewController {
    static final int DEFAULT_REFRESH_TIME_MILLISECONDS = 60000;  // 1 minute
    static final int MAX_REFRESH_TIME_MILLISECONDS = 600000; // 10 minutes
    static final double BACKOFF_FACTOR = 1.5;
    private static final FrameLayout.LayoutParams WRAP_AND_CENTER_LAYOUT_PARAMS =
            new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
    private final static WeakHashMap<View,Boolean> sViewShouldHonorServerDimensions = new WeakHashMap<View, Boolean>();

    private final long mBroadcastIdentifier;

    @Nullable private Context mContext;
    @Nullable private DroiView mDroiView;
    @Nullable private WebViewAdUrlGenerator mUrlGenerator;

    @Nullable private AdResponse mAdResponse;
    private final Runnable mRefreshRunnable;
    @NonNull private final AdRequest.Listener mAdListener;

    private boolean mIsDestroyed;
    private Handler mHandler;
    private boolean mIsLoading;
    private String mUrl;

    // This is the power of the exponential term in the exponential backoff calculation.
    @VisibleForTesting
    int mBackoffPower = 1;

    private Map<String, Object> mLocalExtras = new HashMap<String, Object>();
    private boolean mAutoRefreshEnabled = true;
    private boolean mPreviousAutoRefreshSetting = true;
    private String mKeywords;
    private Location mLocation;
    private boolean mIsTesting;
    private boolean mAdWasLoaded;
    @Nullable private String mAdUnitId;
    private int mTimeoutMilliseconds;
    @Nullable private AdRequest mActiveRequest;
    @Nullable private Integer mRefreshTimeMillis;

    public static void setShouldHonorServerDimensions(View view) {
        sViewShouldHonorServerDimensions.put(view, true);
    }

    private static boolean getShouldHonorServerDimensions(View view) {
        return sViewShouldHonorServerDimensions.get(view) != null;
    }

    public AdViewController(@NonNull Context context, @NonNull DroiView view) {
        mContext = context;
        mDroiView = view;

        // Timeout value of less than 0 means use the ad format's default timeout
        mTimeoutMilliseconds = -1;
        mBroadcastIdentifier = Utils.generateUniqueId();

        mUrlGenerator = new WebViewAdUrlGenerator(mContext.getApplicationContext(),
                MraidNativeCommandHandler.isStorePictureSupported(mContext));

        mAdListener = new AdRequest.Listener() {
            @Override
            public void onSuccess(final AdResponse response) {
                onAdLoadSuccess(response);
            }

            @Override
            public void onErrorResponse(final VolleyError volleyError) {
                onAdLoadError(volleyError);
            }
        };

        mRefreshRunnable = new Runnable() {
            public void run() {
                internalLoadAd();
            }
        };
        mRefreshTimeMillis = DEFAULT_REFRESH_TIME_MILLISECONDS;
        mHandler = new Handler();
    }

    @VisibleForTesting
    void onAdLoadSuccess(@NonNull final AdResponse adResponse) {
        mBackoffPower = 1;
        mAdResponse = adResponse;
        // Do other ad loading setup. See AdFetcher & AdLoadTask.
        mTimeoutMilliseconds = mAdResponse.getAdTimeoutMillis() == null
                ? mTimeoutMilliseconds
                : mAdResponse.getAdTimeoutMillis();
        mRefreshTimeMillis = mAdResponse.getRefreshTimeMillis();
        setNotLoading();

        loadCustomEvent(mDroiView, adResponse.getCustomEventClassName(),
                adResponse.getServerExtras());

        scheduleRefreshTimerIfEnabled();
    }

    @VisibleForTesting
    void onAdLoadError(final VolleyError error) {
        if (error instanceof DroiNetworkError) {
            // If provided, the DroiNetworkError's refresh time takes precedence over the
            // previously set refresh time.
            // The only types of NetworkErrors that can possibly modify
            // an ad's refresh time are CLEAR requests. For CLEAR requests that (erroneously) omit a
            // refresh time header and for all other non-CLEAR types of NetworkErrors, we simply
            // maintain the previous refresh time value.
            final DroiNetworkError droiNetworkError = (DroiNetworkError) error;
            if (droiNetworkError.getRefreshTimeMillis() != null) {
                mRefreshTimeMillis = droiNetworkError.getRefreshTimeMillis();
            }
        }

        final DroiErrorCode errorCode = getErrorCodeFromVolleyError(error, mContext);
        if (errorCode == DroiErrorCode.SERVER_ERROR) {
            mBackoffPower++;
        }

        setNotLoading();
        adDidFail(errorCode);
    }

    @VisibleForTesting
    void loadCustomEvent(@Nullable final DroiView mDroiView,
            @Nullable final String customEventClassName,
            @NonNull final Map<String, String> serverExtras) {
        Preconditions.checkNotNull(serverExtras);

        if (mDroiView == null) {
            DroiLog.d("Can't load an ad in this ad view because it was destroyed.");
            return;
        }

        mDroiView.loadCustomEvent(customEventClassName, serverExtras);
    }

    @VisibleForTesting
    @NonNull
    static DroiErrorCode getErrorCodeFromVolleyError(@NonNull final VolleyError error,
            @Nullable final Context context) {
        final NetworkResponse networkResponse = error.networkResponse;

        // For DroiNetworkErrors, networkResponse is null.
        if (error instanceof DroiNetworkError) {
            switch (((DroiNetworkError) error).getReason()) {
                case WARMING_UP:
                    return DroiErrorCode.WARMUP;
                case NO_FILL:
                    return DroiErrorCode.NO_FILL;
                default:
                    return DroiErrorCode.UNSPECIFIED;
            }
        }

        if (networkResponse == null) {
            if (!DeviceUtils.isNetworkAvailable(context)) {
                return DroiErrorCode.NO_CONNECTION;
            }
            return DroiErrorCode.UNSPECIFIED;
        }

        if (error.networkResponse.statusCode >= 400) {
            return DroiErrorCode.SERVER_ERROR;
        }

        return DroiErrorCode.UNSPECIFIED;
    }

    @Nullable
    public DroiView getDroiView() {
        return mDroiView;
    }

    public void loadAd() {
        mBackoffPower = 1;
        internalLoadAd();
    }

    private void internalLoadAd() {
        mAdWasLoaded = true;
        if (TextUtils.isEmpty(mAdUnitId)) {
            DroiLog.d("Can't load an ad in this ad view because the ad unit ID is not set. " +
                    "Did you forget to call setAdUnitId()?");
            return;
        }

        if (!isNetworkAvailable()) {
            DroiLog.d("Can't load an ad because there is no network connectivity.");
            scheduleRefreshTimerIfEnabled();
            return;
        }

        String adUrl = generateAdUrl();
        loadNonJavascript(adUrl);
    }

    void loadNonJavascript(String url) {
        if (url == null) return;

        DroiLog.d("Loading url: " + url);
        if (mIsLoading) {
            if (!TextUtils.isEmpty(mAdUnitId)) {  // This shouldn't be able to happen?
                DroiLog.i("Already loading an ad for " + mAdUnitId + ", wait to finish.");
            }
            return;
        }

        mUrl = url;
        mIsLoading = true;

        fetchAd(mUrl);
    }

    public void reload() {
        DroiLog.d("Reload ad: " + mUrl);
        loadNonJavascript(mUrl);
    }

    /**
     * Returns true if continuing to load the failover url, false if the ad actually did not fill.
     */
    boolean loadFailUrl(DroiErrorCode errorCode) {
        mIsLoading = false;

        Log.v("Droi", "DroiErrorCode: " + (errorCode == null ? "" : errorCode.toString()));

        final String failUrl = mAdResponse == null ? "" : mAdResponse.getFailoverUrl();
        if (!TextUtils.isEmpty(failUrl)) {
            DroiLog.d("Loading failover url: " + failUrl);
            loadNonJavascript(failUrl);
            return true;
        } else {
            // No other URLs to try, so signal a failure.
            adDidFail(DroiErrorCode.NO_FILL);
            return false;
        }
    }

    void setNotLoading() {
        this.mIsLoading = false;
        if (mActiveRequest != null) {
            if (!mActiveRequest.isCanceled()) {
                mActiveRequest.cancel();
            }
            mActiveRequest = null;
        }
    }

    public String getKeywords() {
        return mKeywords;
    }

    public void setKeywords(String keywords) {
        mKeywords = keywords;
    }

    public Location getLocation() {
        return mLocation;
    }

    public void setLocation(Location location) {
        mLocation = location;
    }

    public String getAdUnitId() {
        return mAdUnitId;
    }

    public void setAdUnitId(@NonNull String adUnitId) {
        mAdUnitId = adUnitId;
    }

    public long getBroadcastIdentifier() {
        return mBroadcastIdentifier;
    }

    public int getAdWidth() {
        if (mAdResponse != null && mAdResponse.getWidth() != null) {
            return mAdResponse.getWidth();
        }

        return 0;
    }

    public int getAdHeight() {
        if (mAdResponse != null && mAdResponse.getHeight() != null) {
            return mAdResponse.getHeight();
        }

        return 0;
    }

    public boolean getAutorefreshEnabled() {
        return mAutoRefreshEnabled;
    }

    void pauseRefresh() {
        mPreviousAutoRefreshSetting = mAutoRefreshEnabled;
        setAutorefreshEnabled(false);
    }

    void unpauseRefresh() {
        setAutorefreshEnabled(mPreviousAutoRefreshSetting);
    }

    void forceSetAutorefreshEnabled(boolean enabled) {
        mPreviousAutoRefreshSetting = enabled;
        setAutorefreshEnabled(enabled);
    }

    private void setAutorefreshEnabled(boolean enabled) {
        final boolean autorefreshChanged = mAdWasLoaded && (mAutoRefreshEnabled != enabled);
        if (autorefreshChanged) {
            final String enabledString = (enabled) ? "enabled" : "disabled";
            DroiLog.d("Refresh " + enabledString + " for ad unit (" + mAdUnitId + ").");
        }

        mAutoRefreshEnabled = enabled;
        if (mAdWasLoaded && mAutoRefreshEnabled) {
            scheduleRefreshTimerIfEnabled();
        } else if (!mAutoRefreshEnabled) {
            cancelRefreshTimer();
        }
    }

    @Nullable
    public AdReport getAdReport() {
        if (mAdUnitId != null && mAdResponse != null) {
            return new AdReport(mAdUnitId, ClientMetadata.getInstance(mContext), mAdResponse);
        }
        return null;
    }

    public boolean getTesting() {
        return mIsTesting;
    }

    public void setTesting(boolean enabled) {
        mIsTesting = enabled;
    }

    boolean isDestroyed() {
        return mIsDestroyed;
    }

    /*
     * Clean up the internal state of the AdViewController.
     */
    void cleanup() {
        if (mIsDestroyed) {
            return;
        }

        if (mActiveRequest != null) {
            mActiveRequest.cancel();
            mActiveRequest = null;
        }

        setAutorefreshEnabled(false);
        cancelRefreshTimer();

        // WebView subclasses are not garbage-collected in a timely fashion on Froyo and below,
        // thanks to some persistent references in WebViewCore. We manually release some resources
        // to compensate for this "leak".
        mDroiView = null;
        mContext = null;
        mUrlGenerator = null;

        // Flag as destroyed. LoadUrlTask checks this before proceeding in its onPostExecute().
        mIsDestroyed = true;
    }

    Integer getAdTimeoutDelay() {
        return mTimeoutMilliseconds;
    }

    void trackImpression() {
        if (mAdResponse != null) {
            TrackingRequest.makeTrackingHttpRequest(mAdResponse.getImpressionTrackingUrl(),
                    mContext, BaseEvent.Name.IMPRESSION_REQUEST);
        }
    }

    void registerClick() {
        if (mAdResponse != null) {
            // Click tracker fired from Banners and Interstitials
            TrackingRequest.makeTrackingHttpRequest(mAdResponse.getClickTrackingUrl(),
                    mContext, BaseEvent.Name.CLICK_REQUEST);
        }
    }

    void fetchAd(String url) {
        DroiView droiView = getDroiView();
        if (droiView == null || mContext == null) {
            DroiLog.d("Can't load an ad in this ad view because it was destroyed.");
            setNotLoading();
            return;
        }

        AdRequest adRequest = new AdRequest(url,
                droiView.getAdFormat(),
                mAdUnitId,
                mContext,
                mAdListener
        );
        RequestQueue requestQueue = Networking.getRequestQueue(mContext);
        requestQueue.add(adRequest);
        mActiveRequest = adRequest;
    }

    void forceRefresh() {
        setNotLoading();
        loadAd();
    }

    @Nullable
    String generateAdUrl() {
        return mUrlGenerator == null ? null : mUrlGenerator
                .withAdUnitId(mAdUnitId)
                .withKeywords(mKeywords)
                .withLocation(mLocation)
                .generateUrlString(Constants.HOST);
    }

    void adDidFail(DroiErrorCode errorCode) {
        DroiLog.i("Ad failed to load.");
        setNotLoading();

        DroiView droiView = getDroiView();
        if (droiView == null) {
            return;
        }

        scheduleRefreshTimerIfEnabled();
        droiView.adFailed(errorCode);
    }

    void scheduleRefreshTimerIfEnabled() {
        cancelRefreshTimer();
        if (mAutoRefreshEnabled && mRefreshTimeMillis != null && mRefreshTimeMillis > 0) {

            mHandler.postDelayed(mRefreshRunnable,
                    Math.min(MAX_REFRESH_TIME_MILLISECONDS,
                            mRefreshTimeMillis * (long) Math.pow(BACKOFF_FACTOR, mBackoffPower)));
        }
    }

    void setLocalExtras(Map<String, Object> localExtras) {
        mLocalExtras = (localExtras != null)
                ? new TreeMap<String,Object>(localExtras)
                : new TreeMap<String,Object>();
    }

    /**
     * Returns a copied map of localExtras
     */
    Map<String, Object> getLocalExtras() {
        return (mLocalExtras != null)
                ? new TreeMap<String,Object>(mLocalExtras)
                : new TreeMap<String,Object>();
    }

    private void cancelRefreshTimer() {
        mHandler.removeCallbacks(mRefreshRunnable);
    }

    private boolean isNetworkAvailable() {
        if (mContext == null) {
            return false;
        }
        // If we don't have network state access, just assume the network is up.
        if (!DeviceUtils.isPermissionGranted(mContext, ACCESS_NETWORK_STATE)) {
            return true;
        }

        // Otherwise, perform the connectivity check.
        ConnectivityManager cm
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    void setAdContentView(final View view) {
        // XXX: This method is called from the WebViewClient's callbacks, which has caused an error on a small portion of devices
        // We suspect that the code below may somehow be running on the wrong UI Thread in the rare case.
        // see: https://stackoverflow.com/questions/10426120/android-got-calledfromwrongthreadexception-in-onpostexecute-how-could-it-be
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                DroiView mDroiView = getDroiView();
                if (mDroiView == null) {
                    return;
                }
                mDroiView.removeAllViews();
                mDroiView.addView(view, getAdLayoutParams(view));
            }
        });
    }

    private FrameLayout.LayoutParams getAdLayoutParams(View view) {
        Integer width = null;
        Integer height = null;
        if (mAdResponse != null) {
            width = mAdResponse.getWidth();
            height = mAdResponse.getHeight();
        }

        if (width != null && height != null && getShouldHonorServerDimensions(view) && width > 0 && height > 0) {
            int scaledWidth = Dips.asIntPixels(width, mContext);
            int scaledHeight = Dips.asIntPixels(height, mContext);

            return new FrameLayout.LayoutParams(scaledWidth, scaledHeight, Gravity.CENTER);
        } else {
            return WRAP_AND_CENTER_LAYOUT_PARAMS;
        }
    }

    @Deprecated // for testing
    @VisibleForTesting
    Integer getRefreshTimeMillis() {
        return mRefreshTimeMillis;
    }

    @Deprecated // for testing
    @VisibleForTesting
    void setRefreshTimeMillis(@Nullable final Integer refreshTimeMillis) {
        mRefreshTimeMillis = refreshTimeMillis;
    }
}
