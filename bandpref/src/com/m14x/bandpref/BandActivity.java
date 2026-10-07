package com.m14x.bandpref;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.Toast;

import java.util.List;

public class BandActivity extends Activity {
    private int selectedSlot = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickSlot();
    }

    private void pickSlot() {
        SubscriptionManager sm = getSystemService(SubscriptionManager.class);
        List<SubscriptionInfo> subs = (sm != null) ? sm.getActiveSubscriptionInfoList() : null;

        String[] slots = new String[]{"SIM 1", "SIM 2"};
        if (subs != null) {
            for (SubscriptionInfo si : subs) {
                int idx = si.getSimSlotIndex();
                if (idx >= 0 && idx < 2) {
                    slots[idx] = "SIM " + (idx + 1) + " (" + si.getDisplayName() + ")";
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select SIM")
                .setItems(slots, (dialog, which) -> {
                    selectedSlot = which;
                    pickBandForSlot(slots[which]);
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void pickBandForSlot(String slotTitle) {
        List<BandLock.BandItem> items = BandLock.buildItems();
        String current = BandLock.saved(this, selectedSlot);

        String[] labels = new String[items.size()];
        int checked = 0;
        for (int i = 0; i < items.size(); i++) {
            BandLock.BandItem item = items.get(i);
            labels[i] = item.label;
            if (item.value.equals(current)) {
                checked = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(slotTitle + " Band")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    String value = items.get(which).value;
                    try {
                        BandLock.save(this, selectedSlot, value);
                        BandLock.applyForSlot(this, selectedSlot, value);
                        Toast.makeText(this, slotTitle + ": " + labels[which], Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Band lock failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    dialog.dismiss();
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }
}
