package com.droi.mobileads;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;

import com.droi.common.Constants;
import com.droi.common.Preconditions;
import com.droi.common.VisibleForTesting;
import com.droi.common.util.Utils;
import com.droi.common.util.VersionCode;
import com.droi.network.Networking;

import static com.droi.common.util.VersionCode.currentApiLevel;

/**
 * A WebView customized for Vast video needs.
 */
class VastWebView extends BaseWebView {
    interface VastWebViewClickListener {
        void onVastWebViewClick();
    }

    @Nullable VastWebViewClickListener mVastWebViewClickListener;

    VastWebView(Context context) {
        super(context);

        disableScrollingAndZoom();
        getSettings().setJavaScriptEnabled(true);

        if (currentApiLevel().isAtLeast(VersionCode.ICE_CREAM_SANDWICH)) {
            enablePlugins(true);
        }

        setBackgroundColor(Color.TRANSPARENT);
        setOnTouchListener(new VastWebViewOnTouchListener());
        setId((int) Utils.generateUniqueId());
    }

    void loadData(String data) {
        loadDataWithBaseURL(Networking.getBaseUrlScheme() + "://" + Constants.HOST + "/",
                data, "text/html", "utf-8", null);
    }

    void setVastWebViewClickListener(@NonNull VastWebViewClickListener vastWebViewClickListener) {
        mVastWebViewClickListener = vastWebViewClickListener;
    }

    private void disableScrollingAndZoom() {
        setHorizontalScrollBarEnabled(false);
        setHorizontalScrollbarOverlay(false);
        setVerticalScrollBarEnabled(false);
        setVerticalScrollbarOverlay(false);
        getSettings().setSupportZoom(false);
        setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
    }

    /**
     * Creates and populates a webview.
     *
     * @param context      the context.
     * @param vastResource A resource describing the contents of the webview
     * @return a fully populated webview
     */
    @NonNull
    static VastWebView createView(@NonNull final Context context,
            @NonNull final VastResource vastResource) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(vastResource);

        VastWebView webView = new VastWebView(context);
        vastResource.initializeWebView(webView);

        return webView;
    }

    /**
     * Custom on touch listener to easily detect clicks on the entire WebView.
     */
    class VastWebViewOnTouchListener implements View.OnTouchListener {
        private boolean mClickStarted;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mClickStarted = true;
                    break;
                case MotionEvent.ACTION_UP:
                    if (!mClickStarted) {
                        return false;
                    }
                    mClickStarted = false;
                    if (mVastWebViewClickListener != null) {
                        mVastWebViewClickListener.onVastWebViewClick();
                    }
            }

            return false;
        }
    }

    @VisibleForTesting
    @Deprecated
    @NonNull
    VastWebViewClickListener getVastWebViewClickListener() {
        return mVastWebViewClickListener;
    }
}
