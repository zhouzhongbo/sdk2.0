package com.droi.network;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.droi.common.Preconditions;
import com.droi.common.VisibleForTesting;
import com.droi.common.logging.DroiLog;
import com.droi.volley.Request;

/**
 * This class is responsible for managing the lifecycle of a request with a backoff policy. This
 * class currently manages a single request at a time. The API allows for it to support multiple
 * simultaneous requests in the future.
 *
 * Subclasses are responsible for implementing the createRequest method that will create a new
 * instance of subclass's specific request type.
 * The subclass is also responsible for listening to success and error responses from its specific
 * request type.
 *
 * @param <T> The type of request factory to generate new requests for each retry.
 */
public abstract class RequestManager<T extends RequestManager.RequestFactory> {

    // This interface is used to bound type T of the RequestManager
    public interface RequestFactory{}

    @Nullable protected Request<?> mCurrentRequest;
    @Nullable protected T mRequestFactory;
    @Nullable protected BackoffPolicy mBackoffPolicy;
    @NonNull protected Handler mHandler;

    public RequestManager(@NonNull Looper looper) {
        mHandler = new Handler(looper);
    }

    @NonNull
    abstract Request<?> createRequest();

    public boolean isAtCapacity() {
        return mCurrentRequest != null;
    }

    /**
     * This method first cancels existing requests in flight and then begins the request
     * lifecycle for the new request.
     *
     * @param requestFactory Factory that constructs a new request for each request retry from the
     *                       backoff policy.
     * @param backoffPolicy The request to cancel.
     */
    public void makeRequest(@NonNull T requestFactory, @NonNull BackoffPolicy backoffPolicy) {
        Preconditions.checkNotNull(requestFactory);
        Preconditions.checkNotNull(backoffPolicy);

        cancelRequest();
        mRequestFactory = requestFactory;
        mBackoffPolicy = backoffPolicy;
        makeRequestInternal();
    }

    /**
     * Cancels the request in flight.
     */
    public void cancelRequest() {
        DroiRequestQueue requestQueue = Networking.getRequestQueue();
        if (requestQueue != null && mCurrentRequest != null) {
            requestQueue.cancel(mCurrentRequest);
        }
        clearRequest();
    }

    @VisibleForTesting
    void makeRequestInternal() {
        mCurrentRequest = createRequest();
        DroiRequestQueue requestQueue = Networking.getRequestQueue();
        if (requestQueue == null) {
            DroiLog.d("DroiRequest queue is null. Clearing request.");
            clearRequest();
            return;
        }

        if (mBackoffPolicy.getRetryCount() == 0) {
            requestQueue.add(mCurrentRequest);
        } else {
            requestQueue.addDelayedRequest(mCurrentRequest, mBackoffPolicy.getBackoffMs());
        }
    }

    @VisibleForTesting
    void clearRequest() {
        mCurrentRequest = null;
        mRequestFactory = null;
        mBackoffPolicy = null;
    }

    @Deprecated
    @VisibleForTesting
    Request<?> getCurrentRequest() {
        return mCurrentRequest;
    }
}
