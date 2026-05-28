package com.arizen.launcher.tools;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import java.util.Locale;

public class SystemInfoTool implements ArizenTool {
    private final Context context;
    public SystemInfoTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        // result stored for UI / AI to read
        context.getSharedPreferences("arizen_sysinfo", Context.MODE_PRIVATE).edit()
            .putString("info", getInfo()).apply();
    }

    public String getInfo() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        if (am != null) am.getMemoryInfo(mi);

        long usedMB  = (mi.totalMem - mi.availMem) / (1024 * 1024);
        long totalMB = mi.totalMem / (1024 * 1024);
        long availMB = mi.availMem / (1024 * 1024);

        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long freeStorage = stat.getAvailableBlocksLong() * stat.getBlockSizeLong() / (1024 * 1024);
        long totalStorage = stat.getBlockCountLong() * stat.getBlockSizeLong() / (1024 * 1024);

        return String.format(Locale.getDefault(),
            "ArizenOS Lite — System Info\n" +
            "────────────────────────\n" +
            "OS        : ArizenOS Lite v%s\n" +
            "Device    : SM-T295\n" +
            "Android   : %s (API %d)\n" +
            "────────────────────────\n" +
            "RAM Used  : %dMB / %dMB\n" +
            "RAM Free  : %dMB\n" +
            "RAM Low   : %s\n" +
            "────────────────────────\n" +
            "Storage   : %dMB free / %dMB total\n" +
            "CPU ABI   : %s\n" +
            "────────────────────────\n",
            "1.0",
            Build.VERSION.RELEASE, Build.VERSION.SDK_INT,
            usedMB, totalMB, availMB,
            mi.lowMemory ? "YES (critical!)" : "No",
            freeStorage, totalStorage,
            Build.SUPPORTED_ABIS[0]
        );
    }

    @Override
    public String describe() {
        return "SystemInfoTool: get RAM, storage, CPU, OS info. Usage: [SystemInfoTool:all]";
    }
}
