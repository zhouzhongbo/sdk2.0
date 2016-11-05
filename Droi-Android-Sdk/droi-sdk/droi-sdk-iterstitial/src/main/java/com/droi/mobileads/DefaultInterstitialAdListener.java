package com.droi.mobileads;

import static com.droi.mobileads.DroiInterstitial.InterstitialAdListener;

public class DefaultInterstitialAdListener implements InterstitialAdListener {
    @Override public void onInterstitialLoaded(DroiInterstitial interstitial) { }
    @Override public void onInterstitialFailed(DroiInterstitial interstitial, DroiErrorCode errorCode) { }
    @Override public void onInterstitialShown(DroiInterstitial interstitial) { }
    @Override public void onInterstitialClicked(DroiInterstitial interstitial) { }
    @Override public void onInterstitialDismissed(DroiInterstitial interstitial) { }
}
