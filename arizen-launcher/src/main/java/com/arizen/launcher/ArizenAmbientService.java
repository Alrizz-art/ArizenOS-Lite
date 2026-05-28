package com.arizen.launcher;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * ArizenOS Lite — Ambient Service
 * Lightweight background service for contextual AI suggestions
 */
public class ArizenAmbientService extends Service {
    private static final String TAG = "ArizenAmbient";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Arizen Ambient Service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Lightweight — no heavy processing here
        // Polls for contextual suggestions only when screen is on
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
