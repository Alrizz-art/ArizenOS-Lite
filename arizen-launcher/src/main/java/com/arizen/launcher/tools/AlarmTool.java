package com.arizen.launcher.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;

public class AlarmTool implements ArizenTool {
    private final Context context;
    public AlarmTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        // params format: "HH:MM" or "HH:MM:label"
        try {
            String[] parts = params.split(":");
            int hour   = Integer.parseInt(parts[0].trim());
            int minute = (parts.length > 1) ? Integer.parseInt(parts[1].trim()) : 0;
            String label = (parts.length > 2) ? parts[2].trim() : "Arizen Alarm";

            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
            intent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, label);
            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            context.getSharedPreferences("arizen_alarm", Context.MODE_PRIVATE).edit()
                .putString("last_alarm", "Alarm diset: " + String.format("%02d:%02d", hour, minute) + " — " + label)
                .apply();
        } catch (Exception e) {
            context.getSharedPreferences("arizen_alarm", Context.MODE_PRIVATE).edit()
                .putString("last_alarm", "Gagal set alarm: " + e.getMessage()).apply();
        }
    }

    @Override
    public String describe() {
        return "AlarmTool: set device alarm. Usage: [AlarmTool:07:30:Wake up]";
    }
}
