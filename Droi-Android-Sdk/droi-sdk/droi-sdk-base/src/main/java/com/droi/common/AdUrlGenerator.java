package com.droi.common;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.droi.common.util.DateAndTime;

import static com.droi.common.ClientMetadata.DroiNetworkType;

public abstract class AdUrlGenerator extends BaseUrlGenerator {

    /**
     * The ad unit id which identifies a spot for an ad to be placed.
     */
    private static final String AD_UNIT_ID_KEY = "adslotid";

    /**
     * nv = native version. This is the version of Droi.
     */
    private static final String SDK_VERSION_KEY = "nv";

    /**
     * q = query. This is for big publishers to send up certain
     * keywords that better match ads.
     */
    private static final String KEYWORDS_KEY = "q";

    /**
     * Location represented in latitude and longitude.
     * e.g. "47.638,-122.321"
     */
    private static final String LAT_LONG_KEY = "ll";

    /**
     * Estimated accuracy of this location, in meters.
     * See {@link android.location.Location#getAccuracy()}
     * for more information.
     */
    private static final String LAT_LONG_ACCURACY_KEY = "lla";

    /**
     * Milliseconds since location was updated.
     */
    private static final String LAT_LONG_FRESHNESS_KEY = "llf";

    /**
     * Whether or not the location came from the Droi SDK
     * and not the developer. 1 = from Droi.
     */
    private static final String LAT_LONG_FROM_SDK_KEY = "llsdk";

    /**
     * Timezone offset. e.g. Pacific Standard Time = -0800.
     */
    private static final String TIMEZONE_OFFSET_KEY = "z";

    /**
     * "p" for portrait, "l" for landscape
     */
    private static final String ORIENTATION_KEY = "o";

    /**
     * Density as represented by a float. See
     * https://developer.android.com/guide/practices/screens_support.html
     * for details on values this can be.
     */
    private static final String SCREEN_SCALE_KEY = "sc_a";

    /**
     * Whether or not this is using mraid. 1 = yes.
     */
    private static final String IS_MRAID_KEY = "mr";

    /**
     * mcc, the mobile country code, paired with the mobile network code,
     * uniquely identifies a carrier in a country.
     */
    private static final String MOBILE_COUNTRY_CODE_KEY = "mcc";
    private static final String MOBILE_NETWORK_CODE_KEY = "mnc";

    /**
     * The International Organization for Standardization's 2-character country code
     */
    private static final String COUNTRY_CODE_KEY = "iso";

    /**
     * String name of the carrier. e.g. "Verizon%20Wireless"
     */
    private static final String CARRIER_NAME_KEY = "cn";

    /**
     * Carrier type as in what kind of network this device is on.
     * See {@link android.net.ConnectivityManager} for constants.
     */
    private static final String CARRIER_TYPE_KEY = "ct";

    /**
     * Bundle ID, as in package name.
     */
    private static final String BUNDLE_ID_KEY = "bundle";

    //add for adcontrol start
    /**
     *IMEI setting
     */
    private static final String IMEI_KEY = "imei";

    /**
     *IMSI setting
     */
    private static final String IMSI_KEY = "imsi";

    /**
     *mac setting
     */
    private static final String MAC_KEY = "mac";

    /**
     *mac1 setting
     */
    private static final String MAC1_KEY = "mac1";

    /**
     *AndroidID setting
     */
    private static final String ANDROID_ID_KEY = "androidid";

    /**
     *APPID setting
     */
    private static final String APPID_KEY = "appid";

    /**
     *CHANNEL setting
     */
    private static final String CHANNEL_ID_KEY = "channel";

    /**
     *CUSTOMER setting
     */
    private static final String CUSTOMER_KEY = "customer";

    /**
     *PROJECT setting
     */
    private static final String PROJECT_KEY = "project";

    /**
     *BRAND setting
     */
    private static final String BRAND_KEY = "brand";

    /**
     *HARDWARE setting
     */
    private static final String HARDWARE_KEY = "hardware";

    /**
     *OSVERSION setting
     */
    private static final String OSVERSION_KEY = "osVersion";

    /**
     *OS TYPEsetting
     */
    private static final String OS_TYPE_KEY = "os";
    //add for adcontrol end

    protected Context mContext;
    protected String mAdUnitId;
    protected String mKeywords;
    protected Location mLocation;

    public AdUrlGenerator(Context context) {
        mContext = context;
    }

    public AdUrlGenerator withAdUnitId(String adUnitId) {
        mAdUnitId = adUnitId;
        return this;
    }

    public AdUrlGenerator withKeywords(String keywords) {
        mKeywords = keywords;
        return this;
    }

    public AdUrlGenerator withLocation(Location location) {
        mLocation = location;
        return this;
    }

    protected void setAdUnitId(String adUnitId) {
        addParam(AD_UNIT_ID_KEY, adUnitId);
    }

    protected void setSdkVersion(String sdkVersion) {
        addParam(SDK_VERSION_KEY, sdkVersion);
    }

    protected void setKeywords(String keywords) {
        addParam(KEYWORDS_KEY, keywords);
    }

