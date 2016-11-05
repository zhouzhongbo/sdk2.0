package com.droi.common;

import android.app.Activity;
import android.support.annotation.NonNull;

/**
 * This empty implementation of {@link com.droi.common.LifecycleListener} is convenient for writing
 * your own adapters for an SDK that Droi can mediate. You can override only the lifecycle callbacks
 * that the SDK requires.
 */
public class BaseLifecycleListener implements LifecycleListener {

    @Override
    public void onCreate(@NonNull final Activity activity) {}

    @Override
    public void onStart(@NonNull final Activity activity) {}

    @Override
    public void onPause(@NonNull final Activity activity) {}

    @Override
    public void onResume(@NonNull final Activity activity) {}

    @Override
    public void onRestart(@NonNull final Activity activity) {}

    @Override
    public void onStop(@NonNull final Activity activity) {}

    @Override
    public void onDestroy(@NonNull final Activity activity) {}

    @Override
    public void onBackPressed(@NonNull final Activity activity) {}
}
