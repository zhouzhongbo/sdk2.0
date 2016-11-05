package com.droi.mobileads;

import android.support.annotation.NonNull;

import com.droi.common.CreativeOrientation;

import java.util.Map;

import static com.droi.common.DataKeys.CLICKTHROUGH_URL_KEY;
import static com.droi.common.DataKeys.CREATIVE_ORIENTATION_KEY;
import static com.droi.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.droi.common.DataKeys.REDIRECT_URL_KEY;
import static com.droi.common.DataKeys.SCROLLABLE_KEY;

public class HtmlInterstitial extends ResponseBodyInterstitial {
    private String mHtmlData;
    private boolean mIsScrollable;
    private String mRedirectUrl;
    private String mClickthroughUrl;
    @NonNull
    private CreativeOrientation mOrientation;

    @Override
    protected void extractExtras(Map<String, String> serverExtras) {
        mHtmlData = serverExtras.get(HTML_RESPONSE_BODY_KEY);
        mIsScrollable = Boolean.valueOf(serverExtras.get(SCROLLABLE_KEY));
        mRedirectUrl = serverExtras.get(REDIRECT_URL_KEY);
        mClickthroughUrl = serverExtras.get(CLICKTHROUGH_URL_KEY);
        mOrientation = CreativeOrientation.fromHeader(serverExtras.get(CREATIVE_ORIENTATION_KEY));
    }

    @Override
    protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener) {
        DroiActivity.preRenderHtml(mContext, mAdReport, customEventInterstitialListener, mHtmlData);
    }

    @Override
    public void showInterstitial() {
        DroiActivity.start(mContext, mHtmlData, mAdReport, mIsScrollable,
                mRedirectUrl, mClickthroughUrl, mOrientation,
                mBroadcastIdentifier);
    }
}
