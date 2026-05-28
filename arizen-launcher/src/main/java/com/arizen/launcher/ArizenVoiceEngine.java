package com.arizen.launcher;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.ArrayList;
import java.util.Locale;

/**
 * ArizenOS Lite — Voice Engine
 * SpeechRecognizer (STT) + TextToSpeech (TTS) — 100% Android native, zero deps.
 */
public class ArizenVoiceEngine {

    public enum VoiceState {
        IDLE, LISTENING, PROCESSING, SPEAKING
    }

    public interface VoiceCallback {
        void onStateChange(VoiceState state);
        void onResult(String text);
        void onError(String error);
        void onSpeakDone();
        void onRmsChanged(float rms); // microphone level 0–10
    }

    private final Context context;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private VoiceState currentState = VoiceState.IDLE;
    private VoiceCallback callback;
    private boolean autoSpeak = true;

    public ArizenVoiceEngine(Context ctx, VoiceCallback cb) {
        this.context  = ctx;
        this.callback = cb;
        initTTS();
    }

    // ── TTS init ─────────────────────────────────────────────────────────────
    private void initTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Prefer Indonesian, fall back to English
                int result = tts.setLanguage(new Locale("id", "ID"));
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.ENGLISH);
                }
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.05f);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {
                        setState(VoiceState.SPEAKING);
                    }
                    @Override public void onDone(String id) {
                        setState(VoiceState.IDLE);
                        if (callback != null) callback.onSpeakDone();
                    }
                    @Override public void onError(String id) {
                        setState(VoiceState.IDLE);
                    }
                });
                ttsReady = true;
            }
        });
    }

    // ── STT ──────────────────────────────────────────────────────────────────
    public void startListening() {
        if (currentState == VoiceState.LISTENING) return;
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (callback != null) callback.onError("Speech recognition tidak tersedia di perangkat ini.");
            return;
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p)    { setState(VoiceState.LISTENING); }
            @Override public void onBeginningOfSpeech()         {}
            @Override public void onRmsChanged(float rms) {
                // Normalize 0–10 for visualizer
                float normalized = Math.max(0, Math.min(10, (rms + 2) * 1.2f));
                if (callback != null) callback.onRmsChanged(normalized);
            }
            @Override public void onBufferReceived(byte[] b)    {}
            @Override public void onEndOfSpeech()               { setState(VoiceState.PROCESSING); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    setState(VoiceState.IDLE);
                    if (callback != null) callback.onResult(text);
                } else {
                    setState(VoiceState.IDLE);
                    if (callback != null) callback.onError("Tidak terdengar — coba lagi.");
                }
            }
            @Override public void onError(int error) {
                setState(VoiceState.IDLE);
                if (callback != null) callback.onError(sttErrorMessage(error));
            }
            @Override public void onPartialResults(Bundle p)    {}
            @Override public void onEvent(int t, Bundle p)      {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_MATCH_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara dengan Arizen…");
        recognizer.startListening(intent);
    }

    public void stopListening() {
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
            recognizer = null;
        }
        setState(VoiceState.IDLE);
    }

    // ── TTS ──────────────────────────────────────────────────────────────────
    public void speak(String text) {
        if (!ttsReady || tts == null) return;
        // Strip markdown and tool call syntax for clean TTS
        String clean = text
            .replaceAll("\\[\\w+:[^]]*]", "")   // [ToolName:param]
            .replaceAll("\\*\\*([^*]+)\\*\\*", "$1") // **bold**
            .replaceAll("\\*([^*]+)\\*", "$1")       // *italic*
            .replaceAll("#{1,6}\\s*", "")             // headings
            .replaceAll("```[\\s\\S]*?```", "kode dieksekusi.") // code blocks
            .replaceAll("`[^`]+`", "")                // inline code
            .replaceAll("[-–—]\\s", ", ")             // dashes
            .trim();
        // Truncate for TTS (don't speak walls of text)
        if (clean.length() > 400) clean = clean.substring(0, 400) + "...";
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "arizen_tts_" + System.currentTimeMillis());
    }

    public void stopSpeaking() {
        if (tts != null) tts.stop();
        setState(VoiceState.IDLE);
    }

    public void setAutoSpeak(boolean auto) { this.autoSpeak = auto; }
    public boolean isAutoSpeak() { return autoSpeak; }
    public VoiceState getState() { return currentState; }
    public boolean isListening() { return currentState == VoiceState.LISTENING; }
    public boolean isSpeaking()  { return currentState == VoiceState.SPEAKING; }

    private void setState(VoiceState s) {
        currentState = s;
        if (callback != null) callback.onStateChange(s);
    }

    private String sttErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:               return "Error audio — coba lagi.";
            case SpeechRecognizer.ERROR_CLIENT:              return "Error client.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Izin mikrofon belum diberikan.";
            case SpeechRecognizer.ERROR_NETWORK:             return "Tidak ada koneksi internet untuk STT.";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:     return "STT timeout — periksa koneksi.";
            case SpeechRecognizer.ERROR_NO_MATCH:            return "Tidak dikenali — coba bicara lebih jelas.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:     return "Speech recognizer sedang sibuk.";
            case SpeechRecognizer.ERROR_SERVER:              return "Server STT error.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:      return "Timeout — tidak ada suara terdeteksi.";
            default:                                         return "Error suara (#" + error + ").";
        }
    }

    public void shutdown() {
        stopListening();
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        ttsReady = false;
    }
}
