package com.m14x.bandpref;

import android.content.Context;
import android.provider.Settings;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.RadioAccessSpecifier;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BandLock {
    private static final String TAG = "BandLock";
    public static final String KEY_PREFIX = "m14x_band_slot_"; // + slotIndex
    public static final String KEY_TOUCH_GAME_MODE = "m14x_touch_game_mode";
    public static final String KEY_TOUCH_GLOVE_MODE = "m14x_touch_glove_mode";
    private static final String TSP_CMD_NODE = "/sys/class/sec/tsp/cmd";

    public static final class BandItem {
        public final String label;
        public final String value; // "auto", "lte:X", "nr:Y"
        public final int ran;      // AccessNetworkType
        public final int bandNumber;

        public BandItem(String label, String value, int ran, int bandNumber) {
            this.label = label;
            this.value = value;
            this.ran = ran;
            this.bandNumber = bandNumber;
        }
    }

    // Samsung F14 5G (SM-E146B / s5e8535) supported bands
    public static final int[] LTE_BANDS = {1, 3, 5, 8, 40, 41};
    public static final int[] NR_BANDS = {1, 3, 5, 8, 28, 40, 41, 77, 78};

    public static List<BandItem> buildItems() {
        List<BandItem> list = new ArrayList<>();
        list.add(new BandItem("Auto (all bands)", "auto", 0, 0));
        for (int b : LTE_BANDS) {
            list.add(new BandItem("4G LTE B" + b, "lte:" + b, AccessNetworkType.EUTRAN, b));
        }
        for (int b : NR_BANDS) {
            list.add(new BandItem("5G NR n" + b, "nr:" + b, AccessNetworkType.NGRAN, b));
        }
        return list;
    }

    private BandLock() {}

    public static String saved(Context ctx, int slotIndex) {
        String val = Settings.Global.getString(ctx.getContentResolver(), KEY_PREFIX + slotIndex);
        return (val == null || val.isEmpty()) ? "auto" : val;
    }

    public static void save(Context ctx, int slotIndex, String value) {
        Settings.Global.putString(ctx.getContentResolver(), KEY_PREFIX + slotIndex, value);
    }

    public static boolean isTouchGameMode(Context ctx) {
        return Settings.Global.getInt(ctx.getContentResolver(), KEY_TOUCH_GAME_MODE, 0) == 1;
    }

    public static void setTouchGameMode(Context ctx, boolean enabled) {
        Settings.Global.putInt(ctx.getContentResolver(), KEY_TOUCH_GAME_MODE, enabled ? 1 : 0);
        writeTspCmd("set_game_mode," + (enabled ? "1" : "0"));
    }

    public static boolean isTouchGloveMode(Context ctx) {
        return Settings.Global.getInt(ctx.getContentResolver(), KEY_TOUCH_GLOVE_MODE, 0) == 1;
    }

    public static void setTouchGloveMode(Context ctx, boolean enabled) {
        Settings.Global.putInt(ctx.getContentResolver(), KEY_TOUCH_GLOVE_MODE, enabled ? 1 : 0);
        writeTspCmd("glove_mode," + (enabled ? "1" : "0"));
    }

    private static void writeTspCmd(String command) {
        try (FileOutputStream fos = new FileOutputStream(TSP_CMD_NODE)) {
            fos.write(command.getBytes(StandardCharsets.UTF_8));
            Log.i(TAG, "Sent TSP cmd: " + command);
        } catch (Exception e) {
            Log.w(TAG, "Failed writing to TSP cmd: " + command, e);
        }
    }

    public static void applyForSlot(Context ctx, int slotIndex, String value) {
        SubscriptionManager sm = ctx.getSystemService(SubscriptionManager.class);
        TelephonyManager tm = ctx.getSystemService(TelephonyManager.class);
        if (sm == null || tm == null) return;

        SubscriptionInfo subInfo = sm.getActiveSubscriptionInfoForSimSlotIndex(slotIndex);
        if (subInfo == null) {
            Log.w(TAG, "No active sub for slot " + slotIndex);
            return;
        }

        List<RadioAccessSpecifier> specifiers;
        if (value == null || "auto".equals(value)) {
            specifiers = Collections.emptyList();
        } else if (value.startsWith("lte:")) {
            int band = Integer.parseInt(value.substring(4));
            specifiers = Collections.singletonList(new RadioAccessSpecifier(
                    AccessNetworkType.EUTRAN,
                    new int[]{band},
                    null));
        } else if (value.startsWith("nr:")) {
            int band = Integer.parseInt(value.substring(3));
            specifiers = Collections.singletonList(new RadioAccessSpecifier(
                    AccessNetworkType.NGRAN,
                    new int[]{band},
                    null));
        } else {
            specifiers = Collections.emptyList();
        }

        Log.i(TAG, "Applying slot " + slotIndex + " sub " + subInfo.getSubscriptionId() + " -> " + value);
        tm.createForSubscriptionId(subInfo.getSubscriptionId())
                .setSystemSelectionChannels(specifiers);
    }

    public static void applyAll(Context ctx) {
        for (int slot = 0; slot < 2; slot++) {
            try {
                applyForSlot(ctx, slot, saved(ctx, slot));
            } catch (Exception e) {
                Log.w(TAG, "Failed applying slot " + slot, e);
            }
        }
        if (isTouchGameMode(ctx)) {
            writeTspCmd("set_game_mode,1");
        }
        if (isTouchGloveMode(ctx)) {
            writeTspCmd("glove_mode,1");
        }
    }
}
