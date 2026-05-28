package com.arizen.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.*;

public class ArizenAssistantActivity extends Activity {

    private static final int REQ_RECORD_AUDIO = 101;

    private EditText inputField;
    private TextView tvTyping, tvStatusBar, responseView;
    private ScrollView scrollView;
    private Button btnSend;
    private ImageButton btnMic;
    private View micRipple;
    private Switch swAutoSpeak;
    private LinearLayout chatContainer;
    private TextView btnBack, btnLabsShortcut;

    private ArizenAIBridge    aiBridge;
    private ArizenVoiceEngine voiceEngine;
    private final Handler     mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        aiBridge = new ArizenAIBridge(this);

        inputField      = findViewById(R.id.input_field);
        responseView    = findViewById(R.id.response_view);
        scrollView      = findViewById(R.id.scroll_view);
        btnSend         = findViewById(R.id.btn_send);
        btnMic          = findViewById(R.id.btn_mic);
        micRipple       = findViewById(R.id.mic_ripple);
        tvTyping        = findViewById(R.id.tv_typing);
        chatContainer   = findViewById(R.id.chat_container);
        btnBack         = findViewById(R.id.btn_back);
        btnLabsShortcut = findViewById(R.id.btn_labs_shortcut);
        tvStatusBar     = findViewById(R.id.tv_status_bar);
        swAutoSpeak     = findViewById(R.id.sw_auto_speak);

        initVoiceEngine();

        ArizenSettings settings = new ArizenSettings(this);
        swAutoSpeak.setChecked(settings.isAutoSpeakEnabled());
        swAutoSpeak.setOnCheckedChangeListener((b, checked) -> {
            settings.setAutoSpeakEnabled(checked);
            if (voiceEngine != null) voiceEngine.setAutoSpeak(checked);
        });

        if (!aiBridge.isConfigured()) {
            responseView.setText(getString(R.string.arizen_no_key));
            tvStatusBar.setText("Konfigurasi API key dulu");
            tvStatusBar.setTextColor(0xFFFF4444);
        }

        // Handle prefill from Labs
        String prefill = getIntent().getStringExtra("prefill");
        if (prefill != null && !prefill.isEmpty()) {
            inputField.setText(prefill);
            inputField.setSelection(prefill.length());
        }

        // Auto-start voice if launched by wake word
        boolean voiceMode = getIntent().getBooleanExtra("voice_mode", false);
        if (voiceMode) {
            mainHandler.postDelayed(this::handleMicTap, 600); // small delay for activity to settle
        }

