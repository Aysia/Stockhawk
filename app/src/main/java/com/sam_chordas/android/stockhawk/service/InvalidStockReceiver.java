package com.sam_chordas.android.stockhawk.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;

/**
 * Creates a toast when stock symbol is invalid
 *
 */
public class InvalidStockReceiver extends BroadcastReceiver {
    public InvalidStockReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, R.string.string_stock_invalid, Toast.LENGTH_LONG).show();
    }
}
