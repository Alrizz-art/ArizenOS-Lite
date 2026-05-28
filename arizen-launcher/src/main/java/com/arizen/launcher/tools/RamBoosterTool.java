package com.arizen.launcher.tools;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import java.util.List;

public class RamBoosterTool implements ArizenTool {
    private final Context context;
    public RamBoosterTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;

        ActivityManager.MemoryInfo before = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(before);

        // Kill all background processes except system + self
        List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
        int killed = 0;
        if (procs != null) {
            for (ActivityManager.RunningAppProcessInfo proc : procs) {
                if (proc.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
                        && !proc.processName.equals(context.getPackageName())
                        && !proc.processName.startsWith("com.android")
                        && !proc.processName.startsWith("android")) {
                    am.killBackgroundProcesses(proc.processName);
                    killed++;
                }
            }
        }

        ActivityManager.MemoryInfo after = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(after);
        long freedMB = (after.availMem - before.availMem) / (1024 * 1024);
        long availMB = after.availMem / (1024 * 1024);
        long totalMB = after.totalMem / (1024 * 1024);

        // Store result for UI to read
        context.getSharedPreferences("arizen_ram", Context.MODE_PRIVATE).edit()
            .putString("last_boost_result",
                "Diboost: " + killed + " proses dihentikan.\n" +
                "RAM bebas: " + availMB + "MB / " + totalMB + "MB\n" +
                "Dibebaskan: ~" + Math.max(0, freedMB) + "MB")
            .apply();
    }

    public String getLastResult(Context ctx) {
        return ctx.getSharedPreferences("arizen_ram", Context.MODE_PRIVATE)
            .getString("last_boost_result", "Belum pernah dijalankan");
    }

    @Override
    public String describe() {
        return "RamBoosterTool: kill background processes to free RAM. Usage: [RamBoosterTool:boost]";
    }
}
