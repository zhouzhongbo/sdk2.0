package com.droi.common;

/**
 * IntentActions are used by a {@link com.droi.mobileads.BaseBroadcastReceiver}
 * to relay information about the current state of a custom event activity.
 */
public class IntentActions {
    public static final String ACTION_INTERSTITIAL_FAIL = "com.droi.action.interstitial.fail";
    public static final String ACTION_INTERSTITIAL_SHOW = "com.droi.action.interstitial.show";
    public static final String ACTION_INTERSTITIAL_DISMISS = "com.droi.action.interstitial.dismiss";
    public static final String ACTION_INTERSTITIAL_CLICK = "com.droi.action.interstitial.click";

    public static final String ACTION_REWARDED_VIDEO_COMPLETE = "com.droi.action.rewardedvideo.complete";

    private IntentActions() {}
}
