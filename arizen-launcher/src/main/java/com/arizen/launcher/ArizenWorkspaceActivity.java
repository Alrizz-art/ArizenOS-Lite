package com.arizen.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ArizenWorkspaceActivity extends Activity {

    private TextView tvTime, tvDate, tvFocus, tvSessionTime;
    private LinearLayout quickLaunchRow, workspaceApps;
    private TextView btnBack, btnAI, btnMonitor, btnCmd;
    private Handler handler;
    private Runnable clockTick, sessionTick;
    private long sessionStartMs;

    private static final String[][] WORKSPACE_APPS = {
        {"com.termux",                  "Terminal",     "0xFF44FF88"},
        {"com.android.chrome",          "Chrome",       "0xFF4A9EFF"},
        {"com.github.android",          "GitHub",       "0xFFFFFFFF"},
        {"io.github.gsantner.markor",   "Markor",       "0xFF00D4AA"},
        {"org.mozilla.firefox",         "Firefox",      "0xFFFF9F0A"},
        {"com.microsoft.teams",         "Teams",        "0xFF4A9EFF"},
        {"com.slack",                   "Slack",        "0xFF8B5CF6"},
        {"org.thoughtcrime.securesms",  "Signal",       "0xFF44FF88"},
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_workspace);

        bindViews();
        setupListeners();
        startClock();
        startSession();
        populateWorkspaceApps();
    }

    private void bindViews() {
        tvTime        = findViewById(R.id.tv_ws_time);
        tvDate        = findViewById(R.id.tv_ws_date);
        tvFocus       = findViewById(R.id.tv_focus_label);
        tvSessionTime = findViewById(R.id.tv_session_time);
        quickLaunchRow = findViewById(R.id.quick_launch_row);
        workspaceApps  = findViewById(R.id.workspace_apps);
        btnBack        = findViewById(R.id.btn_back);
        btnAI          = findViewById(R.id.btn_ws_ai);
        btnMonitor     = findViewById(R.id.btn_ws_monitor);
        btnCmd         = findViewById(R.id.btn_ws_cmd);

        ArizenSettings s = new ArizenSettings(this);
        String focus = s.getWorkspaceFocus();
        tvFocus.setText(focus.isEmpty() ? "WORKSPACE MODE" : focus.toUpperCase(Locale.ROOT));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> { finish(); overridePendingTransition(R.anim.fade_in, R.anim.fade_out); });
        btnAI.setOnClickListener(v -> startActivity(new Intent(this, ArizenAssistantActivity.class)));
        btnMonitor.setOnClickListener(v -> startActivity(new Intent(this, ArizenSystemMonitorActivity.class)));
        btnCmd.setOnClickListener(v -> {
            Intent i = new Intent(this, ArizenCommandPaletteActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        tvFocus.setOnLongClickListener(v -> {
            showFocusDialog();
            return true;
        });
    }

    private void startClock() {
        handler = new Handler(Looper.getMainLooper());
        clockTick = new Runnable() {
            @Override public void run() {
                Date now = new Date();
                tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
                tvDate.setText(new SimpleDateFormat("EEEE, d MMM", new Locale("id", "ID")).format(now));
                handler.postDelayed(this, 30000);
            }
        };
        handler.post(clockTick);
    }

    private void startSession() {
        sessionStartMs = System.currentTimeMillis();
        sessionTick = new Runnable() {
            @Override public void run() {
                long elapsedMs  = System.currentTimeMillis() - sessionStartMs;
                long mins       = elapsedMs / 60000;
                long secs       = (elapsedMs % 60000) / 1000;
                tvSessionTime.setText(String.format(Locale.ROOT, "Sesi: %02d:%02d", mins, secs));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(sessionTick);
    }

    private void populateWorkspaceApps() {
        workspaceApps.removeAllViews();
        PackageManager pm = getPackageManager();

        for (String[] app : WORKSPACE_APPS) {
            String pkg   = app[0];
            String label = app[1];
            int    color = (int)Long.parseLong(app[2].substring(2), 16);

            boolean installed = pm.getLaunchIntentForPackage(pkg) != null;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);
            card.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
            card.setBackground(getDrawable(R.drawable.card_bg));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(6));
            card.setLayoutParams(lp);
            card.setClickable(installed);
            card.setFocusable(installed);
            card.setAlpha(installed ? 1f : 0.35f);

            View dot = new View(this);
            dot.setBackgroundColor(color);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
            dotLp.setMargins(0, 0, dpToPx(14), 0);
            dot.setLayoutParams(dotLp);

            TextView tvLabel = new TextView(this);
            tvLabel.setText(label);
            tvLabel.setTextColor(0xFFFFFFFF);
            tvLabel.setTextSize(14);
            tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvStatus = new TextView(this);
            tvStatus.setText(installed ? "BUKA" : "TIDAK ADA");
            tvStatus.setTextColor(installed ? (color & 0xAAFFFFFF) : 0x44FFFFFF);
            tvStatus.setTextSize(9);
            tvStatus.setTypeface(android.graphics.Typeface.MONOSPACE);

            card.addView(dot);
            card.addView(tvLabel);
            card.addView(tvStatus);

            if (installed) {
                card.setOnClickListener(v -> {
                    Intent launch = pm.getLaunchIntentForPackage(pkg);
                    if (launch != null) startActivity(launch);
                });
            }

            workspaceApps.addView(card);
        }
    }

    private void showFocusDialog() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("Set Fokus");
        String[] options = {"Coding", "Menulis", "Research", "Meeting", "Belajar", "General"};
        b.setItems(options, (d, which) -> {
            new ArizenSettings(this).setWorkspaceFocus(options[which]);
            tvFocus.setText(options[which].toUpperCase(Locale.ROOT));
        });
        android.app.AlertDialog dlg = b.create();
        if (dlg.getWindow() != null) dlg.getWindow().setBackgroundDrawableResource(R.drawable.card_bg);
        dlg.show();
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (handler != null) {
            handler.removeCallbacks(clockTick);
            handler.removeCallbacks(sessionTick);
        }
    }

    @Override public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
