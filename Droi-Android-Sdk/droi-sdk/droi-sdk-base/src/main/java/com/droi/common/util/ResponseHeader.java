package com.droi.common.util;

/**
 * response head key
 *
 */
public enum ResponseHeader {
    AD_TIMEOUT("adTimeout"),
    AD_TYPE("adtype"),
    CLICK_TRACKING_URL("clickthrough"),
    CUSTOM_EVENT_DATA("customEventClassData"),
    CUSTOM_EVENT_NAME("customEventClassName"),
    CUSTOM_EVENT_HTML_DATA("customEventHtmlData"),
    CREATIVE_ID("creativeId"),
    DSP_CREATIVE_ID("dspCreativeid"),
    FAIL_URL("failURL"),
    FULL_AD_TYPE("fulladtype"),
    HEIGHT("height"),
    IMPRESSION_URL("imptrackerURL"),
    REDIRECT_URL("LaunchpageURL"),
    NATIVE_PARAMS("nativeparams"),
    NETWORK_TYPE("networktype"),
    ORIENTATION("orientation"),
    REFRESH_TIME("refreshTime"),
    SCROLLABLE("scrollable"),
    WARMUP("warmup"),
    WIDTH("width"),

    LOCATION("location"),
    USER_AGENT("userAgent"),
    ACCEPT_LANGUAGE("acceptLanguage"),

    // Native Video fields
    PLAY_VISIBLE_PERCENT("playVisiblePercent"),
    PAUSE_VISIBLE_PERCENT("pauseVisiblePercent"),
    IMPRESSION_MIN_VISIBLE_PERCENT("impressionMinVisiblePercent"),
    IMPRESSION_VISIBLE_MS("impressionVisibleMs"),
    MAX_BUFFER_MS("maxBufferMs"),

    // Rewarded Video fields
    REWARDED_VIDEO_CURRENCY_NAME("rewardedVideoCurrencyName"),
    REWARDED_VIDEO_CURRENCY_AMOUNT("rewardedVideoCurrencyAmount"),
    REWARDED_VIDEO_COMPLETION_URL("rewardedVideoCompletionUrl"),

    @Deprecated CUSTOM_SELECTOR("customselector");

    private final String key;
    ResponseHeader(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
