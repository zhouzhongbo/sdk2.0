package com.droi.mobileads.factories;

import android.content.Context;

import com.droi.common.AdReport;
import com.droi.mobileads.HtmlBannerWebView;

import static com.droi.mobileads.CustomEventBanner.CustomEventBannerListener;

public class HtmlBannerWebViewFactory {
    protected static HtmlBannerWebViewFactory instance = new HtmlBannerWebViewFactory();

    public static HtmlBannerWebView create(
            Context context,
            AdReport adReport,
            CustomEventBannerListener customEventBannerListener,
            boolean isScrollable,
            String redirectUrl,
            String clickthroughUrl) {
        return instance.internalCreate(context, adReport, customEventBannerListener, isScrollable, redirectUrl, clickthroughUrl);
    }

    public HtmlBannerWebView internalCreate(
            Context context,
            AdReport adReport,
            CustomEventBannerListener customEventBannerListener,
            boolean isScrollable,
            String redirectUrl,
            String clickthroughUrl) {
        HtmlBannerWebView htmlBannerWebView = new HtmlBannerWebView(context, adReport);
        htmlBannerWebView.init(customEventBannerListener, isScrollable, redirectUrl, clickthroughUrl, adReport.getDspCreativeId());
        return htmlBannerWebView;
    }

    @Deprecated // for testing
    public static void setInstance(HtmlBannerWebViewFactory factory) {
        instance = factory;
    }
}

