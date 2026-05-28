package com.arizen.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import android.view.View;
import com.arizen.launcher.tools.SystemInfoTool;
import com.arizen.launcher.tools.BatteryTool;
import com.arizen.launcher.tools.RamBoosterTool;
import java.io.BufferedReader;
import java.io.FileReader;

public class ArizenSystemMonitorActivity extends Activity {

    private TextView tvRamFree, tvRamUsed, tvRamPct, tvCpuLoad, tvCpuGov;
    private TextView tvBattPct, tvBattStatus, tvBattTemp, tvBattVolt;
    private TextView tvThermal, tvBuildInfo, tvUptime;
    private View barRam, barCpu, barBatt;
    private TextView btnBack, btnBoost;
    private LinearLayout cardRam, cardCpu, cardBatt, cardThermal;
    private TextView tvPerfMode;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updater;
    private BroadcastReceiver battReceiver;
    private long lastCpuTotal = 0, lastCpuIdle = 0;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_monitor);
        bindViews();
        setupListeners();
        startMonitor();
        registerBatteryReceiver();
    }

    private void bindViews() {
        tvRamFree    = findViewById(R.id.tv_ram_free);
        tvRamUsed    = findViewById(R.id.tv_ram_used);
        tvRamPct     = findViewById(R.id.tv_ram_pct);
        tvCpuLoad    = findViewById(R.id.tv_cpu_load);
        tvCpuGov     = findViewById(R.id.tv_cpu_gov);
        tvBattPct    = findViewById(R.id.tv_batt_pct);
        tvBattStatus = findViewById(R.id.tv_batt_status);
        tvBattTemp   = findViewById(R.id.tv_batt_temp);
        tvBattVolt   = findViewById(R.id.tv_batt_volt);
        tvThermal    = findViewById(R.id.tv_thermal);
        tvBuildInfo  = findViewById(R.id.tv_build_info_mon);
        tvUptime     = findViewById(R.id.tv_uptime);
        tvPerfMode   = findViewById(R.id.tv_perf_mode);
        barRam       = findViewById(R.id.bar_ram);
        barCpu       = findViewById(R.id.bar_cpu);
        barBatt      = findViewById(R.id.bar_batt);
        btnBack      = findViewById(R.id.btn_back);
        btnBoost     = findViewById(R.id.btn_ram_boost);
        cardRam      = findViewById(R.id.card_ram);
        cardCpu      = findViewById(R.id.card_cpu);
        cardBatt     = findViewById(R.id.card_batt);
        cardThermal  = findViewById(R.id.card_thermal);

        ArizenSettings settings = new ArizenSettings(this);
        String profile = settings.getPerformanceProfile();
        tvPerfMode.setText(profileLabel(profile));
        tvBuildInfo.setText("ArizenOS Lite  |  SM-T295  |  Snapdragon 429");
    }

    private String profileLabel(String p) {
        switch (p) {
            case "performance": return "⚡ Performance";
            case "saver":       return "🔋 Battery Saver";
            default:            return "⚖ Balanced";
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> { finish(); overridePendingTransition(R.anim.fade_in, R.anim.fade_out); });

        btnBoost.setOnClickListener(v -> {
            RamBoosterTool rbt = new RamBoosterTool(this);
            rbt.execute("boost");
            Toast.makeText(this, rbt.getLastResult(this), Toast.LENGTH_SHORT).show();
            animateCard(cardRam);
        });

        cardCpu.setOnClickListener(v -> cyclePerformanceProfile());
    }

    private void cyclePerformanceProfile() {
        ArizenSettings s = new ArizenSettings(this);
        String cur = s.getPerformanceProfile();
        String next = cur.equals("balanced") ? "performance" : cur.equals("performance") ? "saver" : "balanced";
        s.setPerformanceProfile(next);
        tvPerfMode.setText(profileLabel(next));
        Toast.makeText(this, "Profile: " + profileLabel(next), Toast.LENGTH_SHORT).show();
        animateCard(cardCpu);
    }

    private void startMonitor() {
        updater = new Runnable() {
            @Override public void run() {
                updateRam();
                updateCpu();
                updateThermal();
                updateUptime();
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updater);
    }

    private void updateRam() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long freeMB  = mi.availMem / (1024 * 1024);
        long totalMB = mi.totalMem / (1024 * 1024);
        long usedMB  = totalMB - freeMB;
        int  pct     = (int)((usedMB * 100) / totalMB);

        tvRamFree.setText(freeMB + " MB free");
        tvRamUsed.setText(usedMB + " / " + totalMB + " MB");
        tvRamPct.setText(pct + "%");

        int color = pct > 85 ? 0xFFFF4444 : pct > 70 ? 0xFFFF9F0A : 0xFF44FF88;
        tvRamFree.setTextColor(color);
        tvRamPct.setTextColor(color);
        setBarWidth(barRam, pct, color);
    }

    private void updateCpu() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/stat"));
            String line = br.readLine();
            br.close();
            if (line != null && line.startsWith("cpu ")) {
                String[] parts = line.trim().split("\\s+");
                long user   = Long.parseLong(parts[1]);
                long nice   = Long.parseLong(parts[2]);
                long system = Long.parseLong(parts[3]);
                long idle   = Long.parseLong(parts[4]);
                long iowait = parts.length > 5 ? Long.parseLong(parts[5]) : 0;
                long total  = user + nice + system + idle + iowait;
                long curIdle = idle + iowait;

                if (lastCpuTotal > 0) {
                    long dTotal = total - lastCpuTotal;
                    long dIdle  = curIdle - lastCpuIdle;
                    int cpuPct  = dTotal > 0 ? (int)((dTotal - dIdle) * 100 / dTotal) : 0;
                    tvCpuLoad.setText(cpuPct + "%");
                    int color = cpuPct > 80 ? 0xFFFF4444 : cpuPct > 60 ? 0xFFFF9F0A : 0xFF4A9EFF;
                    tvCpuLoad.setTextColor(color);
                    setBarWidth(barCpu, cpuPct, color);
                }
                lastCpuTotal = total;
                lastCpuIdle  = curIdle;
            }
        } catch (Exception ignored) {
            tvCpuLoad.setText("N/A");
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"));
            String gov = br.readLine();
            br.close();
            tvCpuGov.setText(gov != null ? gov.trim() : "unknown");
        } catch (Exception ignored) {
            tvCpuGov.setText("schedutil");
        }
    }

    private void updateThermal() {
        StringBuilder sb = new StringBuilder();
        String[] zones = {
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp",
        };
        String[] labels = {"CPU", "GPU", "Board"};
        for (int i = 0; i < zones.length; i++) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(zones[i]));
                String line = br.readLine();
                br.close();
                if (line != null) {
                    int temp = Integer.parseInt(line.trim()) / 1000;
                    if (sb.length() > 0) sb.append("  |  ");
                    sb.append(labels[i]).append(": ").append(temp).append("°C");
                }
            } catch (Exception ignored) {}
        }
        tvThermal.setText(sb.length() > 0 ? sb.toString() : "Tidak tersedia");
    }

    private void updateUptime() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/uptime"));
            String line = br.readLine();
            br.close();
            if (line != null) {
                long secs  = (long) Double.parseDouble(line.trim().split(" ")[0]);
                long hours = secs / 3600;
                long mins  = (secs % 3600) / 60;
                tvUptime.setText("Uptime: " + hours + "j " + mins + "m");
            }
        } catch (Exception ignored) {
            tvUptime.setText("Uptime: —");
        }
    }

    private void registerBatteryReceiver() {
        battReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                int level  = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale  = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int temp   = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                int volt   = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                int pct    = (level * 100) / scale;

                tvBattPct.setText(pct + "%");
                tvBattTemp.setText(String.format("%.1f°C", temp / 10f));
                tvBattVolt.setText(String.format("%.2fV", volt / 1000f));

                boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                   status == BatteryManager.BATTERY_STATUS_FULL;
                tvBattStatus.setText(charging ? "Mengisi" : "Baterai");

                int color = pct <= 20 ? 0xFFFF4444 : pct <= 40 ? 0xFFFF9F0A : 0xFF44FF88;
                tvBattPct.setTextColor(color);
                setBarWidth(barBatt, pct, color);
            }
        };
        registerReceiver(battReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void setBarWidth(View bar, int pct, int color) {
        bar.post(() -> {
            View parent = (View) bar.getParent();
            int maxW = parent.getWidth() - dpToPx(32);
            int w    = (int)(maxW * pct / 100f);
            android.view.ViewGroup.LayoutParams lp = bar.getLayoutParams();
            lp.width = Math.max(w, dpToPx(4));
            bar.setLayoutParams(lp);
            bar.setBackgroundColor(color);
        });
    }

    private void animateCard(View card) {
        android.view.animation.AlphaAnimation a = new android.view.animation.AlphaAnimation(0.3f, 1f);
        a.setDuration(350);
        card.startAnimation(a);
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (updater != null) handler.removeCallbacks(updater);
        if (battReceiver != null) unregisterReceiver(battReceiver);
    }

    @Override public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
