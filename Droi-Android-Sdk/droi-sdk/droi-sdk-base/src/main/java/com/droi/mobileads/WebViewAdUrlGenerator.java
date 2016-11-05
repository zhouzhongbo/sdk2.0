package com.droi.mobileads;

import android.content.Context;

import com.droi.common.AdUrlGenerator;
import com.droi.common.ClientMetadata;
import com.droi.common.Constants;

public class WebViewAdUrlGenerator extends AdUrlGenerator {
    private final boolean mIsStorePictureSupported;

    public WebViewAdUrlGenerator(Context context, boolean isStorePictureSupported) {
        super(context);
        mIsStorePictureSupported = isStorePictureSupported;
    }

    @Override
    public String generateUrlString(String serverHostname) {
        initUrlString(serverHostname, Constants.AD_HANDLER);

        setApiVersion("6");

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);
        addBaseParams(clientMetadata);

        setMraidFlag(true);

        setExternalStoragePermission(mIsStorePictureSupported);

        return getFinalUrlString();
    }
}
