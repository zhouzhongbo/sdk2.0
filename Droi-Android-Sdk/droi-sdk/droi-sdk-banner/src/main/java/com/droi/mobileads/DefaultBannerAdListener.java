package com.droi.mobileads;

import com.droi.common.Droi;

import static com.droi.mobileads.DroiView.BannerAdListener;

public class DefaultBannerAdListener implements BannerAdListener {
    @Override public void onBannerLoaded(DroiView banner) { }
    @Override public void onBannerFailed(DroiView banner, DroiErrorCode errorCode) { }
    @Override public void onBannerClicked(DroiView banner) { }
    @Override public void onBannerExpanded(DroiView banner) { }
    @Override public void onBannerCollapsed(DroiView banner) { }
}