        btnBack.setOnClickListener(v -> finish());
        btnLabsShortcut.setOnClickListener(v ->
            startActivity(new Intent(this, ArizenLabsActivity.class)));
        btnSend.setOnClickListener(v -> sendMessage());
        inputField.setOnEditorActionListener((v, actionId, event) -> { sendMessage(); return true; });
        btnMic.setOnClickListener(v -> handleMicTap());
    }

    // ── New intent (re-use if already open) ───────────────────────────────
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        boolean voiceMode = intent.getBooleanExtra("voice_mode", false);
        if (voiceMode && voiceEngine != null
                && voiceEngine.getState() == ArizenVoiceEngine.VoiceState.IDLE) {
            mainHandler.postDelayed(this::handleMicTap, 400);
        }
    }

    // ── Voice Engine ─────────────────────────────────────────────────────
    private void initVoiceEngine() {
        voiceEngine = new ArizenVoiceEngine(this, new ArizenVoiceEngine.VoiceCallback() {
            @Override public void onStateChange(ArizenVoiceEngine.VoiceState s) {
                mainHandler.post(() -> updateVoiceUI(s));
            }
            @Override public void onResult(String text) {
                mainHandler.post(() -> {
                    inputField.setText(text);
                    sendMessage();
                });
            }
            @Override public void onError(String error) {
                mainHandler.post(() -> {
                    tvStatusBar.setText(error);
                    tvStatusBar.setTextColor(0xFFFF4444);
                    setMicIdle();
                    mainHandler.postDelayed(() -> {
                        tvStatusBar.setText("Online");
                        tvStatusBar.setTextColor(0xFF44FF88);
                    }, 3000);
                });
            }
            @Override public void onSpeakDone() {
                mainHandler.post(() -> {
                    tvStatusBar.setText("Online");
                    tvStatusBar.setTextColor(0xFF44FF88);
                });
            }
            @Override public void onRmsChanged(float rms) {
                mainHandler.post(() -> {
                    float scale = 1.0f + (rms / 10f) * 0.6f;
                    micRipple.setScaleX(scale);
                    micRipple.setScaleY(scale);
                    micRipple.setAlpha(0.3f + (rms / 10f) * 0.5f);
                });
            }
        });
        ArizenSettings settings = new ArizenSettings(this);
        voiceEngine.setAutoSpeak(settings.isAutoSpeakEnabled());
    }

    // ── Mic tap ───────────────────────────────────────────────────────────
    private void handleMicTap() {
        if (voiceEngine == null) return;
        switch (voiceEngine.getState()) {
            case IDLE:
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    voiceEngine.stopSpeaking();
                    voiceEngine.startListening();
                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
                }
                break;
            case LISTENING:
                voiceEngine.stopListening();
                break;
            case SPEAKING:
                voiceEngine.stopSpeaking();
                break;
            case PROCESSING:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int reqCode, String[] perms, int[] grants) {
        if (reqCode == REQ_RECORD_AUDIO && grants.length > 0
                && grants[0] == PackageManager.PERMISSION_GRANTED) {
            voiceEngine.startListening();
        } else {
            tvStatusBar.setText("Izin mikrofon ditolak — voice tidak bisa digunakan");
            tvStatusBar.setTextColor(0xFFFF4444);
        }
    }

    // ── Voice UI ──────────────────────────────────────────────────────────
    private void updateVoiceUI(ArizenVoiceEngine.VoiceState state) {
        switch (state) {
            case IDLE:
                setMicIdle();
                break;
            case LISTENING:
                btnMic.setImageResource(android.R.drawable.ic_btn_speak_now);
                btnMic.setColorFilter(0xFFFF453A);
                micRipple.setVisibility(View.VISIBLE);
                tvStatusBar.setText("Mendengarkan…");
                tvStatusBar.setTextColor(0xFFFF453A);
                startMicPulse();
                break;
            case PROCESSING:
                btnMic.clearColorFilter();
                micRipple.setVisibility(View.INVISIBLE);
                tvStatusBar.setText("Memproses suara…");
                tvStatusBar.setTextColor(0xFFFF9F0A);
                break;
            case SPEAKING:
                btnMic.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                btnMic.setColorFilter(0xFF4A9EFF);
                micRipple.setVisibility(View.VISIBLE);
                tvStatusBar.setText("Arizen berbicara…");
                tvStatusBar.setTextColor(0xFF4A9EFF);
                startSpeakPulse();
                break;
        }
    }

    private void setMicIdle() {
        btnMic.setImageResource(android.R.drawable.ic_btn_speak_now);
        btnMic.clearColorFilter();
        micRipple.setVisibility(View.INVISIBLE);
        micRipple.setScaleX(1f); micRipple.setScaleY(1f);
        micRipple.clearAnimation();
        btnMic.clearAnimation();
        tvStatusBar.setText("Online");
        tvStatusBar.setTextColor(0xFF44FF88);
    }

    private void startMicPulse() {
        ScaleAnimation pulse = new ScaleAnimation(
            0.9f, 1.1f, 0.9f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        pulse.setDuration(600);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        btnMic.startAnimation(pulse);
    }

    private void startSpeakPulse() {
        AlphaAnimation pulse = new AlphaAnimation(0.4f, 1.0f);
        pulse.setDuration(800);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        micRipple.startAnimation(pulse);
    }

    // ── Send ──────────────────────────────────────────────────────────────
    private void sendMessage() {
        String input = inputField.getText().toString().trim();
        if (input.isEmpty()) return;
        inputField.setText("");
        btnSend.setEnabled(false);
        if (voiceEngine != null) voiceEngine.stopSpeaking();

        addUserBubble(input);
        tvTyping.setVisibility(View.VISIBLE);
        tvStatusBar.setText("Mengetik…");
        tvStatusBar.setTextColor(0xFFFF9F0A);

        aiBridge.ask(input, new ArizenAIBridge.AIResponseCallback() {
            @Override public void onResponse(String response) {
                mainHandler.post(() -> {
                    tvTyping.setVisibility(View.GONE);
                    addArizenBubble(response);
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                    btnSend.setEnabled(true);
                    if (voiceEngine != null && voiceEngine.isAutoSpeak()) {
                        voiceEngine.speak(response);
                    } else {
                        tvStatusBar.setText("Online");
                        tvStatusBar.setTextColor(0xFF44FF88);
                    }
                });
            }
            @Override public void onError(String error) {
                mainHandler.post(() -> {
                    tvTyping.setVisibility(View.GONE);
                    tvStatusBar.setText("Error");
                    tvStatusBar.setTextColor(0xFFFF4444);
                    addArizenBubble("Maaf, terjadi kesalahan: " + error);
                    btnSend.setEnabled(true);
                });
            }
        });
    }

    // ── Bubbles ───────────────────────────────────────────────────────────
    private void addUserBubble(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(12));
        row.setLayoutParams(lp);

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextColor(0xFFFFFFFF);
        bubble.setTextSize(14);
        bubble.setLineSpacing(0, 1.5f);
        bubble.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        bubble.setBackgroundColor(0xFF1A3A6A);
        row.addView(bubble);
        chatContainer.addView(row);
        fadeIn(row, 200);
    }

    private void addArizenBubble(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(16));
        row.setLayoutParams(lp);

        View dot = new View(this);
        dot.setBackgroundColor(0xFF4A9EFF);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
        dp.setMargins(0, dpToPx(4), dpToPx(10), 0);
        dot.setLayoutParams(dp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView label = new TextView(this);
        label.setText("Arizen");
        label.setTextColor(0xFF4A9EFF);
        label.setTextSize(11);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, 0, 0, dpToPx(4));
        label.setLayoutParams(llp);

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextColor(0xFFFFFFFF);
        bubble.setTextSize(14);
        bubble.setLineSpacing(0, 1.6f);
        bubble.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        bubble.setBackgroundColor(0xFF0F0F0F);

        content.addView(label);
        content.addView(bubble);
        row.addView(dot);
        row.addView(content);
        chatContainer.addView(row);
        fadeIn(row, 300);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void fadeIn(View v, int ms) {
        AlphaAnimation a = new AlphaAnimation(0f, 1f);
        a.setDuration(ms);
        v.startAnimation(a);
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        aiBridge.shutdown();
        if (voiceEngine != null) voiceEngine.shutdown();
    }

    @Override public void onBackPressed() { finish(); }
}
