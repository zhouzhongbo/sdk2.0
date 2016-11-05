package com.droi.common;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.droi.common.logging.DroiLog;
import com.droi.exceptions.IntentNotResolvableException;

import java.util.EnumSet;

import static com.droi.common.util.Drawables.LEFT_ARROW;
import static com.droi.common.util.Drawables.RIGHT_ARROW;
import static com.droi.common.util.Drawables.UNLEFT_ARROW;
import static com.droi.common.util.Drawables.UNRIGHT_ARROW;

class BrowserWebViewClient extends WebViewClient {

    private static final EnumSet<UrlAction> SUPPORTED_URL_ACTIONS = EnumSet.of(
            UrlAction.HANDLE_PHONE_SCHEME,
            UrlAction.OPEN_APP_MARKET,
            UrlAction.OPEN_IN_APP_BROWSER,
            UrlAction.HANDLE_SHARE_TWEET,
            UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
            UrlAction.FOLLOW_DEEP_LINK
    );

    @NonNull
    private DroiBrowser mdroiBrowser;

    public BrowserWebViewClient(@NonNull final DroiBrowser droiBrowser) {
        mdroiBrowser = droiBrowser;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description,
            String failingUrl) {
        DroiLog.d("DroiBrowser error: " + description);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        UrlHandler urlHandler = new UrlHandler.Builder()
                .withSupportedUrlActions(SUPPORTED_URL_ACTIONS)
                .withoutDroiBrowser()
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                            @NonNull UrlAction urlAction) {
                        if (urlAction.equals(UrlAction.OPEN_IN_APP_BROWSER)) {
                            mdroiBrowser.getWebView().loadUrl(url);
                        } else {
                            // UrlAction opened in external app, so close DroiBrowser
                            mdroiBrowser.finish();
                        }
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                            @NonNull UrlAction lastFailedUrlAction) {
                    }
                })
                .build();

        return urlHandler.handleResolvedUrl(mdroiBrowser.getApplicationContext(), url,
                true, // = fromUserInteraction
                null // = trackingUrls
        );
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mdroiBrowser.getForwardButton()
                .setImageDrawable(UNRIGHT_ARROW.createDrawable(mdroiBrowser));
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        Drawable backImageDrawable = view.canGoBack()
                ? LEFT_ARROW.createDrawable(mdroiBrowser)
                : UNLEFT_ARROW.createDrawable(mdroiBrowser);
        mdroiBrowser.getBackButton().setImageDrawable(backImageDrawable);

        Drawable forwardImageDrawable = view.canGoForward()
                ? RIGHT_ARROW.createDrawable(mdroiBrowser)
                : UNRIGHT_ARROW.createDrawable(mdroiBrowser);
        mdroiBrowser.getForwardButton().setImageDrawable(forwardImageDrawable);
    }
}
