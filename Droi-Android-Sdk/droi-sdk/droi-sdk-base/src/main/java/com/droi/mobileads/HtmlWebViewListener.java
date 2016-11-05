package com.droi.mobileads;

public interface HtmlWebViewListener {
    void onLoaded(BaseHtmlWebView mHtmlWebView);
    void onFailed(DroiErrorCode unspecified);
    void onClicked();
    void onCollapsed();
}
