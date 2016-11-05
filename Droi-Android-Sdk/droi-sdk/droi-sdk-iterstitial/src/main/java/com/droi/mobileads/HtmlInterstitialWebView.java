package com.droi.mobileads;

import android.content.Context;
import android.os.Handler;

import com.droi.common.AdReport;
import com.droi.mobileads.BaseHtmlWebView;

import static com.droi.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;

public class HtmlInterstitialWebView extends BaseHtmlWebView {
    private Handler mHandler;

    public HtmlInterstitialWebView(Context context, AdReport adReport) {
        super(context, adReport);

        mHandler = new Handler();
    }

    public void init(final CustomEventInterstitialListener customEventInterstitialListener, boolean isScrollable, String redirectUrl, String clickthroughUrl, String dspCreativeId) {
        super.init(isScrollable);

        HtmlInterstitialWebViewListener htmlInterstitialWebViewListener = new HtmlInterstitialWebViewListener(customEventInterstitialListener);
        HtmlWebViewClient htmlWebViewClient = new HtmlWebViewClient(htmlInterstitialWebViewListener, this, clickthroughUrl, redirectUrl, dspCreativeId);
        setWebViewClient(htmlWebViewClient);
    }

    private void postHandlerRunnable(Runnable r) {
        mHandler.post(r);
    }

    static class HtmlInterstitialWebViewListener implements HtmlWebViewListener {
        private final CustomEventInterstitialListener mCustomEventInterstitialListener;

        public HtmlInterstitialWebViewListener(CustomEventInterstitialListener customEventInterstitialListener) {
            mCustomEventInterstitialListener = customEventInterstitialListener;
        }

        @Override
        public void onLoaded(BaseHtmlWebView mHtmlWebView) {
            mCustomEventInterstitialListener.onInterstitialLoaded();
        }

        @Override
        public void onFailed(DroiErrorCode errorCode) {
            mCustomEventInterstitialListener.onInterstitialFailed(errorCode);
        }

        @Override
        public void onClicked() {
            mCustomEventInterstitialListener.onInterstitialClicked();
        }

        @Override
        public void onCollapsed() {
            // Ignored
        }
    }
}
