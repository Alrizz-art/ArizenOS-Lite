package com.arizen.launcher;

import android.content.Context;
import android.util.Log;
import com.arizen.launcher.tools.ArizenToolManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ArizenOS Lite — AI Bridge
 * Connects Arizen Core AI to the launcher UI
 */
public class ArizenAIBridge {
    private static final String TAG = "ArizenAIBridge";
    private final Context context;
    private final ArizenSettings settings;
    private final ArizenToolManager toolManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface AIResponseCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public ArizenAIBridge(Context context) {
        this.context = context;
        this.settings = new ArizenSettings(context);
        this.toolManager = new ArizenToolManager(context);
    }

    public void ask(String userMessage, AIResponseCallback callback) {
        executor.submit(() -> {
            try {
                String response = ArizenAPIClient.chat(
                    settings.getBaseUrl(),
                    settings.getApiKey(),
                    settings.getModel(),
                    toolManager.getToolDescriptions(),
                    userMessage
                );
                toolManager.processResponse(response);
                callback.onResponse(response);
            } catch (Exception e) {
                Log.e(TAG, "AI request failed", e);
                callback.onError(e.getMessage());
            }
        });
    }

    public boolean isConfigured() {
        String key = settings.getApiKey();
        return key != null && !key.isEmpty();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
