package com.arizen.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.os.AsyncTask;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.net.*;

/**
 * ArizenOS Lite — AI Assistant Activity
 * Ambient AI interface powered by cloud LLM
 */
public class ArizenAssistantActivity extends Activity {

    private EditText inputField;
    private TextView responseView;
    private ArizenToolManager toolManager;
    private ArizenSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        settings = new ArizenSettings(this);
        toolManager = new ArizenToolManager(this);

        inputField = findViewById(R.id.input_field);
        responseView = findViewById(R.id.response_view);
        Button sendBtn = findViewById(R.id.btn_send);

        sendBtn.setOnClickListener(v -> {
            String userInput = inputField.getText().toString().trim();
            if (!userInput.isEmpty()) {
                sendToAI(userInput);
                inputField.setText("");
            }
        });
    }

    private void sendToAI(String userMessage) {
        responseView.setText("Arizen is thinking...");

        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                try {
                    return callAIAPI(params[0]);
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String response) {
                responseView.setText(response);
                toolManager.processResponse(response);
            }
        }.execute(userMessage);
    }

    private String callAIAPI(String userMessage) throws Exception {
        String apiKey = settings.getApiKey();
        String baseUrl = settings.getBaseUrl();
        String model = settings.getModel();

        if (apiKey == null || apiKey.isEmpty()) {
            return "Please set your API key in Arizen Settings → AI.";
        }

        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("stream", false);

        JSONArray messages = new JSONArray();

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "You are Arizen, an intelligent AI assistant in ArizenOS Lite. " +
            "You can control the device using tools. Be concise and helpful. " +
            "Available tools: " + toolManager.getToolDescriptions());
        messages.put(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.put(userMsg);

        payload.put("messages", messages);
        payload.put("max_tokens", 512);

        URL url = new URL(baseUrl + "/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes("UTF-8"));
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject response = new JSONObject(sb.toString());
        return response.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content");
    }
}
