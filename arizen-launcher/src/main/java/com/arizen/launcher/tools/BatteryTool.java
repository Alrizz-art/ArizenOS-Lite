package com.arizen.launcher.tools;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryTool implements ArizenTool {
    private final Context context;
    public BatteryTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        context.getSharedPreferences("arizen_battery", Context.MODE_PRIVATE).edit()
            .putString("info", getInfo()).apply();
    }

    public String getInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent status = context.registerReceiver(null, ifilter);
        if (status == null) return "Tidak bisa baca baterai";

        int level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int pct   = (scale > 0) ? (level * 100 / scale) : -1;

        int plugged = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        String charging;
        if (plugged == BatteryManager.BATTERY_PLUGGED_AC)   charging = "Charging (AC)";
        else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) charging = "Charging (USB)";
        else charging = "Not charging";

        int health = status.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        String healthStr;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:    healthStr = "Good"; break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:healthStr = "Overheat!"; break;
            case BatteryManager.BATTERY_HEALTH_DEAD:    healthStr = "Dead"; break;
            default: healthStr = "Unknown";
        }

        int temp = status.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        float tempC = temp / 10.0f;

        return String.format(
            "Baterai: %d%%\nStatus: %s\nSuhu: %.1f°C\nKondisi: %s",
            pct, charging, tempC, healthStr
        );
    }

    @Override
    public String describe() {
        return "BatteryTool: get battery level, charging status, temperature. Usage: [BatteryTool:status]";
    }
}
