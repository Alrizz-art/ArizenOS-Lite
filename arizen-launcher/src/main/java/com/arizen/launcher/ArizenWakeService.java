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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * ArizenOS Lite — Wake Word + Routine Trigger Service
 *
 * Listens for:
 *   1. "Hey Arizen" → open ArizenAssistantActivity in voice mode
 *   2. Any registered Routine wake trigger → execute that routine directly
 *
 * Cycle: LISTEN 3s → PAUSE 800ms → LISTEN → ...
 * Auto-pauses when screen is off. Restarts on BOOT_COMPLETED.
 */
public class ArizenWakeService extends Service {

    private static final String TAG        = "ArizenWake";
    private static final String CHANNEL_ID = "arizen_wake";
    public  static final String ACTION_WAKE_DETECTED    = "com.arizen.launcher.WAKE_DETECTED";
    public  static final String ACTION_ROUTINE_TRIGGERED= "com.arizen.launcher.ROUTINE_TRIGGERED";

    private static final String[] WAKE_WORDS = {
        "hey arizen","hei arizen","hai arizen","arizen",
        "ey arizen","arizen tolong","arizen bantu",
        "wake arizen","ok arizen","oke arizen","hi arizen"
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean running   = false;
    private boolean screenOn  = true;
    private int     cycles    = 0;
    private ArizenRoutineManager routineManager;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                screenOn = false;
                stopRecognizer();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)
                    || Intent.ACTION_USER_PRESENT.equals(action)) {
                screenOn = true;
                if (running) mainHandler.postDelayed(ArizenWakeService.this::startListenCycle, 600);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        routineManager = new ArizenRoutineManager(this);
        createNotificationChannel();
        startForeground(1, buildNotification("Mendengarkan wake word…", false));
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, f);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            mainHandler.post(this::startListenCycle);
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        running = false;
        stopRecognizer();
        mainHandler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(screenReceiver); } catch (Exception e) { /* ignore */ }
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    // ── Listen loop ────────────────────────────────────────────────────────
    private void startListenCycle() {
        if (!running || !screenOn) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            mainHandler.postDelayed(this::startListenCycle, 10000); return;
        }
        stopRecognizer();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p)    { cycles++; }
            @Override public void onBeginningOfSpeech()         {}
            @Override public void onRmsChanged(float r)         {}
            @Override public void onBufferReceived(byte[] b)    {}
            @Override public void onEndOfSpeech()               {}
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) processMatches(matches, false);
                else scheduleNext(800);
            }
            @Override public void onError(int error) {
                int delay;
                switch (error) {
                    case SpeechRecognizer.ERROR_NETWORK:
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: delay = 5000; break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: delay = 2000; break;
                    default: delay = 700;
                }
                scheduleNext(delay);
            }
            @Override public void onPartialResults(Bundle b) {
                ArrayList<String> partial =
                    b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null) processMatches(partial, true);
            }
            @Override public void onEvent(int t, Bundle p) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L);
        try { recognizer.startListening(intent); }
        catch (Exception e) { scheduleNext(2000); }
    }

    private void processMatches(List<String> matches, boolean partial) {
        for (String phrase : matches) {
            if (phrase == null) continue;
            String lower = phrase.toLowerCase().trim();

            // 1. Check routine triggers FIRST (more specific)
            ArizenRoutine routine = routineManager.findByTrigger(lower);
            if (routine != null) {
                Log.i(TAG, "Routine trigger: " + phrase + " → " + routine.name);
                onRoutineTriggered(routine, phrase);
                return;
            }

            // 2. Check wake words
            for (String kw : WAKE_WORDS) {
                if (lower.contains(kw)) {
                    Log.i(TAG, "Wake word: " + phrase);
                    onWakeDetected(phrase);
                    return;
                }
            }
        }
        if (!partial) scheduleNext(700);
    }

    private void scheduleNext(long ms) {
        if (running && screenOn) mainHandler.postDelayed(this::startListenCycle, ms);
    }

    private void stopRecognizer() {
        if (recognizer != null) {
            try { recognizer.stopListening(); recognizer.destroy(); }
            catch (Exception e) { /* ignore */ }
            recognizer = null;
        }
    }

    // ── Wake detected → open assistant ────────────────────────────────────
    private void onWakeDetected(String phrase) {
        stopRecognizer();
        updateNotification("Hey Arizen — mendengarkan!", true);
        vibrate(100);

        sendBroadcast(new Intent(ACTION_WAKE_DETECTED).putExtra("phrase", phrase));

        Intent launch = new Intent(this, ArizenAssistantActivity.class);
        launch.putExtra("voice_mode", true);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                      | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(launch);

        mainHandler.postDelayed(() -> {
            if (running) { updateNotification("Mendengarkan wake word…", false); startListenCycle(); }
        }, 9000);
    }

    // ── Routine triggered → execute directly ──────────────────────────────
    private void onRoutineTriggered(ArizenRoutine routine, String phrase) {
        stopRecognizer();
        updateNotification("▶ " + routine.name + "…", true);
        vibrate(80);

        sendBroadcast(new Intent(ACTION_ROUTINE_TRIGGERED)
            .putExtra("routine_id", routine.id).putExtra("phrase", phrase));

        // Execute routine — it runs on background thread internally
        routineManager.execute(routine, new ArizenRoutineManager.ExecutionCallback() {
            @Override public void onStepStart(int i, RoutineStep s) {
                updateNotification("▶ " + s.label + "…", false);
            }
            @Override public void onStepDone(int i, RoutineStep s) {}
            @Override public void onFinished(ArizenRoutine r) {
                updateNotification("✓ " + r.name + " selesai", false);
                mainHandler.postDelayed(() -> {
                    if (running) { updateNotification("Mendengarkan wake word…", false); startListenCycle(); }
                }, 3000);
            }
            @Override public void onError(int i, String msg) {
                Log.e(TAG, "Routine step error: " + msg);
                scheduleNext(2000);
            }
        });
    }

    // ── Notification ───────────────────────────────────────────────────────
    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Arizen Wake Word",
            NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false); ch.enableLights(false); ch.enableVibration(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    private Notification buildNotification(String status, boolean active) {
        Intent i = new Intent(this, ArizenAssistantActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(active ? "Arizen — AKTIF" : "Arizen")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi).setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE).build();
    }

    private void updateNotification(String status, boolean active) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(1, buildNotification(status, active));
    }

    private void vibrate(int ms) {
        android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(ms);
    }
}
