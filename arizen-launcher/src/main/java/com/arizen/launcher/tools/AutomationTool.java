package com.arizen.launcher.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class AutomationTool implements ArizenTool {
    private final Context context;
    public AutomationTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        // Format: "remind:30m:drink water" or "alarm:08:00"
        String[] parts = params.split(":", 3);
        if (parts.length < 2) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        switch (parts[0].toLowerCase()) {
            case "remind":
                scheduleReminder(am, parts);
                break;
            case "alarm":
                scheduleAlarm(am, parts);
                break;
        }
    }

    private void scheduleReminder(AlarmManager am, String[] parts) {
        String timeStr = parts.length > 1 ? parts[1] : "30m";
        long delayMs = parseDelay(timeStr);
        Intent intent = new Intent("com.arizen.launcher.REMINDER");
        if (parts.length > 2) intent.putExtra("message", parts[2]);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delayMs, pi);
    }

    private void scheduleAlarm(AlarmManager am, String[] parts) {
        // Simplified — real impl would parse HH:MM
    }

    private long parseDelay(String s) {
        s = s.trim().toLowerCase();
        if (s.endsWith("h"))  return Long.parseLong(s.replace("h","")) * 3600000L;
        if (s.endsWith("m"))  return Long.parseLong(s.replace("m","")) * 60000L;
        if (s.endsWith("s"))  return Long.parseLong(s.replace("s","")) * 1000L;
        return 60000L; // default 1 min
    }

    @Override
    public String describe() {
        return "AutomationTool: set reminders/alarms. Usage: [AutomationTool:remind:30m:drink water] | [AutomationTool:alarm:08:00]";
    }
}
