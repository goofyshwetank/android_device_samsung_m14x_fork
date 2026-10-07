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
        showMainMenu();
    }

    private void showMainMenu() {
        SubscriptionManager sm = getSystemService(SubscriptionManager.class);
        List<SubscriptionInfo> subs = (sm != null) ? sm.getActiveSubscriptionInfoList() : null;

        String name1 = "SIM 1";
        String name2 = "SIM 2";
        if (subs != null) {
            for (SubscriptionInfo si : subs) {
                int idx = si.getSimSlotIndex();
                if (idx == 0) name1 = "SIM 1 (" + si.getDisplayName() + ")";
                else if (idx == 1) name2 = "SIM 2 (" + si.getDisplayName() + ")";
            }
        }
        final String finalSim1 = name1;
        final String finalSim2 = name2;

        final boolean gameMode = BandLock.isTouchGameMode(this);
        final boolean gloveMode = BandLock.isTouchGloveMode(this);

        String[] menuItems = new String[]{
                finalSim1 + " [Band Lock]",
                finalSim2 + " [Band Lock]",
                "Touch Polling Rate: " + (gameMode ? "MAX (Game Mode ON)" : "Standard (OFF)"),
                "High Touch Sensitivity: " + (gloveMode ? "ON (Glove Mode)" : "Standard (OFF)")
        };

        new AlertDialog.Builder(this)
                .setTitle("Hardware Controls")
                .setItems(menuItems, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            selectedSlot = 0;
                            pickBandForSlot(finalSim1);
                            break;
                        case 1:
                            selectedSlot = 1;
                            pickBandForSlot(finalSim2);
                            break;
                        case 2:
                            toggleGameMode(!gameMode);
                            break;
                        case 3:
                            toggleGloveMode(!gloveMode);
                            break;
                    }
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void toggleGameMode(boolean enable) {
        BandLock.setTouchGameMode(this, enable);
        Toast.makeText(this, "Touch Polling Rate: " + (enable ? "MAX (Game Mode 240Hz+)" : "Standard"), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void toggleGloveMode(boolean enable) {
        BandLock.setTouchGloveMode(this, enable);
        Toast.makeText(this, "Touch Sensitivity: " + (enable ? "High (Glove Mode)" : "Standard"), Toast.LENGTH_SHORT).show();
        finish();
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
