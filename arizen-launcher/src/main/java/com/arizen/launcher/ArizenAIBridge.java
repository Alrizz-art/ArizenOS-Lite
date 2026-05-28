package com.arizen.launcher;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArizenAIBridge {
    private final ArizenSettings settings;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface AIResponseCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public ArizenAIBridge(Context ctx) {
        settings = new ArizenSettings(ctx);
    }

    public boolean isConfigured() {
        return settings.hasApiKey();
    }

    public void ask(String message, AIResponseCallback callback) {
        executor.submit(() -> {
            try {
                String toolDescriptions =
                    "LaunchApp:packageName — buka aplikasi berdasarkan package name. " +
                    "OpenSettings:section — buka Settings section (wifi/bluetooth/display/sound). " +
                    "GetBattery: — cek persentase baterai saat ini. " +
                    "GetTime: — cek waktu sekarang. " +
                    "GetRAM: — cek penggunaan RAM.";

                String response = ArizenAPIClient.chat(
                    settings.getBaseUrl(),
                    settings.getApiKey(),
                    settings.getModel(),
                    toolDescriptions,
                    message
                );
                callback.onResponse(response);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
