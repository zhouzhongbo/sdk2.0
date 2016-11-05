package com.droi.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.droi.common.AdFormat;
import com.droi.common.AdType;
import com.droi.common.util.ResponseHeader;

import java.util.Map;

import static com.droi.network.HeaderUtils.extractHeader;

public class AdTypeTranslator {
    public enum CustomEventType {
        // "Special" custom events that we let people choose in the UI.
        GOOGLE_PLAY_SERVICES_BANNER("admob_native_banner", "com.droi.mobileads.GooglePlayServicesBanner"),
        GOOGLE_PLAY_SERVICES_INTERSTITIAL("admob_full_interstitial", "com.droi.mobileads.GooglePlayServicesInterstitial"),
        MILLENNIAL_BANNER("millennial_native_banner", "com.droi.mobileads.MillennialBanner"),
        MILLENNIAL_INTERSTITIAL("millennial_full_interstitial", "com.droi.mobileads.MillennialInterstitial"),

        // Droi-specific custom events.
        MRAID_BANNER("mraid_banner", "com.droi.mraid.MraidBanner"),
        MRAID_INTERSTITIAL("mraid_interstitial", "com.droi.mraid.MraidInterstitial"),
        HTML_BANNER("html_banner", "com.droi.mobileads.HtmlBanner"),
        HTML_INTERSTITIAL("html_interstitial", "com.droi.mobileads.HtmlInterstitial"),
        VAST_VIDEO_INTERSTITIAL("vast_interstitial", "com.droi.mobileads.VastVideoInterstitial"),
        DROI_NATIVE("droi_native", "com.droi.nativeads.DroiCustomEventNative"),
        DROI_VIDEO_NATIVE("droi_video_native", "com.droi.nativeads.DroiCustomEventVideoNative"),
        DROI_REWARDED_VIDEO("rewarded_video", "com.droi.mobileads.DroiRewardedVideo"),

        UNSPECIFIED("", null);

        private final String mKey;
        private final String mClassName;

        private CustomEventType(String key, String className) {
            mKey = key;
            mClassName = className;
        }

        private static CustomEventType fromString(String key) {
            for (CustomEventType customEventType : values()) {
                if (customEventType.mKey.equals(key)) {
                    return customEventType;
                }
            }

            return UNSPECIFIED;
        }

        @Override
        public String toString() {
            return mClassName;
        }
    }

    public static final String BANNER_SUFFIX = "_banner";
    public static final String INTERSTITIAL_SUFFIX = "_interstitial";

    static String getAdNetworkType(String adType, String fullAdType) {
        String adNetworkType = AdType.INTERSTITIAL.equals(adType) ? fullAdType : adType;
        return adNetworkType != null ? adNetworkType : "unknown";
    }

    public static String getCustomEventName(@NonNull AdFormat adFormat,
            @NonNull String adType,
            @Nullable String fullAdType,
            @NonNull Map<String, String> headers) {
        if (AdType.CUSTOM.equalsIgnoreCase(adType)) {
            return extractHeader(headers, ResponseHeader.CUSTOM_EVENT_NAME);
        } else if (AdType.STATIC_NATIVE.equalsIgnoreCase(adType)){
            return CustomEventType.DROI_NATIVE.toString();
        } else if (AdType.VIDEO_NATIVE.equalsIgnoreCase(adType)) {
            return CustomEventType.DROI_VIDEO_NATIVE.toString();
        } else if (AdType.REWARDED_VIDEO.equalsIgnoreCase(adType)) {
            return CustomEventType.DROI_REWARDED_VIDEO.toString();
        } else if (AdType.HTML.equalsIgnoreCase(adType) || AdType.MRAID.equalsIgnoreCase(adType)) {
            return (AdFormat.INTERSTITIAL.equals(adFormat)
                    ? CustomEventType.fromString(adType + INTERSTITIAL_SUFFIX)
                    : CustomEventType.fromString(adType + BANNER_SUFFIX)).toString();
        } else if (AdType.INTERSTITIAL.equalsIgnoreCase(adType)) {
            return CustomEventType.fromString(fullAdType + INTERSTITIAL_SUFFIX).toString();
        } else {
            return CustomEventType.fromString(adType + BANNER_SUFFIX).toString();
        }
    }
}
