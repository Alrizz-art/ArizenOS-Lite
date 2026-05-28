package com.arizen.launcher;

import android.content.Context;
import com.arizen.launcher.tools.ArizenToolManager;
import com.arizen.launcher.tools.TimeTool;
import com.arizen.launcher.tools.BatteryTool;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArizenAIBridge {
    private final ArizenSettings     settings;
    private final ArizenToolManager  toolManager;
    private final ArizenRoutineManager routineManager;
    private final ExecutorService    executor = Executors.newSingleThreadExecutor();

    public interface AIResponseCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public ArizenAIBridge(Context ctx) {
        settings       = new ArizenSettings(ctx);
        toolManager    = new ArizenToolManager(ctx);
        routineManager = new ArizenRoutineManager(ctx);
    }

    public boolean isConfigured() { return settings.hasApiKey(); }

    public void ask(String message, AIResponseCallback callback) {
        executor.submit(() -> {
            try {
                // Inject live context into every request
                StringBuilder ctx = new StringBuilder();
                TimeTool tt = toolManager.getTool(TimeTool.class);
                BatteryTool bt = toolManager.getTool(BatteryTool.class);
                if (tt != null) ctx.append(tt.getCurrentDateTime()).append("\n");
                if (bt != null) ctx.append(bt.getInfo()).append("\n");
                // Inject saved routines so AI knows what's available
                ctx.append("\n").append(routineManager.describeForAI());

                String raw = ArizenAPIClient.chat(
                    settings.getBaseUrl(),
                    settings.getApiKey(),
                    settings.getModel(),
                    toolManager.getToolDescriptions(),
                    message,
                    ctx.toString()
                );

                // Execute tool calls embedded in response
                String cleaned = toolManager.processAndExecute(raw);
                callback.onResponse(cleaned.isEmpty() ? "Selesai." : cleaned);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        });
    }

    public void shutdown() { executor.shutdown(); }
}
