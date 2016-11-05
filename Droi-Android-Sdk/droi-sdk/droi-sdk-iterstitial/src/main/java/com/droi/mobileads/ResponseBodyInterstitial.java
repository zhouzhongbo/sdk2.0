package com.droi.mobileads;

import android.content.Context;

import com.droi.common.AdReport;
import com.droi.common.logging.DroiLog;

import java.util.Map;

import static com.droi.common.DataKeys.AD_REPORT_KEY;
import static com.droi.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.droi.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.droi.mobileads.DroiErrorCode.NETWORK_INVALID_STATE;

public abstract class ResponseBodyInterstitial extends CustomEventInterstitial {
    private EventForwardingBroadcastReceiver mBroadcastReceiver;
    protected Context mContext;
    protected AdReport mAdReport;
    protected long mBroadcastIdentifier;

    abstract protected void extractExtras(Map<String, String> serverExtras);
    abstract protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener);
    public abstract void showInterstitial();

    @Override
    public void loadInterstitial(
            Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {

        mContext = context;

        if (extrasAreValid(serverExtras)) {
            extractExtras(serverExtras);
        } else {
            customEventInterstitialListener.onInterstitialFailed(NETWORK_INVALID_STATE);
            return;
        }


        try {
            mAdReport = (AdReport) localExtras.get(AD_REPORT_KEY);
            Long boxedBroadcastId = (Long) localExtras.get(BROADCAST_IDENTIFIER_KEY);
            if (boxedBroadcastId == null) {
                DroiLog.e("Broadcast Identifier was not set in localExtras");
                customEventInterstitialListener.onInterstitialFailed(DroiErrorCode.INTERNAL_ERROR);
                return;
            }
            mBroadcastIdentifier = boxedBroadcastId;
        } catch (ClassCastException e) {
            DroiLog.e("LocalExtras contained an incorrect type.");
            customEventInterstitialListener.onInterstitialFailed(DroiErrorCode.INTERNAL_ERROR);
            return;
        }

        mBroadcastReceiver = new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                mBroadcastIdentifier);
        mBroadcastReceiver.register(mBroadcastReceiver, context);

        preRenderHtml(customEventInterstitialListener);
    }

    @Override
    public void onInvalidate() {
        if (mBroadcastReceiver != null) {
            mBroadcastReceiver.unregister(mBroadcastReceiver);
        }
    }

    private boolean extrasAreValid(Map<String,String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }
}
