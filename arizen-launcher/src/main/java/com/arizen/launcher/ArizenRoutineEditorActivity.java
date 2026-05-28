package com.arizen.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

public class ArizenRoutineEditorActivity extends Activity {

    private EditText etName, etTrigger;
    private Spinner  spIcon, spColor;
    private LinearLayout stepsContainer;
    private Button btnAddStep, btnSave, btnDelete;
    private TextView btnBack, tvTitle;

    private ArizenRoutineManager manager;
    private ArizenRoutine currentRoutine;
    private final List<RoutineStep> editableSteps = new ArrayList<>();
    private boolean isNew = true;

    // Available steps user can add
    private static final String[][] STEP_PRESETS = {
        {"AppLauncherTool",    "",              "Buka Aplikasi"},
        {"WifiTool",           "on",            "Aktifkan WiFi"},
        {"WifiTool",           "off",           "Matikan WiFi"},
        {"BluetoothTool",      "on",            "Aktifkan Bluetooth"},
        {"BluetoothTool",      "off",           "Matikan Bluetooth"},
        {"RamBoosterTool",     "boost",         "Boost RAM"},
        {"AlarmTool",          "07:00:Alarm",   "Set Alarm"},
        {"NotificationTool",   "silent",        "Mode Senyap"},
        {"SettingsTool",       "wifi",          "Buka Pengaturan WiFi"},
        {"SettingsTool",       "display",       "Buka Pengaturan Layar"},
        {"SettingsTool",       "sound",         "Buka Pengaturan Suara"},
        {"SettingsTool",       "battery",       "Buka Pengaturan Baterai"},
        {"BrowserTool",        "https://google.com", "Buka Browser"},
        {"TimeTool",           "now",           "Cek Waktu"},
        {"BatteryTool",        "status",        "Cek Baterai"},
        {"SystemInfoTool",     "all",           "Cek System Info"},
    };

    private static final String[] ICONS = {"⚡","🌅","🌙","🎯","🎮","💼","🔄","🏃","📚","🎵","🌐","💡"};
    private static final String[] COLORS = {"#4A9EFF","#FF9F0A","#BF5AF2","#00D4AA","#FF453A","#44FF88","#FF9F0A","#FFFFFF"};
    private static final String[] COLOR_NAMES = {"Biru","Oranye","Ungu","Teal","Merah","Hijau","Kuning","Putih"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_editor);

        manager = new ArizenRoutineManager(this);

        etName        = findViewById(R.id.et_routine_name);
        etTrigger     = findViewById(R.id.et_wake_trigger);
        spIcon        = findViewById(R.id.sp_icon);
        spColor       = findViewById(R.id.sp_color);
        stepsContainer= findViewById(R.id.steps_container);
        btnAddStep    = findViewById(R.id.btn_add_step);
        btnSave       = findViewById(R.id.btn_save_routine);
        btnDelete     = findViewById(R.id.btn_delete_routine);
        btnBack       = findViewById(R.id.btn_back);
        tvTitle       = findViewById(R.id.tv_editor_title);

