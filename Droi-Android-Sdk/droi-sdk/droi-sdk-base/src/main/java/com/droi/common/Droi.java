package com.droi.common;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.droi.common.logging.DroiLog;
import com.droi.common.util.Reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Droi {
    public static final String SDK_VERSION = "4.9.0";

    public enum LocationAwareness { NORMAL, TRUNCATED, DISABLED }

    private static final String DROI_REWARDED_VIDEOS =
            "com.droi.mobileads.DroiRewardedVideos";
    private static final String DROI_REWARDED_VIDEO_MANAGER =
            "com.droi.mobileads.DroiRewardedVideoManager";
    private static final String DROI_REWARDED_VIDEO_LISTENER =
            "com.droi.mobileads.DroiRewardedVideoListener";
    private static final String DROI_REWARDED_VIDEO_MANAGER_REQUEST_PARAMETERS =
            "com.droi.mobileads.DroiRewardedVideoManager$RequestParameters";

    private static final int DEFAULT_LOCATION_PRECISION = 6;
    private static volatile LocationAwareness sLocationLocationAwareness = LocationAwareness.NORMAL;
    private static volatile int sLocationPrecision = DEFAULT_LOCATION_PRECISION;
    private static boolean sSearchedForUpdateActivityMethod = false;
    @Nullable private static Method sUpdateActivityMethod;

    public static LocationAwareness getLocationAwareness() {
        return sLocationLocationAwareness;
    }

    public static void setLocationAwareness(LocationAwareness locationAwareness) {
        sLocationLocationAwareness = locationAwareness;
    }

    public static int getLocationPrecision() {
        return sLocationPrecision;
    }

    /**
     * Sets the precision to use when the SDK's location awareness is set
     * to {@link com.droi.common.Droi.LocationAwareness#TRUNCATED}.
     */
    public static void setLocationPrecision(int precision) {
        sLocationPrecision = Math.min(Math.max(0, precision), DEFAULT_LOCATION_PRECISION);
    }


    //////// Droi LifecycleListener messages ////////

    public static void onCreate(@NonNull final Activity activity) {
        DroiLifecycleManager.getInstance(activity).onCreate(activity);
        updateActivity(activity);
    }

    public static void onStart(@NonNull final Activity activity) {
        DroiLifecycleManager.getInstance(activity).onStart(activity);
        updateActivity(activity);
    }

    public static void onPause(@NonNull final Activity activity) {
        DroiLifecycleManager.getInstance(activity).onPause(activity);
    }

    public static void onResume(@NonNull final Activity activity) {
        DroiLifecycleManager.getInstance(activity).onResume(activity);
        updateActivity(activity);
    }

    public static void onRestart(@NonNull final Activity activity) {
        DroiLifecycleManager.getInstance(activity).onRestart(activity);
        updateActivity(activity);
    }

    public static void onStop(@NonNull final Activity activity) {
        DroiLifecycleManager.getInstance(activity).onStop(activity);
    }

    public static void onDestroy(@NonNull final Activity activity) {
        DroiLifecycleManager.getInstance(activity).onDestroy(activity);
    }

    public static void onBackPressed(@NonNull final Activity activity) {
        DroiLifecycleManager.getInstance(activity).onBackPressed(activity);
    }

    ////////// Droi RewardedVideoControl methods //////////
    // These methods have been deprecated as of release 4.9 due to SDK modularization. Droi is
    // inside of the base module while DroiRewardedVideos is inside of the rewarded video module.
    // DroiRewardedVideos methods must now be called with reflection because the publisher
    // may have excluded the rewarded video module.


    /**
     * @deprecated As of release 4.9, use DroiRewardedVideos#initializeRewardedVideo instead
     */
    @Deprecated
    public static void initializeRewardedVideo(@NonNull Activity activity, MediationSettings... mediationSettings) {
        try {
            new Reflection.MethodBuilder(null, "initializeRewardedVideo")
                    .setStatic(Class.forName(DROI_REWARDED_VIDEOS))
                    .addParam(Activity.class, activity)
                    .addParam(MediationSettings[].class, mediationSettings)
                    .execute();
        } catch (ClassNotFoundException e) {
            DroiLog.w("initializeRewardedVideo was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            DroiLog.w("initializeRewardedVideo was called without the rewarded video module");
        } catch (Exception e) {
            DroiLog.e("Error while initializing rewarded video", e);
        }
    }

    @VisibleForTesting
    static void updateActivity(@NonNull Activity activity) {
        if (!sSearchedForUpdateActivityMethod) {
            sSearchedForUpdateActivityMethod = true;
            try {
                Class mDroiRewardedVideoManagerClass = Class.forName(
                        DROI_REWARDED_VIDEO_MANAGER);
                sUpdateActivityMethod = Reflection.getDeclaredMethodWithTraversal(
                        mDroiRewardedVideoManagerClass, "updateActivity", Activity.class);
            } catch (ClassNotFoundException e) {
                // rewarded video module not included
            } catch (NoSuchMethodException e) {
                // rewarded video module not included
            }
        }

        if (sUpdateActivityMethod != null) {
            try {
                sUpdateActivityMethod.invoke(null, activity);
            } catch (IllegalAccessException e) {
                DroiLog.e("Error while attempting to access the update activity method - this " +
                        "should not have happened", e);
            } catch (InvocationTargetException e) {
                DroiLog.e("Error while attempting to access the update activity method - this " +
                        "should not have happened", e);
            }
        }
    }

    /**
     * @deprecated As of release 4.9, use DroiRewardedVideos#setRewardedVideoListener instead
     */
    @Deprecated
    public static void setRewardedVideoListener(@Nullable Object listener) {
        try {
            Class mDroiRewardedVideoListenerClass = Class.forName(
                    DROI_REWARDED_VIDEO_LISTENER);
            new Reflection.MethodBuilder(null, "setRewardedVideoListener")
                    .setStatic(Class.forName(DROI_REWARDED_VIDEOS))
                    .addParam(mDroiRewardedVideoListenerClass, listener)
                    .execute();
        } catch (ClassNotFoundException e) {
            DroiLog.w("setRewardedVideoListener was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            DroiLog.w("setRewardedVideoListener was called without the rewarded video module");
        } catch (Exception e) {
            DroiLog.e("Error while setting rewarded video listener", e);
        }
    }

    /**
     * @deprecated As of release 4.9, use DroiRewardedVideos#loadRewardedVideo instead
     */
    @Deprecated
    public static void loadRewardedVideo(@NonNull String adUnitId,
            @Nullable MediationSettings... mediationSettings) {
        Droi.loadRewardedVideo(adUnitId, null, mediationSettings);
    }

    /**
     * @deprecated As of release 4.9, use DroiRewardedVideos#loadRewardedVideo instead
     */
    @Deprecated
    public static void loadRewardedVideo(@NonNull String adUnitId,
            @Nullable Object requestParameters,
            @Nullable MediationSettings... mediationSettings) {
        try {
            Class requestParametersClass = Class.forName(
                    DROI_REWARDED_VIDEO_MANAGER_REQUEST_PARAMETERS);
            new Reflection.MethodBuilder(null, "loadRewardedVideo")
                    .setStatic(Class.forName(DROI_REWARDED_VIDEOS))
                    .addParam(String.class, adUnitId)
                    .addParam(requestParametersClass, requestParameters)
                    .addParam(MediationSettings[].class, mediationSettings)
                    .execute();
        } catch (ClassNotFoundException e) {
            DroiLog.w("loadRewardedVideo was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            DroiLog.w("loadRewardedVideo was called without the rewarded video module");
        } catch (Exception e) {
            DroiLog.e("Error while loading rewarded video", e);
        }
    }

    /**
     * @deprecated As of release 4.9, use DroiRewardedVideos#hasRewardedVideo instead
     */
    @Deprecated
    public static boolean hasRewardedVideo(@NonNull String adUnitId) {
        try {
            return (boolean) new Reflection.MethodBuilder(null, "hasRewardedVideo")
                    .setStatic(Class.forName(DROI_REWARDED_VIDEOS))
                    .addParam(String.class, adUnitId)
                    .execute();
        } catch (ClassNotFoundException e) {
            DroiLog.w("hasRewardedVideo was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            DroiLog.w("hasRewardedVideo was called without the rewarded video module");
        } catch (Exception e) {
            DroiLog.e("Error while checking rewarded video", e);
        }
        return false;
    }

    /**
     * @deprecated As of release 4.9, use DroiRewardedVideos#showRewardedVideo instead
     */
    @Deprecated
    public static void showRewardedVideo(@NonNull String adUnitId) {
        try {
            new Reflection.MethodBuilder(null, "showRewardedVideo")
                    .setStatic(Class.forName(DROI_REWARDED_VIDEOS))
                    .addParam(String.class, adUnitId)
                    .execute();
        } catch (ClassNotFoundException e) {
            DroiLog.w("showRewardedVideo was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            DroiLog.w("showRewardedVideo was called without the rewarded video module");
        } catch (Exception e) {
            DroiLog.e("Error while showing rewarded video", e);
        }
    }
}
