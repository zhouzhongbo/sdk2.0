package com.droi.nativeads;

import android.support.annotation.NonNull;

import com.droi.common.VisibleForTesting;
import com.droi.network.DroiNetworkError;
import com.droi.volley.NetworkResponse;
import com.droi.volley.Response;
import com.droi.volley.VolleyError;
import com.droi.volley.toolbox.HttpHeaderParser;
import com.droi.volley.toolbox.JsonRequest;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import static com.droi.nativeads.DroiNativeAdPositioning.DroiClientPositioning;

public class PositioningRequest extends JsonRequest<DroiClientPositioning> {
    private static final String FIXED_KEY = "fixed";
    private static final String SECTION_KEY = "section";
    private static final String POSITION_KEY = "position";
    private static final String REPEATING_KEY = "repeating";
    private static final String INTERVAL_KEY = "interval";

    // Max value to avoid bad integer math calculations. This is 2 ^ 16.
    private static final int MAX_VALUE = 1 << 16;

    public PositioningRequest(final String url,
            final Response.Listener<DroiClientPositioning> listener,
            final Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
    }

    // This is done just for unit testing visibolity.
    @Override
    protected void deliverResponse(final DroiClientPositioning response) {
        super.deliverResponse(response);
    }

    @Override
    protected Response<DroiClientPositioning> parseNetworkResponse(final NetworkResponse response) {
        if (response.statusCode != HttpStatus.SC_OK) {
            return Response.error(new VolleyError(response));
        }

        if (response.data.length == 0) {
            return Response.error(new VolleyError("Empty positioning response", new JSONException("Empty response")));
        }

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));

            return Response.success(parseJson(jsonString), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new VolleyError("Couldn't parse JSON from Charset", e));
        } catch (JSONException e) {
            return Response.error(new VolleyError("JSON Parsing Error", e));
        } catch (DroiNetworkError e) {
            return Response.error(e);
        }
    }

    @NonNull
    @VisibleForTesting
    DroiClientPositioning parseJson(@NonNull String jsonString) throws  JSONException, DroiNetworkError {
        JSONObject jsonObject = new JSONObject(jsonString);

        // If the server returns an error explicitly, throw an exception with the message.
        String error = jsonObject.optString("error", null);
        if (error != null) {
            if (error.equalsIgnoreCase("WARMING_UP")) {
                throw new DroiNetworkError(DroiNetworkError.Reason.WARMING_UP);
            }
            throw new JSONException(error);
        }

        // Parse fixed and repeating rules.
        JSONArray fixed = jsonObject.optJSONArray(FIXED_KEY);
        JSONObject repeating = jsonObject.optJSONObject(REPEATING_KEY);
        if (fixed == null && repeating == null) {
            throw new JSONException("Must contain fixed or repeating positions");
        }

        DroiClientPositioning positioning = new DroiClientPositioning();
        if (fixed != null) {
            parseFixedJson(fixed, positioning);
        }
        if (repeating != null) {
            parseRepeatingJson(repeating, positioning);
        }
        return positioning;
    }

    private void parseFixedJson(@NonNull final JSONArray fixed,
            @NonNull final DroiClientPositioning positioning) throws JSONException {
        for (int i = 0; i < fixed.length(); ++i) {
            JSONObject positionObject = fixed.getJSONObject(i);
            int section = positionObject.optInt(SECTION_KEY, 0);
            if (section < 0) {
                throw new JSONException("Invalid section " + section + " in JSON response");
            }
            if (section > 0) {
                // Ignore sections > 0.
                continue;
            }
            int position = positionObject.getInt(POSITION_KEY);
            if (position < 0 || position > MAX_VALUE) {
                throw new JSONException("Invalid position " + position + " in JSON response");
            }
            positioning.addFixedPosition(position);
        }
    }

    private void parseRepeatingJson(@NonNull final JSONObject repeatingObject,
            @NonNull final DroiClientPositioning positioning) throws JSONException {
        int interval = repeatingObject.getInt(INTERVAL_KEY);
        if (interval < 2 || interval > MAX_VALUE) {
            throw new JSONException("Invalid interval " + interval + " in JSON response");
        }
        positioning.enableRepeatingPositions(interval);
    }
}
