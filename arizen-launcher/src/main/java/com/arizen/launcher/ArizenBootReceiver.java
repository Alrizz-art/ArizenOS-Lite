package com.arizen.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * ArizenOS Lite — Boot Receiver
 * Auto-starts ArizenWake service on boot if wake word is enabled in settings.
 */
public class ArizenBootReceiver extends BroadcastReceiver {
    private static final String TAG = "ArizenBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        Log.i(TAG, "Boot completed — checking ArizenOS services");
        ArizenSettings settings = new ArizenSettings(context);

        // Start wake word service if enabled
        if (settings.isWakeWordEnabled()) {
            Log.i(TAG, "Starting ArizenWake service on boot");
            Intent wakeIntent = new Intent(context, ArizenWakeService.class);
            context.startForegroundService(wakeIntent);
        }

        // Start ambient service if enabled
        if (settings.isAmbienceEnabled()) {
            Log.i(TAG, "Starting ArizenAmbient service on boot");
            Intent ambientIntent = new Intent(context, ArizenAmbientService.class);
            context.startService(ambientIntent);
        }
    }
}
