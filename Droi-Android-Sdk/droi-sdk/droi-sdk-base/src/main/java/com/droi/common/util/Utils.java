package com.droi.common.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.droi.common.logging.DroiLog;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {
    private static final AtomicLong sNextGeneratedId = new AtomicLong(1);

    public static String sha1(String string) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = string.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();

            for (final byte b : bytes) {
                stringBuilder.append(String.format("%02X", b));
            }

            return stringBuilder.toString().toLowerCase(Locale.US);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Adaptation of View.generateViewId() from API 17.
     * There is only a guarantee of ID uniqueness within a given session. Please do not store these
     * values between sessions.
     */
    public static long generateUniqueId() {
        for (;;) {
            final long result = sNextGeneratedId.get();
            long newValue = result + 1;
            if (newValue > Long.MAX_VALUE - 1) {
                newValue = 1;
            }
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static boolean bitMaskContainsFlag(final int bitMask, final int flag) {
        return (bitMask & flag) != 0;
    }

    public static String getMD5(String info)
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(info.getBytes("UTF-8"));
            byte[] encryption = md5.digest();

            StringBuffer strBuf = new StringBuffer();
            for (int i = 0; i < encryption.length; i++)
            {
                if (Integer.toHexString(0xff & encryption[i]).length() == 1)
                {
                    strBuf.append("0").append(Integer.toHexString(0xff & encryption[i]));
                }
                else
                {
                    strBuf.append(Integer.toHexString(0xff & encryption[i]));
                }
            }
            return strBuf.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            return "";
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public static String getKeyValue(Context context, String mkey) {
        String keyvalue = "";
        try {
            PackageManager manager = context.getPackageManager();
            ApplicationInfo info = manager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

            if ((info != null) && (info.metaData != null)) {
                Object idObject = info.metaData.get(mkey);
                if (idObject != null) {
                    String id = idObject.toString();
                    if (id != null)
                        keyvalue = id;
                    else
                        DroiLog.d(new StringBuilder().append("Could not read ").append(mkey).append(" meta-data from AndroidManifest.xml.").toString());
                }
            }
        } catch (Exception e) {
            DroiLog.e(new StringBuilder().append("Could not read ").append(mkey).append(" meta-data from AndroidManifest.xml.").toString());
            e.printStackTrace();
        }
        return keyvalue;
    }
}
