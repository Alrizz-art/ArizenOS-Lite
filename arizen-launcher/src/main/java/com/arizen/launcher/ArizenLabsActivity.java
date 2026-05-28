package com.arizen.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import com.arizen.launcher.tools.*;
import java.util.Locale;

public class ArizenLabsActivity extends Activity {

    private TextView tvRamFree, tvRamUsed, tvBattery, tvBuildInfo, btnBack;
    private Switch   switchAmbience;
    private LinearLayout cardAgent, cardRamBoost, cardSysInfo, cardNotes,
                         cardCalc, cardBattery, cardRoutines;
    private LinearLayout quickSummarize, quickTranslate, quickCode, quickAlarm;
    private TextView tvLastResult;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sysUpdater;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labs);

        bindViews();
        setupClickListeners();
        startSystemMonitor();
        updateBatteryInfo();
    }

    private void bindViews() {
        tvRamFree      = findViewById(R.id.tv_ram_free);
        tvRamUsed      = findViewById(R.id.tv_ram_used);
        tvBattery      = findViewById(R.id.tv_battery_labs);
        tvBuildInfo    = findViewById(R.id.tv_build_info);
        tvLastResult   = findViewById(R.id.tv_last_result);
        btnBack        = findViewById(R.id.btn_back);
        switchAmbience = findViewById(R.id.switch_ambience);
        cardAgent      = findViewById(R.id.card_agent);
        cardRamBoost   = findViewById(R.id.card_ram_boost);
        cardSysInfo    = findViewById(R.id.card_sysinfo);
        cardNotes      = findViewById(R.id.card_notes);
        cardCalc       = findViewById(R.id.card_calc);
        cardBattery    = findViewById(R.id.card_battery);
        cardRoutines   = findViewById(R.id.card_routines);
        quickSummarize = findViewById(R.id.quick_summarize);
        quickTranslate = findViewById(R.id.quick_translate);
        quickCode      = findViewById(R.id.quick_code);
        quickAlarm     = findViewById(R.id.quick_alarm);

        tvBuildInfo.setText("ArizenOS Lite 1.0  |  ArizenLabs  |  SM-T295");

        ArizenSettings settings = new ArizenSettings(this);
        switchAmbience.setChecked(settings.isAmbienceEnabled());
        switchAmbience.setOnCheckedChangeListener((b, checked) -> {
            settings.setAmbienceEnabled(checked);
            if (checked) startService(new Intent(this, ArizenAmbientService.class));
            else         stopService(new Intent(this, ArizenAmbientService.class));
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> { finish(); overridePendingTransition(R.anim.fade_in, R.anim.fade_out); });

        cardAgent.setOnClickListener(v ->
            startActivity(new Intent(this, ArizenAssistantActivity.class)));

        cardRoutines.setOnClickListener(v ->
            startActivity(new Intent(this, ArizenRoutinesActivity.class)));

        cardRamBoost.setOnClickListener(v -> {
            tvLastResult.setText("Meng-boost RAM…");
            tvLastResult.setVisibility(View.VISIBLE);
            RamBoosterTool rbt = new RamBoosterTool(this);
            rbt.execute("boost");
            tvLastResult.setText(rbt.getLastResult(this));
            flashCard(cardRamBoost);
        });

        cardSysInfo.setOnClickListener(v -> {
            tvLastResult.setText(new SystemInfoTool(this).getInfo());
            tvLastResult.setVisibility(View.VISIBLE);
        });

        cardBattery.setOnClickListener(v -> {
            BatteryTool bt = new BatteryTool(this);
            bt.execute("");
            tvLastResult.setText(bt.getInfo());
            tvLastResult.setVisibility(View.VISIBLE);
        });

        cardNotes.setOnClickListener(v -> {
            NotesTool nt = new NotesTool(this);
            showInputDialog("Catatan Baru", "Tulis catatan…", input -> {
                nt.saveNote(input);
                tvLastResult.setText("Catatan disimpan!\n\n" + nt.getNotesList());
                tvLastResult.setVisibility(View.VISIBLE);
            });
        });

        cardCalc.setOnClickListener(v ->
            showInputDialog("Kalkulator", "Contoh: sqrt(144), sin(45), 2^10", input -> {
                CalculatorTool ct = new CalculatorTool(this);
                ct.execute(input);
                String result = getSharedPreferences("arizen_calc", MODE_PRIVATE).getString("result","");
                tvLastResult.setText(result);
                tvLastResult.setVisibility(View.VISIBLE);
            }));

        quickSummarize.setOnClickListener(v -> launchAI("Ringkas teks ini:\n\n" + getClipboard()));
        quickTranslate.setOnClickListener(v -> launchAI("Terjemahkan ke Bahasa Indonesia:\n\n" + getClipboard()));
        quickCode.setOnClickListener(v ->
            showInputDialog("AI Coding", "Describe apa yang mau dikodekan:", input ->
                launchAI("Tulis kode untuk: " + input)));
        quickAlarm.setOnClickListener(v ->
            showInputDialog("Set Alarm", "Format: HH:MM (contoh: 07:30)", input -> {
                AlarmTool at = new AlarmTool(this);
                at.execute(input + ":Arizen Alarm");
                tvLastResult.setText(getSharedPreferences("arizen_alarm", MODE_PRIVATE)
                    .getString("last_alarm",""));
                tvLastResult.setVisibility(View.VISIBLE);
            }));
    }

    private void launchAI(String prefill) {
        Intent i = new Intent(this, ArizenAssistantActivity.class);
        i.putExtra("prefill", prefill);
        startActivity(i);
    }

    private String getClipboard() {
        ClipboardTool ct = new ClipboardTool(this);
        String c = ct.getClipboard();
        return c.isEmpty() ? "(clipboard kosong, silakan ketik)" : c;
    }

    private void showInputDialog(String title, String hint, java.util.function.Consumer<String> onDone) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0x66FFFFFF);
        input.setPadding(40, 20, 40, 20);
        AlertDialog d = new AlertDialog.Builder(this)
            .setTitle(title).setView(input)
            .setPositiveButton("OK", (dlg, w) -> {
                String t = input.getText().toString().trim();
                if (!t.isEmpty()) onDone.accept(t);
            })
            .setNegativeButton("Batal", null).create();
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawableResource(R.drawable.card_bg);
        d.show();
    }

    private void flashCard(View card) {
        android.view.animation.AlphaAnimation a = new android.view.animation.AlphaAnimation(0.3f, 1f);
        a.setDuration(400); card.startAnimation(a);
    }

    private void startSystemMonitor() {
        sysUpdater = new Runnable() {
            @Override public void run() {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am == null) return;
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                long freeMB  = mi.availMem / (1024*1024);
                long totalMB = mi.totalMem / (1024*1024);
                tvRamFree.setText(freeMB + "MB free");
                tvRamUsed.setText((totalMB-freeMB) + "MB / " + totalMB + "MB");
                tvRamFree.setTextColor(mi.lowMemory ? 0xFFFF4444 : freeMB < 200 ? 0xFFFF9F0A : 0xFF44FF88);
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(sysUpdater);
    }

    private void updateBatteryInfo() {
        tvBattery.setText(new BatteryTool(this).getInfo().split("\n")[0]);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (sysUpdater != null) handler.removeCallbacks(sysUpdater);
    }

    @Override public void onBackPressed() {
        finish(); overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
