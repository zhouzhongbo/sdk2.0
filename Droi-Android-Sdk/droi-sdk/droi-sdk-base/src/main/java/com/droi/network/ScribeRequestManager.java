package com.droi.network;

import android.os.Looper;
import android.support.annotation.NonNull;

import com.droi.common.logging.DroiLog;
import com.droi.volley.Request;
import com.droi.volley.VolleyError;

import static com.droi.network.ScribeRequest.ScribeRequestFactory;

/**
 * Request manager to manage scribe requests. This class implements the specific Scribe
 * request listener.
 */
public class ScribeRequestManager extends RequestManager<ScribeRequestFactory> implements ScribeRequest.Listener {

    public ScribeRequestManager(final Looper looper) {
        super(looper);
    }

    // RequestManager
    @NonNull
    @Override
    Request<?> createRequest() {
        return mRequestFactory.createRequest(this);
    }

    // ScribeRequest.Listener
    @Override
    public void onResponse() {
        DroiLog.d("Successfully scribed events");
        // Get back to the dedicated event logging thread before touching shared resources
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                clearRequest();
            }
        });
    }

    @Override
    public void onErrorResponse(final VolleyError volleyError) {
        // Post back to the dedicated event logging thread before touching shared resources
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mBackoffPolicy.backoff(volleyError);
                    makeRequestInternal();
                } catch (VolleyError e) {
                    DroiLog.d("Failed to Scribe events: " + volleyError);
                    clearRequest();
                }
            }
        });
    }
}

