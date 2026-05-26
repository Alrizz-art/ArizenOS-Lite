package com.arizen.launcher.tools;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class SettingsTool implements ArizenTool {
    private final Context context;
    public SettingsTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        Intent intent;
        switch (params.toLowerCase()) {
            case "wifi":        intent = new Intent(Settings.ACTION_WIFI_SETTINGS); break;
            case "bluetooth":   intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS); break;
            case "display":     intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS); break;
            case "sound":       intent = new Intent(Settings.ACTION_SOUND_SETTINGS); break;
            case "battery":     intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS); break;
            case "apps":        intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS); break;
            default:            intent = new Intent(Settings.ACTION_SETTINGS);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public String describe() {
        return "SettingsTool: open device settings panel. Usage: [SettingsTool:wifi] or [SettingsTool:display]";
    }
}