        // Spinners
        spIcon.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ICONS));
        spColor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, COLOR_NAMES));

        // Load existing or init new
        String existingId = getIntent().getStringExtra("routine_id");
        if (existingId != null) {
            currentRoutine = manager.getById(existingId);
            if (currentRoutine != null) {
                isNew = false;
                populateFromRoutine(currentRoutine);
            }
        }
        if (isNew) {
            tvTitle.setText("Rutinitas Baru");
            btnDelete.setVisibility(android.view.View.GONE);
        } else {
            tvTitle.setText("Edit Rutinitas");
            btnDelete.setVisibility(android.view.View.VISIBLE);
        }

        btnBack.setOnClickListener(v -> finish());
        btnAddStep.setOnClickListener(v -> showAddStepDialog());
        btnSave.setOnClickListener(v -> saveRoutine());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populateFromRoutine(ArizenRoutine r) {
        etName.setText(r.name);
        etTrigger.setText(r.wakeTrigger);
        // Find icon index
        for (int i = 0; i < ICONS.length; i++)
            if (ICONS[i].equals(r.icon)) { spIcon.setSelection(i); break; }
        for (int i = 0; i < COLORS.length; i++)
            if (COLORS[i].equalsIgnoreCase(r.color)) { spColor.setSelection(i); break; }
        editableSteps.clear();
        editableSteps.addAll(r.steps);
        refreshStepsUI();
    }

    // ── Add step dialog ───────────────────────────────────────────────────
    private void showAddStepDialog() {
        String[] labels = new String[STEP_PRESETS.length + 1];
        for (int i = 0; i < STEP_PRESETS.length; i++) labels[i] = STEP_PRESETS[i][2];
        labels[STEP_PRESETS.length] = "Kustom…";

        new AlertDialog.Builder(this)
            .setTitle("Tambah Langkah")
            .setItems(labels, (d, which) -> {
                if (which < STEP_PRESETS.length) {
                    String[] preset = STEP_PRESETS[which];
                    // If params is empty (like AppLauncherTool), ask for input
                    if (preset[1].isEmpty()) {
                        askParams(preset[0], preset[2]);
                    } else if (preset[1].contains("Alarm")) {
                        askParamsWithHint(preset[0], preset[2], "HH:MM:Label, contoh: 07:30:Bangun!");
                    } else {
                        editableSteps.add(new RoutineStep(preset[0], preset[1], preset[2], 400));
                        refreshStepsUI();
                    }
                } else {
                    showCustomStepDialog();
                }
            })
            .setNegativeButton("Batal", null)
            .create().show();
    }

    private void askParams(String tool, String label) {
        EditText input = new EditText(this);
        input.setHint("Contoh: com.google.android.youtube");
        input.setTextColor(0xFFFFFFFF);
        input.setPadding(40, 20, 40, 20);
        new AlertDialog.Builder(this)
            .setTitle(label + " — Parameter")
            .setView(input)
            .setPositiveButton("OK", (d, w) -> {
                String val = input.getText().toString().trim();
                if (!val.isEmpty()) {
                    editableSteps.add(new RoutineStep(tool, val, label + ": " + val, 400));
                    refreshStepsUI();
                }
            })
            .setNegativeButton("Batal", null)
            .create().show();
    }

    private void askParamsWithHint(String tool, String label, String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(0xFFFFFFFF);
        input.setPadding(40, 20, 40, 20);
        new AlertDialog.Builder(this)
            .setTitle(label)
            .setView(input)
            .setPositiveButton("OK", (d, w) -> {
                String val = input.getText().toString().trim();
                if (!val.isEmpty()) {
                    editableSteps.add(new RoutineStep(tool, val, label + ": " + val, 500));
                    refreshStepsUI();
                }
            })
            .setNegativeButton("Batal", null)
            .create().show();
    }

    private void showCustomStepDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etTool = new EditText(this);
        etTool.setHint("Tool (contoh: AppLauncherTool)");
        etTool.setTextColor(0xFFFFFFFF); etTool.setHintTextColor(0x44FFFFFF);
        layout.addView(etTool);

        EditText etParams = new EditText(this);
        etParams.setHint("Parameter (contoh: com.whatsapp)");
        etParams.setTextColor(0xFFFFFFFF); etParams.setHintTextColor(0x44FFFFFF);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ep.setMargins(0, 12, 0, 0); etParams.setLayoutParams(ep);
        layout.addView(etParams);

        EditText etLabel = new EditText(this);
        etLabel.setHint("Label (opsional)");
        etLabel.setTextColor(0xFFFFFFFF); etLabel.setHintTextColor(0x44FFFFFF);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.setMargins(0, 12, 0, 0); etLabel.setLayoutParams(lp2);
        layout.addView(etLabel);

        new AlertDialog.Builder(this)
            .setTitle("Langkah Kustom")
            .setView(layout)
            .setPositiveButton("Tambah", (d, w) -> {
                String tool   = etTool.getText().toString().trim();
                String params = etParams.getText().toString().trim();
                String label  = etLabel.getText().toString().trim();
                if (!tool.isEmpty()) {
                    if (label.isEmpty()) label = tool.replace("Tool","");
                    editableSteps.add(new RoutineStep(tool, params, label, 400));
                    refreshStepsUI();
                }
            })
            .setNegativeButton("Batal", null)
            .create().show();
    }

    // ── Steps UI ──────────────────────────────────────────────────────────
    private void refreshStepsUI() {
        stepsContainer.removeAllViews();
        for (int i = 0; i < editableSteps.size(); i++) {
            stepsContainer.addView(buildStepRow(i, editableSteps.get(i)));
        }
        if (editableSteps.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada langkah. Tap + untuk menambah.");
            empty.setTextColor(0x44FFFFFF);
            empty.setTextSize(12);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(20), 0, dp(20));
            stepsContainer.addView(empty);
        }
    }

    private android.view.View buildStepRow(int index, RoutineStep step) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(0xFF111111);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, dp(4));
        row.setLayoutParams(rp);

        TextView num = new TextView(this);
        num.setText(String.valueOf(index + 1));
        num.setTextColor(0x44FFFFFF);
        num.setTextSize(12);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(dp(24), LinearLayout.LayoutParams.WRAP_CONTENT);
        num.setLayoutParams(np);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView lbl = new TextView(this);
        lbl.setText(step.label);
        lbl.setTextColor(0xFFFFFFFF);
        lbl.setTextSize(13);

        TextView meta = new TextView(this);
        meta.setText(step.tool + (step.params.isEmpty() ? "" : " : " + step.params));
        meta.setTextColor(0x44FFFFFF);
        meta.setTextSize(10);
        meta.setTypeface(android.graphics.Typeface.MONOSPACE);

        col.addView(lbl);
        col.addView(meta);

        Button del = new Button(this);
        del.setText("✕");
        del.setTextColor(0x88FF4444);
        del.setTextSize(14);
        del.setBackgroundColor(0x00000000);
        del.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        final int idx = index;
        del.setOnClickListener(v -> {
            editableSteps.remove(idx);
            refreshStepsUI();
        });

        row.addView(num);
        row.addView(col);
        row.addView(del);
        return row;
    }

    // ── Save / Delete ─────────────────────────────────────────────────────
    private void saveRoutine() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) { etName.setError("Nama wajib diisi"); return; }
        if (editableSteps.isEmpty()) {
            Toast.makeText(this, "Tambahkan minimal 1 langkah", Toast.LENGTH_SHORT).show(); return;
        }

        String id = isNew ? "routine_" + System.currentTimeMillis()
                          : currentRoutine.id;
        String icon  = ICONS[spIcon.getSelectedItemPosition()];
        String color = COLORS[spColor.getSelectedItemPosition()];
        String trig  = etTrigger.getText().toString().trim().toLowerCase();

        ArizenRoutine r = new ArizenRoutine(id, name, trig, icon, color);
        r.steps.addAll(editableSteps);
        if (!isNew) { r.runCount = currentRoutine.runCount; r.lastRunMs = currentRoutine.lastRunMs; }

        manager.addRoutine(r);
        Toast.makeText(this, "Rutinitas \"" + name + "\" disimpan!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
            .setTitle("Hapus rutinitas?")
            .setMessage("\"" + currentRoutine.name + "\" akan dihapus permanen.")
            .setPositiveButton("Hapus", (d, w) -> {
                manager.removeRoutine(currentRoutine.id);
                Toast.makeText(this, "Rutinitas dihapus", Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton("Batal", null)
            .create().show();
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
    @Override public void onBackPressed() { finish(); }
}
