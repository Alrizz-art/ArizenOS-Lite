package com.arizen.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * ArizenOS Lite — Boot Receiver
 * Initializes Arizen services on device boot
 */
public class ArizenBootReceiver extends BroadcastReceiver {
    private static final String TAG = "ArizenBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        Log.i(TAG, "ArizenOS Lite boot sequence initiated");
        // Start Arizen ambient service
        Intent serviceIntent = new Intent(context, ArizenAmbientService.class);
        context.startService(serviceIntent);
    }
}
