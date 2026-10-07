package com.m14x.bandpref;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            BandLock.applyAll(context);
        } catch (Throwable ignored) {
        }
    }
}
