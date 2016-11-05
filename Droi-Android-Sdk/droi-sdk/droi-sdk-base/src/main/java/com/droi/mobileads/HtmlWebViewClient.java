package com.droi.mobileads;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.droi.common.UrlHandler;
import com.droi.common.UrlAction;
import com.droi.common.logging.DroiLog;
import com.droi.common.util.Intents;
import com.droi.exceptions.IntentNotResolvableException;

import java.util.EnumSet;

import static com.droi.mobileads.DroiErrorCode.UNSPECIFIED;

class HtmlWebViewClient extends WebViewClient {
    static final String DROI_FINISH_LOAD = "droi://finishLoad";
    static final String DROI_FAIL_LOAD = "droi://failLoad";

    private final EnumSet<UrlAction> SUPPORTED_URL_ACTIONS = EnumSet.of(
            UrlAction.HANDLE_DROI_SCHEME,
            UrlAction.IGNORE_ABOUT_SCHEME,
            UrlAction.HANDLE_PHONE_SCHEME,
            UrlAction.OPEN_APP_MARKET,
            UrlAction.OPEN_NATIVE_BROWSER,
            UrlAction.OPEN_IN_APP_BROWSER,
            UrlAction.HANDLE_SHARE_TWEET,
            UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
            UrlAction.FOLLOW_DEEP_LINK);

    private final Context mContext;
    private final String mDspCreativeId;
    private HtmlWebViewListener mHtmlWebViewListener;
    private BaseHtmlWebView mHtmlWebView;
    private final String mClickthroughUrl;
    private final String mRedirectUrl;

    HtmlWebViewClient(HtmlWebViewListener htmlWebViewListener,
            BaseHtmlWebView htmlWebView, String clickthrough,
            String redirect, String dspCreativeId) {
        mHtmlWebViewListener = htmlWebViewListener;
        mHtmlWebView = htmlWebView;
        mClickthroughUrl = clickthrough;
        mRedirectUrl = redirect;
        mDspCreativeId = dspCreativeId;
        mContext = htmlWebView.getContext();
    }

    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        new UrlHandler.Builder()
                .withDspCreativeId(mDspCreativeId)
                .withSupportedUrlActions(SUPPORTED_URL_ACTIONS)
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                            @NonNull UrlAction urlAction) {
                        if (mHtmlWebView.wasClicked()) {
                            mHtmlWebViewListener.onClicked();
                            mHtmlWebView.onResetUserClick();
                        }
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                            @NonNull UrlAction lastFailedUrlAction) {
                    }
                })
                .withDroiSchemeListener(new UrlHandler.DroiSchemeListener() {
                    @Override
                    public void onFinishLoad() {
                        mHtmlWebViewListener.onLoaded(mHtmlWebView);
                    }

                    @Override
                    public void onClose() {
                        mHtmlWebViewListener.onCollapsed();
                    }

                    @Override
                    public void onFailLoad() {
                        mHtmlWebViewListener.onFailed(UNSPECIFIED);
                    }
                })
                .build().handleUrl(mContext, url, mHtmlWebView.wasClicked());
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // If the URL being loaded shares the redirectUrl prefix, open it in the browser.
        if (mRedirectUrl != null && url.startsWith(mRedirectUrl)) {
            view.stopLoading();
            if (mHtmlWebView.wasClicked()) {
                try {
                    Intents.showDroiBrowserForUrl(mContext, Uri.parse(url), mDspCreativeId);
                } catch (IntentNotResolvableException e) {
                    DroiLog.d(e.getMessage());
                }
            } else {
                DroiLog.d("Attempted to redirect without user interaction");
            }
        }
    }

}