    protected void setLocation(@Nullable Location location) {
        Location bestLocation = location;
        Location locationFromLocationService = LocationService.getLastKnownLocation(mContext,
                Droi.getLocationPrecision(),
                Droi.getLocationAwareness());

        if (locationFromLocationService != null &&
                (location == null || locationFromLocationService.getTime() >= location.getTime())) {
            bestLocation = locationFromLocationService;
        }

        if (bestLocation != null) {
            addParam(LAT_LONG_KEY, bestLocation.getLatitude() + "," + bestLocation.getLongitude());
            addParam(LAT_LONG_ACCURACY_KEY, String.valueOf((int) bestLocation.getAccuracy()));
            addParam(LAT_LONG_FRESHNESS_KEY,
                    String.valueOf(calculateLocationStalenessInMilliseconds(bestLocation)));

            if (bestLocation == locationFromLocationService) {
                addParam(LAT_LONG_FROM_SDK_KEY, "1");
            }
        }
    }

    protected void setTimezone(String timeZoneOffsetString) {
        addParam(TIMEZONE_OFFSET_KEY, timeZoneOffsetString);
    }

    protected void setOrientation(String orientation) {
        addParam(ORIENTATION_KEY, orientation);
    }

    protected void setDensity(float density) {
        addParam(SCREEN_SCALE_KEY, "" + density);
    }

    protected void setMraidFlag(boolean mraid) {
        if (mraid) {
            addParam(IS_MRAID_KEY, "1");
        }
    }

    protected void setMccCode(String networkOperator) {
        String mcc = networkOperator == null ? "" : networkOperator.substring(0, mncPortionLength(networkOperator));
        addParam(MOBILE_COUNTRY_CODE_KEY, mcc);
    }

    protected void setMncCode(String networkOperator) {
        String mnc = networkOperator == null ? "" : networkOperator.substring(mncPortionLength(networkOperator));
        addParam(MOBILE_NETWORK_CODE_KEY, mnc);
    }

    protected void setIsoCountryCode(String networkCountryIso) {
        addParam(COUNTRY_CODE_KEY, networkCountryIso);
    }

    protected void setCarrierName(String networkOperatorName) {
        addParam(CARRIER_NAME_KEY, networkOperatorName);
    }

    protected void setNetworkType(DroiNetworkType networkType) {
        addParam(CARRIER_TYPE_KEY, networkType);
    }

    protected void setBundleId(String bundleId) {
        if (!TextUtils.isEmpty(bundleId)) {
            addParam(BUNDLE_ID_KEY, bundleId);
        }
    }

    protected void addBaseParams(final ClientMetadata clientMetadata) {
        setAdUnitId(mAdUnitId);

        setSdkVersion(clientMetadata.getSdkVersion());
        setDeviceInfo(clientMetadata.getDeviceManufacturer(),
                clientMetadata.getDeviceModel(),
                clientMetadata.getDeviceProduct());
        setBundleId(clientMetadata.getAppPackageName());

        setKeywords(mKeywords);

        setLocation(mLocation);

        setTimezone(DateAndTime.getTimeZoneOffsetString());

        setOrientation(clientMetadata.getOrientationString());
        setDeviceDimensions(clientMetadata.getDeviceDimensions());
        setDensity(clientMetadata.getDensity());

        final String networkOperator = clientMetadata.getNetworkOperatorForUrl();
        setMccCode(networkOperator);
        setMncCode(networkOperator);

        setIsoCountryCode(clientMetadata.getIsoCountryCode());
        setCarrierName(clientMetadata.getNetworkOperatorName());

        setNetworkType(clientMetadata.getActiveNetworkType());

        setAppVersion(clientMetadata.getAppVersion());

        appendAdvertisingInfoTemplates();
    }

    protected void setAppID(String appid){
        addParam(APPID_KEY, appid);

    }

    protected void setChannelID(String channel){
        addParam(CHANNEL_ID_KEY, channel);

    }

    protected void setIMEI(String imei){
        addParam(IMEI_KEY, imei);
    }

    protected void setIMSI(String imsi){
        addParam(IMSI_KEY, imsi);
    }

    /**
     * here set 1 mean android os
     */
    protected void setOS(){
        addParam(OS_TYPE_KEY, "1");

    }

    protected void setMAC(String mac){
        addParam(MAC_KEY, mac);

    }

    protected void setMAC1(String mac1){
        addParam(MAC1_KEY, mac1);
    }

    protected void setAndroidID(String androidID){
        addParam(ANDROID_ID_KEY, androidID);
    }

    protected void setCustomer(String customer){
        addParam(CUSTOMER_KEY, customer);
    }

    protected void setProject(String project){
        addParam(PROJECT_KEY, project);
    }


    protected void setBrands(String brand){
        addParam(BRAND_KEY, brand);
    }

    protected void setHardWare(String hardware){
        addParam(HARDWARE_KEY, hardware);
    }

    protected void setReleaseVersion(String version){
        addParam(OSVERSION_KEY,version);
    }

    protected void addBaseParams(final DroiMetadata droiMetadata) {
        setAppID(droiMetadata.getAppID());
        setChannelID(droiMetadata.getChannel());
    }

    private void addParam(String key, DroiNetworkType value) {
        addParam(key, value.toString());
    }

    private int mncPortionLength(String networkOperator) {
        return Math.min(3, networkOperator.length());
    }

    private static int calculateLocationStalenessInMilliseconds(final Location location) {
        Preconditions.checkNotNull(location);
        final long locationLastUpdatedInMillis = location.getTime();
        final long nowInMillis = System.currentTimeMillis();
        return (int) (nowInMillis - locationLastUpdatedInMillis);
    }

    /**
     * @deprecated As of release 2.4
     */
    @Deprecated
    public AdUrlGenerator withFacebookSupported(boolean enabled) {
        return this;
    }
}
