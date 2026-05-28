package com.arizen.launcher;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;

public class ArizenAPIClient {
    private static final int TIMEOUT_MS = 30000;
    private static final int MAX_HISTORY = 10; // keep last N exchanges

    // Conversation history (in-memory, per-session)
    private static final JSONArray history = new JSONArray();

    public static void clearHistory() {
        while (history.length() > 0) try { history.remove(0); } catch (Exception e) { break; }
    }

    public static String chat(String baseUrl, String apiKey, String model,
                               String toolDescriptions, String userMessage,
                               String contextInfo) throws Exception {
        if (apiKey == null || apiKey.isEmpty())
            throw new IllegalStateException("API key belum dikonfigurasi. Buka Arizen Settings.");

        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("stream", false);
        payload.put("max_tokens", 1024);
        payload.put("temperature", 0.7);

        String systemPrompt =
            "Kamu adalah Arizen — AI agent canggih yang tertanam di ArizenOS Lite, " +
            "sistem operasi Android buatan ArizenLabs untuk Samsung Galaxy Tab A (SM-T295). " +
            "Kamu cerdas, ringkas, dan bisa mengontrol device langsung.\n\n" +
            "KONTEKS DEVICE SAAT INI:\n" + contextInfo + "\n\n" +
            "TOOLS YANG TERSEDIA (gunakan sintaks [NamaTool:parameter]):\n" +
            toolDescriptions + "\n\n" +
            "ATURAN:\n" +
            "- Jawab dalam Bahasa Indonesia kecuali user pakai bahasa lain\n" +
            "- Kalau user minta aksi (buka app, set alarm, dll), langsung jalankan tool-nya\n" +
            "- Setelah tool call, jelaskan apa yang sudah dilakukan\n" +
            "- Ringkas dan to the point — ini OS, bukan chatbot\n" +
            "- Untuk perhitungan, gunakan CalculatorTool\n" +
            "- Untuk info RAM/sistem, gunakan SystemInfoTool atau RamBoosterTool";

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));

        // Add conversation history
        for (int i = 0; i < history.length(); i++) {
            messages.put(history.getJSONObject(i));
        }

        // Add current message
        JSONObject userMsg = new JSONObject().put("role", "user").put("content", userMessage);
        messages.put(userMsg);
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
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }

        JSONObject resp = new JSONObject(sb.toString());
        if (code >= 400) {
            String err = resp.optJSONObject("error") != null
                ? resp.getJSONObject("error").optString("message", sb.toString())
                : sb.toString();
            throw new IOException("API error " + code + ": " + err);
        }

        String reply = resp.getJSONArray("choices")
            .getJSONObject(0).getJSONObject("message").getString("content");

        // Save to history
        history.put(userMsg);
        history.put(new JSONObject().put("role", "assistant").put("content", reply));
        // Trim history to MAX_HISTORY exchanges (2 msgs each)
        while (history.length() > MAX_HISTORY * 2) history.remove(0);

        return reply;
    }
}
