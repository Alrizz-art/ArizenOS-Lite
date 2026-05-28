package com.arizen.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;

/**
 * ArizenOS Lite — Wake Word Service
 *
 * Foreground service that continuously listens for "Hey Arizen" (and variants).
 * Uses Android SpeechRecognizer in a loop — no third-party libs, zero extra deps.
 *
 * Cycle: [LISTEN 4s] → [PAUSE 1s] → [LISTEN 4s] → ...
 * On wake word detection → launch ArizenAssistantActivity in voice mode.
 * Pauses automatically when screen is off to save battery.
 */
public class ArizenWakeService extends Service {

    private static final String TAG        = "ArizenWake";
    private static final String CHANNEL_ID = "arizen_wake";
    public  static final String ACTION_WAKE_DETECTED = "com.arizen.launcher.WAKE_DETECTED";

    // Wake word variants (Indonesian + English)
    private static final String[] WAKE_WORDS = {
        "hey arizen", "hei arizen", "hai arizen",
        "arizen", "ey arizen", "arizen tolong",
        "arizen bantu", "wake arizen", "ok arizen",
        "oke arizen"
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean running = false;
    private boolean screenOn = true;
    private int listenCycles = 0;
    private WakeState state = WakeState.IDLE;

    private enum WakeState { IDLE, LISTENING, PAUSED }

    // ── Screen on/off receiver ─────────────────────────────────────────────
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                screenOn = false;
                stopRecognizer();
                Log.d(TAG, "Screen off — pausing wake listener");
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())
                    || Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                screenOn = true;
                if (running) {
                    Log.d(TAG, "Screen on — resuming wake listener");
                    mainHandler.postDelayed(ArizenWakeService.this::startListenCycle, 500);
                }
            }
        }
    };

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification("Mendengarkan…", false));

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);

        Log.i(TAG, "ArizenWake service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            // Must start listening on main thread (SpeechRecognizer requirement)
            mainHandler.post(this::startListenCycle);
            Log.i(TAG, "Wake word listening started");
        }
        return START_STICKY; // Auto-restart if killed
    }

    @Override
    public void onDestroy() {
        running = false;
        stopRecognizer();
        mainHandler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(screenReceiver); } catch (Exception e) { /* ignore */ }
        stopForeground(true);
        Log.i(TAG, "Wake service destroyed");
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    // ── Listen loop ────────────────────────────────────────────────────────
    private void startListenCycle() {
        if (!running || !screenOn) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "SpeechRecognizer not available — retrying in 10s");
            mainHandler.postDelayed(this::startListenCycle, 10000);
            return;
        }

        stopRecognizer();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                state = WakeState.LISTENING;
                listenCycles++;
                // Update notification every 10 cycles to avoid spam
                if (listenCycles % 10 == 0) {
                    updateNotification("Mendengarkan… (siklus " + listenCycles + ")", false);
                }
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rms) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String phrase : matches) {
                        if (isWakeWord(phrase)) {
                            onWakeDetected(phrase);
                            return;
                        }
                    }
                }
                // Not a wake word — schedule next cycle after a short pause
                scheduleNextCycle(800);
            }
            @Override public void onError(int error) {
                // Most errors are normal (timeout, no speech) — just restart
                int delayMs;
                switch (error) {
                    case SpeechRecognizer.ERROR_NETWORK:
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        delayMs = 5000; break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        delayMs = 2000; break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        delayMs = 500; break;
                    default:
                        delayMs = 1500;
                }
                scheduleNextCycle(delayMs);
            }
            @Override public void onPartialResults(Bundle b) {
                // Check partial results too — faster detection
                ArrayList<String> partial =
                    b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null) {
                    for (String phrase : partial) {
                        if (isWakeWord(phrase)) {
                            onWakeDetected(phrase + " (partial)");
                            return;
                        }
                    }
                }
            }
            @Override public void onEvent(int t, Bundle p) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_MATCH_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        // Short listen window to save battery (3 seconds max)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L);

        try {
            recognizer.startListening(intent);
            state = WakeState.LISTENING;
        } catch (Exception e) {
            Log.e(TAG, "startListening error: " + e.getMessage());
            scheduleNextCycle(2000);
        }
    }

    private void scheduleNextCycle(long delayMs) {
        state = WakeState.PAUSED;
        if (running && screenOn) {
            mainHandler.postDelayed(this::startListenCycle, delayMs);
        }
    }

    private void stopRecognizer() {
        if (recognizer != null) {
            try {
                recognizer.stopListening();
                recognizer.destroy();
            } catch (Exception e) { /* ignore */ }
            recognizer = null;
        }
        state = WakeState.IDLE;
    }

    // ── Wake word detection ────────────────────────────────────────────────
    private boolean isWakeWord(String phrase) {
        if (phrase == null) return false;
        String lower = phrase.toLowerCase().trim();
        for (String kw : WAKE_WORDS) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private void onWakeDetected(String phrase) {
        Log.i(TAG, "WAKE DETECTED: " + phrase);
        stopRecognizer();

        // Flash notification
        updateNotification("Wake detected: " + phrase, true);

        // Small vibration feedback
        android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(150);

        // Broadcast for any listeners
        Intent broadcast = new Intent(ACTION_WAKE_DETECTED);
        broadcast.putExtra("phrase", phrase);
        sendBroadcast(broadcast);

        // Launch ArizenAssistant in voice-ready mode
        Intent launch = new Intent(this, ArizenAssistantActivity.class);
        launch.putExtra("voice_mode", true);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                      | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(launch);

        // Resume listening after user is done (8 second delay)
        mainHandler.postDelayed(() -> {
            if (running) {
                updateNotification("Mendengarkan…", false);
                startListenCycle();
            }
        }, 8000);
    }

    // ── Notification ───────────────────────────────────────────────────────
    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID, "Arizen Wake Word",
            NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Mendeteksi 'Hey Arizen'");
        ch.setShowBadge(false);
        ch.enableLights(false);
        ch.enableVibration(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    private Notification buildNotification(String status, boolean alert) {
        Intent openIntent = new Intent(this, ArizenAssistantActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Arizen" + (alert ? " — MENDENGARKAN" : ""))
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build();
    }

    private void updateNotification(String status, boolean alert) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(1, buildNotification(status, alert));
    }
}
