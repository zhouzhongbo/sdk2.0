package com.droi.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;

import com.droi.common.BaseUrlGenerator;
import com.droi.common.ClientMetadata;
import com.droi.common.Constants;

class PositioningUrlGenerator extends BaseUrlGenerator {
    private static final String POSITIONING_API_VERSION = "1";

    @NonNull private final Context mContext;
    @NonNull private String mAdUnitId;

    public PositioningUrlGenerator(@NonNull Context context) {
        mContext = context;
    }

    @NonNull
    public PositioningUrlGenerator withAdUnitId(@NonNull final String adUnitId) {
        mAdUnitId = adUnitId;
        return this;
    }

    @Override
    public String generateUrlString(@NonNull final String serverHostname) {
        initUrlString(serverHostname, Constants.POSITIONING_HANDLER);

        setAdUnitId(mAdUnitId);

        setApiVersion(POSITIONING_API_VERSION);

        ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);

        setSdkVersion(clientMetadata.getSdkVersion());

        setDeviceInfo(clientMetadata.getDeviceManufacturer(),
                clientMetadata.getDeviceModel(),
                clientMetadata.getDeviceProduct());

        setAppVersion(clientMetadata.getAppVersion());

        appendAdvertisingInfoTemplates();

        return getFinalUrlString();
    }

    private void setAdUnitId(@NonNull String adUnitId) {
        addParam("id", adUnitId);
    }

    private void setSdkVersion(@NonNull String sdkVersion) {
        addParam("nsv", sdkVersion);
    }
}
