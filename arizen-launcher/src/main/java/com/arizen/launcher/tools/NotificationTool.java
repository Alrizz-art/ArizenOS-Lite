package com.arizen.launcher.tools;

import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;

public class NotificationTool implements ArizenTool {
    private final Context context;
    public NotificationTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        NotificationManager nm =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        switch (params.toLowerCase()) {
            case "clear":
            case "dismiss":
                nm.cancelAll();
                break;
            case "dnd":
            case "silent":
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                break;
            case "normal":
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                break;
        }
    }

    @Override
    public String describe() {
        return "NotificationTool: manage notifications. Usage: [NotificationTool:clear] | [NotificationTool:dnd] | [NotificationTool:normal]";
    }
}
