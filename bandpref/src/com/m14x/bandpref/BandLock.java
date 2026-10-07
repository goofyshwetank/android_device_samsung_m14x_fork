package com.m14x.bandpref;

import android.content.Context;
import android.provider.Settings;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.EutranBand;
import android.telephony.RadioAccessSpecifier;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.Collections;
import java.util.List;

/** User LTE band choice. Missing or "auto" scans every band. */
public final class BandLock {
    public static final String KEY = "m14x_lte_band";

    // ponytail: SM-E146B radio bands only. A modem capability query would replace this list.
    public static final int[] BANDS = {
        EutranBand.BAND_1,
        EutranBand.BAND_3,
        EutranBand.BAND_5,
        EutranBand.BAND_8,
        EutranBand.BAND_40,
        EutranBand.BAND_41,
    };

    private BandLock() {}

    public static String saved(Context context) {
        String value = Settings.Global.getString(context.getContentResolver(), KEY);
        return value == null || value.isEmpty() ? "auto" : value;
    }

    public static void save(Context context, String value) {
        Settings.Global.putString(context.getContentResolver(), KEY, value);
    }

    public static void apply(Context context, String value) {
        List<RadioAccessSpecifier> specifiers = Collections.emptyList();
        if (value != null && !"auto".equals(value)) {
            int band = Integer.parseInt(value);
            specifiers = Collections.singletonList(new RadioAccessSpecifier(
                    AccessNetworkConstants.AccessNetworkType.EUTRAN,
                    new int[] {band},
                    null));
        }
        SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
        int[] subs = sm.getActiveSubscriptionIdList();
        if (subs == null || subs.length == 0) {
            context.getSystemService(TelephonyManager.class).setSystemSelectionChannels(specifiers);
            return;
        }
        for (int sub : subs) {
            context.getSystemService(TelephonyManager.class)
                    .createForSubscriptionId(sub)
                    .setSystemSelectionChannels(specifiers);
        }
    }
}
