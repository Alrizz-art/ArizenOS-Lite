package com.arizen.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.*;
import com.arizen.launcher.tools.*;
import java.util.Locale;

public class ArizenLabsActivity extends Activity {

    private TextView tvRamFree, tvRamUsed, tvBattery, tvBuildInfo, btnBack;
    private Switch switchAmbience;
    private LinearLayout cardAgent, cardRamBoost, cardSysInfo, cardNotes, cardCalc, cardBattery;
    private LinearLayout quickSummarize, quickTranslate, quickCode, quickAlarm;
    private TextView tvLastResult;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sysUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labs);

        bindViews();
        setupClickListeners();
        startSystemMonitor();
        updateBatteryInfo();
    }

    private void bindViews() {
        tvRamFree   = findViewById(R.id.tv_ram_free);
        tvRamUsed   = findViewById(R.id.tv_ram_used);
        tvBattery   = findViewById(R.id.tv_battery_labs);
        tvBuildInfo = findViewById(R.id.tv_build_info);
        tvLastResult= findViewById(R.id.tv_last_result);
        btnBack     = findViewById(R.id.btn_back);
        switchAmbience = findViewById(R.id.switch_ambience);
        cardAgent   = findViewById(R.id.card_agent);
        cardRamBoost= findViewById(R.id.card_ram_boost);
        cardSysInfo = findViewById(R.id.card_sysinfo);
        cardNotes   = findViewById(R.id.card_notes);
        cardCalc    = findViewById(R.id.card_calc);
        cardBattery = findViewById(R.id.card_battery);
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

        // Arizen Agent — open full AI chat
        cardAgent.setOnClickListener(v -> startActivity(new Intent(this, ArizenAssistantActivity.class)));

        // RAM Booster — actually kill bg processes
        cardRamBoost.setOnClickListener(v -> {
            tvLastResult.setText("Meng-boost RAM...");
            RamBoosterTool rbt = new RamBoosterTool(this);
            rbt.execute("boost");
            String result = rbt.getLastResult(this);
            tvLastResult.setText(result);
            tvLastResult.setVisibility(View.VISIBLE);
            flashCard(cardRamBoost);
        });

        // System Info
        cardSysInfo.setOnClickListener(v -> {
            SystemInfoTool si = new SystemInfoTool(this);
            tvLastResult.setText(si.getInfo());
            tvLastResult.setVisibility(View.VISIBLE);
        });

        // Battery detail
        cardBattery.setOnClickListener(v -> {
            BatteryTool bt = new BatteryTool(this);
            bt.execute("");
            tvLastResult.setText(bt.getInfo());
            tvLastResult.setVisibility(View.VISIBLE);
        });

        // Notes
        cardNotes.setOnClickListener(v -> {
            NotesTool nt = new NotesTool(this);
            showInputDialog("Catatan Baru", "Tulis catatan...", input -> {
                nt.saveNote(input);
                tvLastResult.setText("Catatan disimpan!\n\n" + nt.getNotesList());
                tvLastResult.setVisibility(View.VISIBLE);
            });
        });

        // Calculator
        cardCalc.setOnClickListener(v -> {
            showInputDialog("Kalkulator", "Contoh: 2+2, sqrt(144), sin(45)", input -> {
                CalculatorTool ct = new CalculatorTool(this);
                ct.execute(input);
                String result = getSharedPreferences("arizen_calc", MODE_PRIVATE).getString("result", "");
                tvLastResult.setText(result);
                tvLastResult.setVisibility(View.VISIBLE);
            });
        });

        // Quick AI actions
        quickSummarize.setOnClickListener(v -> launchAI("Ringkas teks ini secara singkat dan jelas:\n\n" + getClipboard()));
        quickTranslate.setOnClickListener(v -> launchAI("Terjemahkan ke Bahasa Indonesia:\n\n" + getClipboard()));
        quickCode.setOnClickListener(v -> {
            showInputDialog("AI Coding", "Describe apa yang mau dikodekan:", input -> launchAI("Tulis kode untuk: " + input));
        });
        quickAlarm.setOnClickListener(v -> {
            showInputDialog("Set Alarm", "Format: HH:MM (contoh: 07:30)", input -> {
                AlarmTool at = new AlarmTool(this);
                at.execute(input + ":Arizen Alarm");
                String result = getSharedPreferences("arizen_alarm", MODE_PRIVATE).getString("last_alarm","");
                tvLastResult.setText(result);
                tvLastResult.setVisibility(View.VISIBLE);
            });
        });
    }

    private void launchAI(String prefill) {
        Intent i = new Intent(this, ArizenAssistantActivity.class);
        i.putExtra("prefill", prefill);
        startActivity(i);
    }

    private String getClipboard() {
        ClipboardTool ct = new ClipboardTool(this);
        String clip = ct.getClipboard();
        return clip.isEmpty() ? "(clipboard kosong, silakan ketik)" : clip;
    }

    private void showInputDialog(String title, String hint, java.util.function.Consumer<String> onDone) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(title);
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0x66FFFFFF);
        input.setPadding(40, 20, 40, 20);
        builder.setView(input);
        builder.setPositiveButton("OK", (d, w) -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) onDone.accept(text);
        });
        builder.setNegativeButton("Batal", null);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.card_bg);
        dialog.show();
    }

    private void flashCard(View card) {
        AlphaAnimation anim = new AlphaAnimation(0.3f, 1.0f);
        anim.setDuration(400);
        card.startAnimation(anim);
    }

    private void startSystemMonitor() {
        sysUpdater = new Runnable() {
            @Override public void run() {
                updateRamDisplay();
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(sysUpdater);
    }

    private void updateRamDisplay() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long freeMB  = mi.availMem / (1024 * 1024);
        long totalMB = mi.totalMem / (1024 * 1024);
        long usedMB  = totalMB - freeMB;
        tvRamFree.setText(freeMB + "MB free");
        tvRamUsed.setText(usedMB + "MB / " + totalMB + "MB");
        if (mi.lowMemory) {
            tvRamFree.setTextColor(0xFFFF4444);
        } else if (freeMB < 200) {
            tvRamFree.setTextColor(0xFFFF9F0A);
        } else {
            tvRamFree.setTextColor(0xFF44FF88);
        }
    }

    private void updateBatteryInfo() {
        BatteryTool bt = new BatteryTool(this);
        tvBattery.setText(bt.getInfo().split("\n")[0]); // just "Baterai: XX%"
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (sysUpdater != null) handler.removeCallbacks(sysUpdater);
    }

    @Override public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
