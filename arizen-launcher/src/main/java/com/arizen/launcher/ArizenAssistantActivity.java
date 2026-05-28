package com.arizen.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * ArizenOS Lite — AI Assistant Activity
 * FIXED: Replaced deprecated AsyncTask with ExecutorService + Handler
 */
public class ArizenAssistantActivity extends Activity {

    private EditText inputField;
    private TextView responseView;
    private ScrollView scrollView;
    private Button sendBtn;
    private ArizenAIBridge aiBridge;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        aiBridge = new ArizenAIBridge(this);

        inputField  = findViewById(R.id.input_field);
        responseView = findViewById(R.id.response_view);
        scrollView  = findViewById(R.id.scroll_view);
        sendBtn     = findViewById(R.id.btn_send);

        if (!aiBridge.isConfigured()) {
            responseView.setText("API key belum diset.\n\nBuka Arizen Settings → AI → API Key.");
        }

        sendBtn.setOnClickListener(v -> {
            String input = inputField.getText().toString().trim();
            if (input.isEmpty()) return;
            inputField.setText("");
            sendBtn.setEnabled(false);
            responseView.setText("Arizen sedang berpikir…");

            aiBridge.ask(input, new ArizenAIBridge.AIResponseCallback() {
                @Override
                public void onResponse(String response) {
                    mainHandler.post(() -> {
                        responseView.setText(response);
                        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                        sendBtn.setEnabled(true);
                    });
                }
                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        responseView.setText("Error: " + error + "\n\nCek koneksi & API key kamu.");
                        sendBtn.setEnabled(true);
                    });
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aiBridge.shutdown();
    }
}
