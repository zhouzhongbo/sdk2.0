package com.droi.mraid;


import android.support.annotation.NonNull;

import com.droi.mobileads.MraidActivity;
import com.droi.mobileads.ResponseBodyInterstitial;

import java.util.Map;

import static com.droi.common.DataKeys.HTML_RESPONSE_BODY_KEY;

class MraidInterstitial extends ResponseBodyInterstitial {
    private String mHtmlData;

    @Override
    protected void extractExtras(Map<String, String> serverExtras) {
        mHtmlData = serverExtras.get(HTML_RESPONSE_BODY_KEY);
    }

    @Override
    protected void preRenderHtml(@NonNull CustomEventInterstitialListener
            customEventInterstitialListener) {
        MraidActivity.preRenderHtml(mContext, customEventInterstitialListener, mHtmlData);
    }

    @Override
    public void showInterstitial() {
        MraidActivity.start(mContext, mAdReport, mHtmlData, mBroadcastIdentifier);
    }
}
