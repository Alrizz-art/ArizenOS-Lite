package com.arizen.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.List;
import android.widget.Toast;

/**
 * ArizenOS Lite — Arizen Launcher
 * Ultra-lightweight dashboard launcher for SM-T295 (2GB RAM)
 */
public class MainActivity extends Activity {

    private ArizenAIBridge aiBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aiBridge = new ArizenAIBridge(this);
        setupDashboard();
        setupAIBubble();
        setupAppGrid();
    }

    private void setupDashboard() {
        // Setup clock, date, battery stats
        TextView timeView = findViewById(R.id.tv_time);
        TextView dateView = findViewById(R.id.tv_date);
        // Clock updates handled by ClockWidget
    }

    private void setupAIBubble() {
        View aiBubble = findViewById(R.id.ai_bubble);
        aiBubble.setOnClickListener(v -> {
            Intent intent = new Intent(this, ArizenAssistantActivity.class);
            startActivity(intent);
        });
    }

    private void setupAppGrid() {
        LinearLayout appGrid = findViewById(R.id.app_grid);
        PackageManager pm = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : apps) {
            AppIconView icon = new AppIconView(this, info, pm);
            appGrid.addView(icon);
        }
    }

    @Override
    public void onBackPressed() {
        // Launcher intercepts back — no exit
    }
}
