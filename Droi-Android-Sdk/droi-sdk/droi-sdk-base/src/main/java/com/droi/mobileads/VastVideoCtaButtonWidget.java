package com.droi.mobileads;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.droi.common.VisibleForTesting;
import com.droi.common.logging.DroiLog;
import com.droi.common.util.Dips;
import com.droi.common.util.Utils;
import com.droi.mobileads.resource.CtaButtonDrawable;
import com.droi.mobileads.resource.DrawableConstants;

public class VastVideoCtaButtonWidget extends ImageView {
    @NonNull private CtaButtonDrawable mCtaButtonDrawable;
    @NonNull private final RelativeLayout.LayoutParams mLandscapeLayoutParams;
    @NonNull private final RelativeLayout.LayoutParams mPortraitLayoutParams;

    private boolean mIsVideoSkippable;
    private boolean mIsVideoComplete;
    private boolean mHasCompanionAd;
    private boolean mHasClickthroughUrl;
    private boolean mHasSocialActions;

    public VastVideoCtaButtonWidget(@NonNull final Context context, final int videoViewId,
            final boolean hasCompanionAd, final boolean hasClickthroughUrl) {
        super(context);

        mHasCompanionAd = hasCompanionAd;
        mHasClickthroughUrl = hasClickthroughUrl;
        mHasSocialActions = false;

        setId((int) Utils.generateUniqueId());

        final int width = Dips.dipsToIntPixels(DrawableConstants.CtaButton.WIDTH_DIPS, context);
        final int height = Dips.dipsToIntPixels(DrawableConstants.CtaButton.HEIGHT_DIPS, context);
        final int margin = Dips.dipsToIntPixels(DrawableConstants.CtaButton.MARGIN_DIPS, context);

        mCtaButtonDrawable = new CtaButtonDrawable(context);
        setImageDrawable(mCtaButtonDrawable);

        // landscape layout: placed bottom-right corner of video view
        mLandscapeLayoutParams = new RelativeLayout.LayoutParams(width, height);
        mLandscapeLayoutParams.setMargins(margin, margin, margin, margin);
        mLandscapeLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, videoViewId);
        mLandscapeLayoutParams.addRule(RelativeLayout.ALIGN_RIGHT, videoViewId);

        // portrait layout: placed bottom-right corner of screen
        mPortraitLayoutParams = new RelativeLayout.LayoutParams(width, height);
        mPortraitLayoutParams.setMargins(margin, margin, margin, margin);
        mPortraitLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mPortraitLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        updateLayoutAndVisibility();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateLayoutAndVisibility();
    }

    void updateCtaText(@NonNull final String customCtaText) {
        mCtaButtonDrawable.setCtaText(customCtaText);
    }

    void setHasSocialActions(final boolean hasSocialActions) {
        mHasSocialActions = hasSocialActions;
    }

    boolean getHasSocialActions() {
        return mHasSocialActions;
    }

    void notifyVideoSkippable() {
        mIsVideoSkippable = true;
        updateLayoutAndVisibility();
    }

    void notifyVideoComplete() {
        mIsVideoSkippable = true;
        mIsVideoComplete = true;
        updateLayoutAndVisibility();
    }

    private void updateLayoutAndVisibility() {
        // If the video does not have a clickthrough url, never show the CTA button
        if (!mHasClickthroughUrl) {
            setVisibility(View.GONE);
            return;
        }

        // If video is not skippable yet, do not show CTA button
        if (!mIsVideoSkippable) {
            setVisibility(View.INVISIBLE);
            return;
        }

        // If video has finished playing and there's a companion ad, do not show CTA button
        // Unless the ad has social actions
        if (mIsVideoComplete && mHasCompanionAd && !mHasSocialActions) {
            setVisibility(View.GONE);
            return;
        }

        final int currentOrientation = getResources().getConfiguration().orientation;

        switch (currentOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                setLayoutParams(mLandscapeLayoutParams);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                setLayoutParams(mPortraitLayoutParams);
                break;
            case Configuration.ORIENTATION_UNDEFINED:
                DroiLog.d("Screen orientation undefined: CTA button widget defaulting to portrait layout");
                setLayoutParams(mPortraitLayoutParams);
                break;
            case Configuration.ORIENTATION_SQUARE:
                DroiLog.d("Screen orientation is deprecated ORIENTATION_SQUARE: CTA button widget defaulting to portrait layout");
                setLayoutParams(mPortraitLayoutParams);
                break;
            default:
                DroiLog.d("Unrecognized screen orientation: CTA button widget defaulting to portrait layout");
                setLayoutParams(mPortraitLayoutParams);
                break;
        }
        setVisibility(View.VISIBLE);
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    String getCtaText() {
        return mCtaButtonDrawable.getCtaText();
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    boolean hasPortraitLayoutParams() {
        return getLayoutParams().equals(mPortraitLayoutParams);
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    boolean hasLandscapeLayoutParams() {
        return getLayoutParams().equals(mLandscapeLayoutParams);
    }
}
