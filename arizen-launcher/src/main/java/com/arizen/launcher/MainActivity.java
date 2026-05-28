package com.arizen.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvTime, tvDate, tvBattery, tvGreeting;
    private View aiOrb, aiGlowRing;
    private LinearLayout aiCard, dock, appGrid, btnLabs;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Handler pulseHandler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTime    = findViewById(R.id.tv_time);
        tvDate    = findViewById(R.id.tv_date);
        tvBattery = findViewById(R.id.tv_battery);
        tvGreeting = findViewById(R.id.tv_greeting);
        aiOrb     = findViewById(R.id.ai_orb);
        aiGlowRing = findViewById(R.id.ai_glow_ring);
        aiCard    = findViewById(R.id.ai_card);
        dock      = findViewById(R.id.dock);
        appGrid   = findViewById(R.id.app_grid);
        btnLabs   = findViewById(R.id.btn_labs);

        setupClock();
        setupGreeting();
        setupAIOrb();
        setupBattery();
        setupAppGrid();
    }

    private void setupClock() {
        Runnable clockTick = new Runnable() {
            @Override public void run() {
                Date now = new Date();
                tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
                tvDate.setText(new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID")).format(now));
                clockHandler.postDelayed(this, 30000);
            }
        };
        clockHandler.post(clockTick);
    }

    private void setupGreeting() {
        int hour = new java.util.Calendar.Builder().build().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 5)       tvGreeting.setText("Selamat malam. Ada yang bisa saya bantu?");
        else if (hour < 11) tvGreeting.setText(getString(R.string.arizen_greeting_morning));
        else if (hour < 17) tvGreeting.setText(getString(R.string.arizen_greeting_day));
        else                tvGreeting.setText(getString(R.string.arizen_greeting_evening));
    }

    private void setupAIOrb() {
        startOrbPulse();
        aiCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, ArizenAssistantActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up_enter, R.anim.fade_out);
        });
        btnLabs.setOnClickListener(v -> {
            Intent intent = new Intent(this, ArizenLabsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    private void startOrbPulse() {
        Runnable pulse = new Runnable() {
            boolean expanding = true;
            @Override public void run() {
                ScaleAnimation scale = new ScaleAnimation(
                    expanding ? 1.0f : 1.08f,
                    expanding ? 1.08f : 1.0f,
                    expanding ? 1.0f : 1.08f,
                    expanding ? 1.08f : 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
                scale.setDuration(1200);
                scale.setFillAfter(true);
                aiGlowRing.startAnimation(scale);

                AlphaAnimation alpha = new AlphaAnimation(
                    expanding ? 0.4f : 0.8f,
                    expanding ? 0.8f : 0.4f);
                alpha.setDuration(1200);
                alpha.setFillAfter(true);
                aiGlowRing.startAnimation(alpha);

                expanding = !expanding;
                pulseHandler.postDelayed(this, 1200);
            }
        };
        pulseHandler.post(pulse);
    }

    private void setupBattery() {
        batteryReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    int pct = (level * 100) / scale;
                    tvBattery.setText(pct + "%");
                    if (pct <= 20) tvBattery.setTextColor(0xFFFF4444);
                    else tvBattery.setTextColor(0x44FFFFFF);
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }

    private void setupAppGrid() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        int appsPerRow = 5;
        LinearLayout row = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % appsPerRow == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, dpToPx(8));
                row.setLayoutParams(rowParams);
                appGrid.addView(row);
            }
            AppIconView icon = new AppIconView(this, apps.get(i), pm);
            if (row != null) row.addView(icon);
        }

        // Add launcher shortcut to dock
        addDockShortcut(dock, "Arizen", 0xFF4A9EFF, () -> {
            Intent i = new Intent(this, ArizenAssistantActivity.class);
            startActivity(i);
        });
        addDockShortcut(dock, "Labs", 0xFF00D4AA, () -> {
            Intent i = new Intent(this, ArizenLabsActivity.class);
            startActivity(i);
        });
        addDockShortcut(dock, "Settings", 0xFF888888, () -> {
            Intent i = new Intent(this, ArizenSettingsActivity.class);
            startActivity(i);
        });
    }

    private void addDockShortcut(LinearLayout dock, String label, int color, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dpToPx(56), LinearLayout.LayoutParams.MATCH_PARENT);
        p.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        item.setLayoutParams(p);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v -> action.run());

        View dot = new View(this);
        dot.setBackgroundColor(color);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
        dot.setLayoutParams(dp);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xAAFFFFFF);
        lbl.setTextSize(9);
        lbl.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(3), 0, 0);
        lbl.setLayoutParams(lp);

        item.addView(dot);
        item.addView(lbl);
        dock.addView(item);
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null);
        pulseHandler.removeCallbacksAndMessages(null);
        if (batteryReceiver != null) unregisterReceiver(batteryReceiver);
    }

    @Override public void onBackPressed() {}
}
