package com.arizen.launcher;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;

/**
 * ArizenOS Lite — API Client
 * OpenAI-compatible REST client (works with Groq, Together, OpenAI, Cloudflare, Ollama)
 */
public class ArizenAPIClient {
    private static final int TIMEOUT_MS = 30000;

    public static String chat(String baseUrl, String apiKey, String model,
                               String toolDescriptions, String userMessage) throws Exception {
        if (apiKey == null || apiKey.isEmpty())
            throw new IllegalStateException("API key not configured");

        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("stream", false);
        payload.put("max_tokens", 512);
        payload.put("temperature", 0.7);

        String systemPrompt =
            "You are Arizen, an intelligent AI assistant embedded in ArizenOS Lite — " +
            "a premium AI-native tablet OS for the Samsung Galaxy Tab A 8.0. " +
            "You are calm, precise, and helpful. When you want to control the device, " +
            "use tool syntax: [ToolName:parameter]. " +
            "Available tools: " + toolDescriptions;

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userMessage));
        payload.put("messages", messages);

        String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions"
                                                : baseUrl + "/chat/completions";
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);

        byte[] body = payload.toString().getBytes("UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));

        try (OutputStream os = conn.getOutputStream()) { os.write(body); }

        int code = conn.getResponseCode();
        InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        JSONObject resp = new JSONObject(sb.toString());
        if (code >= 400) {
            String err = resp.optJSONObject("error") != null
                ? resp.getJSONObject("error").optString("message", sb.toString())
                : sb.toString();
            throw new IOException("API error " + code + ": " + err);
        }

        return resp.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content");
    }
}
