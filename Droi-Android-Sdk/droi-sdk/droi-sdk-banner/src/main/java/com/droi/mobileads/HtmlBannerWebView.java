package com.droi.mobileads;

import android.content.Context;

import com.droi.common.AdReport;
import com.droi.common.Droi;

import static com.droi.mobileads.CustomEventBanner.CustomEventBannerListener;

public class HtmlBannerWebView extends BaseHtmlWebView {
    public static final String EXTRA_AD_CLICK_DATA = "com.droi.intent.extra.AD_CLICK_DATA";

    public HtmlBannerWebView(Context context, AdReport adReport) {
        super(context, adReport);
    }

    public void init(CustomEventBannerListener customEventBannerListener, boolean isScrollable, String redirectUrl, String clickthroughUrl, String dspCreativeId) {
        super.init(isScrollable);

        setWebViewClient(new HtmlWebViewClient(new HtmlBannerWebViewListener(customEventBannerListener), this, clickthroughUrl, redirectUrl, dspCreativeId));
    }

    static class HtmlBannerWebViewListener implements HtmlWebViewListener {
        private final CustomEventBannerListener mCustomEventBannerListener;

        public HtmlBannerWebViewListener(CustomEventBannerListener customEventBannerListener) {
            mCustomEventBannerListener = customEventBannerListener;
        }

        @Override
        public void onLoaded(BaseHtmlWebView htmlWebView) {
            mCustomEventBannerListener.onBannerLoaded(htmlWebView);
        }

        @Override
        public void onFailed(DroiErrorCode errorCode) {
            mCustomEventBannerListener.onBannerFailed(errorCode);
        }

        @Override
        public void onClicked() {
            mCustomEventBannerListener.onBannerClicked();
        }

        @Override
        public void onCollapsed() {
            mCustomEventBannerListener.onBannerCollapsed();
        }

    }
}
