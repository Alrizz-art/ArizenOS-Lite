package com.arizen.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ArizenAssistantActivity extends Activity {

    private EditText inputField;
    private TextView responseView, tvTyping, tvStatusBar;
    private ScrollView scrollView;
    private Button btnSend;
    private LinearLayout chatContainer;
    private TextView btnBack, btnLabsShortcut;
    private ArizenAIBridge aiBridge;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        aiBridge = new ArizenAIBridge(this);

        inputField     = findViewById(R.id.input_field);
        responseView   = findViewById(R.id.response_view);
        scrollView     = findViewById(R.id.scroll_view);
        btnSend        = findViewById(R.id.btn_send);
        tvTyping       = findViewById(R.id.tv_typing);
        chatContainer  = findViewById(R.id.chat_container);
        btnBack        = findViewById(R.id.btn_back);
        btnLabsShortcut = findViewById(R.id.btn_labs_shortcut);
        tvStatusBar    = findViewById(R.id.tv_status_bar);

        if (!aiBridge.isConfigured()) {
            responseView.setText(getString(R.string.arizen_no_key));
            tvStatusBar.setText("API key belum diset");
            tvStatusBar.setTextColor(0xFFFF4444);
        }

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_down_exit);
        });

        btnLabsShortcut.setOnClickListener(v -> {
            startActivity(new Intent(this, ArizenLabsActivity.class));
        });

        btnSend.setOnClickListener(v -> sendMessage());

        inputField.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String input = inputField.getText().toString().trim();
        if (input.isEmpty()) return;
        inputField.setText("");
        btnSend.setEnabled(false);

        // Add user message bubble
        addUserBubble(input);

        // Show typing
        tvTyping.setVisibility(View.VISIBLE);
        tvStatusBar.setText("Mengetik...");
        tvStatusBar.setTextColor(0xFFFF9F0A);

        aiBridge.ask(input, new ArizenAIBridge.AIResponseCallback() {
            @Override
            public void onResponse(String response) {
                mainHandler.post(() -> {
                    tvTyping.setVisibility(View.GONE);
                    tvStatusBar.setText("Online");
                    tvStatusBar.setTextColor(0xFF44FF88);
                    addArizenBubble(response);
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                    btnSend.setEnabled(true);
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    tvTyping.setVisibility(View.GONE);
                    tvStatusBar.setText("Error");
                    tvStatusBar.setTextColor(0xFFFF4444);
                    addArizenBubble("Maaf, terjadi error: " + error +
                        "\n\nPastikan API key sudah dikonfigurasi di Arizen Settings.");
                    btnSend.setEnabled(true);
                });
            }
        });
    }

    private void addUserBubble(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, dpToPx(12));
        row.setLayoutParams(rp);

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextColor(0xFFFFFFFF);
        bubble.setTextSize(14);
        bubble.setLineSpacing(0, 1.5f);
        bubble.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        bubble.setBackgroundColor(0xFF1A3A6A);

        row.addView(bubble);
        chatContainer.addView(row);

        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(200);
        row.startAnimation(anim);
    }

    private void addArizenBubble(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, dpToPx(16));
        row.setLayoutParams(rp);

        View orbDot = new View(this);
        orbDot.setBackgroundColor(0xFF4A9EFF);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
        op.setMargins(0, dpToPx(4), dpToPx(10), 0);
        orbDot.setLayoutParams(op);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView label = new TextView(this);
        label.setText("Arizen");
        label.setTextColor(0xFF4A9EFF);
        label.setTextSize(11);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(4));
        label.setLayoutParams(lp);

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextColor(0xFFFFFFFF);
        bubble.setTextSize(14);
        bubble.setLineSpacing(0, 1.6f);
        bubble.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        bubble.setBackgroundColor(0xFF0F0F0F);

        content.addView(label);
        content.addView(bubble);
        row.addView(orbDot);
        row.addView(content);
        chatContainer.addView(row);

        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(300);
        row.startAnimation(anim);
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        aiBridge.shutdown();
    }

    @Override public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_down_exit);
    }
}
