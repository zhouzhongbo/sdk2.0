package com.droi.mraid;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.droi.common.AdReport;
import com.droi.common.VisibleForTesting;
import com.droi.common.logging.DroiLog;
import com.droi.mobileads.AdViewController;
import com.droi.mobileads.CustomEventBanner;
import com.droi.mobileads.factories.MraidControllerFactory;
import com.droi.mraid.MraidController.MraidListener;

import java.util.Map;

import static com.droi.common.DataKeys.AD_REPORT_KEY;
import static com.droi.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.droi.mobileads.DroiErrorCode.MRAID_LOAD_ERROR;

class MraidBanner extends CustomEventBanner {

    @Nullable private MraidController mMraidController;
    @Nullable private CustomEventBannerListener mBannerListener;
    @Nullable private MraidWebViewDebugListener mDebugListener;

    @Override
    protected void loadBanner(@NonNull Context context,
                    @NonNull CustomEventBannerListener customEventBannerListener,
                    @NonNull Map<String, Object> localExtras,
                    @NonNull Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;

        String htmlData;
        if (extrasAreValid(serverExtras)) {
            htmlData = serverExtras.get(HTML_RESPONSE_BODY_KEY);
        } else {
            mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            return;
        }

        try {
            AdReport adReport = (AdReport) localExtras.get(AD_REPORT_KEY);
            mMraidController = MraidControllerFactory.create(
                    context, adReport, PlacementType.INLINE);
        } catch (ClassCastException e) {
            DroiLog.w("MRAID banner creating failed:", e);
            mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            return;
        }

        mMraidController.setDebugListener(mDebugListener);
        mMraidController.setMraidListener(new MraidListener() {
            @Override
            public void onLoaded(View view) {
                // Honoring the server dimensions forces the WebView to be the size of the banner
                AdViewController.setShouldHonorServerDimensions(view);
                mBannerListener.onBannerLoaded(view);
            }

            @Override
            public void onFailedToLoad() {
                mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            }

            @Override
            public void onExpand() {
                mBannerListener.onBannerExpanded();
                mBannerListener.onBannerClicked();
            }

            @Override
            public void onOpen() {
                mBannerListener.onBannerClicked();
            }

            @Override
            public void onClose() {
                mBannerListener.onBannerCollapsed();
            }
        });
        mMraidController.loadContent(htmlData);
    }

    @Override
    protected void onInvalidate() {
        if (mMraidController != null) {
            mMraidController.setMraidListener(null);
            mMraidController.destroy();
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }

    @VisibleForTesting
    public void setDebugListener(@Nullable MraidWebViewDebugListener debugListener) {
        mDebugListener = debugListener;
        if (mMraidController != null) {
            mMraidController.setDebugListener(debugListener);
        }
    }
}
