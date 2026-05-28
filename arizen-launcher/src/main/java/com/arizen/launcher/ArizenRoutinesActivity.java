package com.arizen.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ArizenRoutinesActivity extends Activity {

    private LinearLayout listContainer;
    private TextView btnBack, btnNewRoutine, tvLog, tvEmpty;
    private LinearLayout tabRoutines, tabLog;
    private ScrollView scrollList, scrollLog;
    private TextView tvActiveCount;

    private ArizenRoutineManager manager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean showingLog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routines);

        manager = new ArizenRoutineManager(this);
        manager.loadPresetsIfEmpty();

        listContainer = findViewById(R.id.routine_list);
        btnBack       = findViewById(R.id.btn_back);
        btnNewRoutine = findViewById(R.id.btn_new_routine);
        tvLog         = findViewById(R.id.tv_run_log);
        tvEmpty       = findViewById(R.id.tv_empty);
        tabRoutines   = findViewById(R.id.tab_routines);
        tabLog        = findViewById(R.id.tab_log);
        scrollList    = findViewById(R.id.scroll_list);
        scrollLog     = findViewById(R.id.scroll_log);
        tvActiveCount = findViewById(R.id.tv_active_count);

        btnBack.setOnClickListener(v -> finish());
        btnNewRoutine.setOnClickListener(v -> openEditor(null));
        tabRoutines.setOnClickListener(v -> switchTab(false));
        tabLog.setOnClickListener(v -> switchTab(true));

        renderRoutineList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderRoutineList();
    }

    // ── Tab ───────────────────────────────────────────────────────────────
    private void switchTab(boolean toLog) {
        showingLog = toLog;
        scrollList.setVisibility(toLog ? View.GONE  : View.VISIBLE);
        scrollLog .setVisibility(toLog ? View.VISIBLE : View.GONE);
        tabRoutines.setBackgroundColor(toLog ? 0xFF111111 : 0xFF1A2A4A);
        tabLog     .setBackgroundColor(toLog ? 0xFF1A2A4A : 0xFF111111);
        if (toLog) tvLog.setText(manager.getRunLog());
    }

    // ── Routine list ──────────────────────────────────────────────────────
    private void renderRoutineList() {
        listContainer.removeAllViews();
        List<ArizenRoutine> routines = manager.getAll();

        int active = 0;
        for (ArizenRoutine r : routines) if (r.enabled) active++;
        tvActiveCount.setText(active + " aktif");

        tvEmpty.setVisibility(routines.isEmpty() ? View.VISIBLE : View.GONE);

        for (ArizenRoutine routine : routines) {
            listContainer.addView(buildRoutineCard(routine));
        }
    }

    private View buildRoutineCard(ArizenRoutine routine) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cp);
        card.setBackgroundColor(0xFF0D0D0D);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        // Row 1: icon + name + run button
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Color accent dot
        View dot = new View(this);
        try { dot.setBackgroundColor(android.graphics.Color.parseColor(routine.color)); }
        catch (Exception e) { dot.setBackgroundColor(0xFF4A9EFF); }
        LinearLayout.LayoutParams dotp = new LinearLayout.LayoutParams(dp(4), dp(36));
        dotp.setMargins(0, 0, dp(12), 0);
        dot.setLayoutParams(dotp);

        // Icon + name
        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(routine.icon + "  " + routine.name);
        tvName.setTextColor(routine.enabled ? 0xFFFFFFFF : 0x66FFFFFF);
        tvName.setTextSize(15);

        TextView tvMeta = new TextView(this);
        String meta = routine.steps.size() + " langkah";
        if (!routine.wakeTrigger.isEmpty()) meta += "  ·  trigger: \"" + routine.wakeTrigger + "\"";
        if (routine.runCount > 0) meta += "  ·  " + routine.runCount + "× dijalankan";
        tvMeta.setText(meta);
        tvMeta.setTextColor(0x66FFFFFF);
        tvMeta.setTextSize(11);

        nameCol.addView(tvName);
        nameCol.addView(tvMeta);

        // ▶ Run button
        Button btnRun = new Button(this);
        btnRun.setText("▶");
        btnRun.setTextColor(0xFFFFFFFF);
        btnRun.setTextSize(16);
        btnRun.setBackgroundColor(0xFF1A3A1A);
        LinearLayout.LayoutParams runp = new LinearLayout.LayoutParams(dp(44), dp(44));
        runp.setMargins(dp(8), 0, 0, 0);
        btnRun.setLayoutParams(runp);
        btnRun.setOnClickListener(v -> runRoutine(routine, btnRun));

        // ⋮ Edit button
        Button btnEdit = new Button(this);
        btnEdit.setText("✎");
        btnEdit.setTextColor(0x88FFFFFF);
        btnEdit.setTextSize(16);
        btnEdit.setBackgroundColor(0x00000000);
        LinearLayout.LayoutParams editp = new LinearLayout.LayoutParams(dp(44), dp(44));
        editp.setMargins(dp(4), 0, 0, 0);
        btnEdit.setLayoutParams(editp);
        btnEdit.setOnClickListener(v -> openEditor(routine.id));

        row1.addView(dot);
        row1.addView(nameCol);
        row1.addView(btnRun);
        row1.addView(btnEdit);
        card.addView(row1);

        // Row 2: steps preview (up to 4 steps)
        if (!routine.steps.isEmpty()) {
            LinearLayout stepsRow = new LinearLayout(this);
            stepsRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams srp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            srp.setMargins(dp(16), dp(8), 0, 0);
            stepsRow.setLayoutParams(srp);

            int show = Math.min(routine.steps.size(), 4);
            for (int i = 0; i < show; i++) {
                TextView badge = new TextView(this);
                badge.setText(routine.steps.get(i).label);
                badge.setTextColor(0xAAFFFFFF);
                badge.setTextSize(10);
                badge.setBackgroundColor(0xFF1A1A1A);
                badge.setPadding(dp(8), dp(3), dp(8), dp(3));
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                bp.setMargins(0, 0, dp(6), 0);
                badge.setLayoutParams(bp);
                stepsRow.addView(badge);
            }
            if (routine.steps.size() > 4) {
                TextView more = new TextView(this);
                more.setText("+" + (routine.steps.size()-4) + " lagi");
                more.setTextColor(0x44FFFFFF);
                more.setTextSize(10);
                stepsRow.addView(more);
            }
            card.addView(stepsRow);
        }

        return card;
    }

    // ── Run routine with live progress ────────────────────────────────────
    private void runRoutine(ArizenRoutine routine, Button btnRun) {
        btnRun.setText("…");
        btnRun.setEnabled(false);
        btnRun.setBackgroundColor(0xFF2A2A1A);

        // Show toast with progress
        showToast("Menjalankan: " + routine.name);

        manager.execute(routine, new ArizenRoutineManager.ExecutionCallback() {
            @Override public void onStepStart(int idx, RoutineStep step) {
                showToast("▶ " + step.label + "…");
            }
            @Override public void onStepDone(int idx, RoutineStep step) {}
            @Override public void onFinished(ArizenRoutine r) {
                showToast("✓ " + r.name + " selesai!");
                btnRun.setText("▶");
                btnRun.setEnabled(true);
                btnRun.setBackgroundColor(0xFF1A3A1A);
                renderRoutineList(); // refresh run count
            }
            @Override public void onError(int idx, String msg) {
                showToast("Error pada langkah " + (idx+1) + ": " + msg);
                btnRun.setText("▶");
                btnRun.setEnabled(true);
                btnRun.setBackgroundColor(0xFF1A3A1A);
            }
        });
    }

    private void openEditor(String routineId) {
        Intent i = new Intent(this, ArizenRoutineEditorActivity.class);
        if (routineId != null) i.putExtra("routine_id", routineId);
        startActivity(i);
    }

    private void showToast(String msg) {
        mainHandler.post(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    @Override public void onBackPressed() { finish(); }
}
