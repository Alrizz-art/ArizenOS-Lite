package com.arizen.launcher;

import android.content.Context;
import com.arizen.launcher.tools.ArizenToolManager;
import com.arizen.launcher.tools.SystemInfoTool;
import com.arizen.launcher.tools.BatteryTool;
import com.arizen.launcher.tools.TimeTool;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArizenAIBridge {
    private final ArizenSettings settings;
    private final ArizenToolManager toolManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface AIResponseCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public ArizenAIBridge(Context ctx) {
        settings    = new ArizenSettings(ctx);
        toolManager = new ArizenToolManager(ctx);
    }

    public boolean isConfigured() { return settings.hasApiKey(); }

    public void ask(String message, AIResponseCallback callback) {
        executor.submit(() -> {
            try {
                // Inject live context into system
                String sysInfo = "";
                TimeTool tt = toolManager.getTool(TimeTool.class);
                BatteryTool bt = toolManager.getTool(BatteryTool.class);
                if (tt != null) sysInfo += tt.getCurrentDateTime() + "\n";
                if (bt != null) sysInfo += bt.getInfo() + "\n";

                String raw = ArizenAPIClient.chat(
                    settings.getBaseUrl(),
                    settings.getApiKey(),
                    settings.getModel(),
                    toolManager.getToolDescriptions(),
                    message,
                    sysInfo
                );

                // Execute any tool calls in response
                String cleaned = toolManager.processAndExecute(raw);
                callback.onResponse(cleaned.isEmpty() ? "Selesai." : cleaned);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        });
    }

    public void shutdown() { executor.shutdown(); }
}
