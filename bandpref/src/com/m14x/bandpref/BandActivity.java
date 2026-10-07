package com.m14x.bandpref;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

public class BandActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String current = BandLock.saved(this);
        String[] items = new String[BandLock.BANDS.length + 1];
        items[0] = "Auto";
        int checked = 0;
        for (int i = 0; i < BandLock.BANDS.length; i++) {
            items[i + 1] = "LTE B" + BandLock.BANDS[i];
            if (Integer.toString(BandLock.BANDS[i]).equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle("LTE band")
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    String value = which == 0 ? "auto" : Integer.toString(BandLock.BANDS[which - 1]);
                    try {
                        BandLock.save(this, value);
                        BandLock.apply(this, value);
                        Toast.makeText(this, which == 0 ? "LTE auto" : items[which], Toast.LENGTH_SHORT).show();
                    } catch (RuntimeException e) {
                        Toast.makeText(this, "Band change failed", Toast.LENGTH_LONG).show();
                    }
                    dialog.dismiss();
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }
}
