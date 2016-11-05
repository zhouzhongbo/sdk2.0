package com.droi.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.droi.common.VisibleForTesting;
import com.droi.common.util.Dips;
import com.droi.common.util.Utils;
import com.droi.mobileads.resource.DrawableConstants;
import com.droi.mobileads.resource.ProgressBarDrawable;

public class VastVideoProgressBarWidget extends ImageView {
    @NonNull private ProgressBarDrawable mProgressBarDrawable;
    private final int mProgressBarHeight;

    public VastVideoProgressBarWidget(@NonNull final Context context) {
        super(context);

        setId((int) Utils.generateUniqueId());

        mProgressBarDrawable = new ProgressBarDrawable(context);
        setImageDrawable(mProgressBarDrawable);

        mProgressBarHeight =
                Dips.dipsToIntPixels(DrawableConstants.ProgressBar.HEIGHT_DIPS, context);
    }

    public void setAnchorId(final int anchorId) {
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                mProgressBarHeight);
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, anchorId);
        setLayoutParams(layoutParams);

    }

    public void calibrateAndMakeVisible(final int duration, final int skipOffset) {
        mProgressBarDrawable.setDurationAndSkipOffset(duration, skipOffset);
        setVisibility(View.VISIBLE);
    }

    public void updateProgress(final int progress) {
        mProgressBarDrawable.setProgress(progress);
    }

    public void reset() {
        mProgressBarDrawable.reset();
        mProgressBarDrawable.setProgress(0);
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    ProgressBarDrawable getImageViewDrawable() {
        return mProgressBarDrawable;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setImageViewDrawable(@NonNull ProgressBarDrawable drawable) {
        mProgressBarDrawable = drawable;
    }
}
