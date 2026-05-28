package com.arizen.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

public class ArizenLabsActivity extends Activity {

    private TextView tvRam, btnBack;
    private Switch switchAmbience;
    private LinearLayout cardAgent, cardSysmon, cardAmbience;
    private LinearLayout quickSummarize, quickTranslate, quickCode;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ramUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labs);

        tvRam           = findViewById(R.id.tv_ram);
        btnBack         = findViewById(R.id.btn_back);
        switchAmbience  = findViewById(R.id.switch_ambience);
        cardAgent       = findViewById(R.id.card_agent);
        cardSysmon      = findViewById(R.id.card_sysmon);
        cardAmbience    = findViewById(R.id.card_ambience);
        quickSummarize  = findViewById(R.id.quick_summarize);
        quickTranslate  = findViewById(R.id.quick_translate);
        quickCode       = findViewById(R.id.quick_code);

        btnBack.setOnClickListener(v -> { finish(); overridePendingTransition(R.anim.fade_in, R.anim.fade_out); });

        cardAgent.setOnClickListener(v -> startActivity(new Intent(this, ArizenAssistantActivity.class)));

        cardSysmon.setOnClickListener(v -> showSystemInfo());

        ArizenSettings settings = new ArizenSettings(this);
        switchAmbience.setChecked(settings.isAmbienceEnabled());
        switchAmbience.setOnCheckedChangeListener((btn, checked) -> {
            settings.setAmbienceEnabled(checked);
            if (checked) startService(new Intent(this, ArizenAmbientService.class));
            else stopService(new Intent(this, ArizenAmbientService.class));
        });

        quickSummarize.setOnClickListener(v -> launchAI("Ringkas teks berikut dengan singkat dan jelas: "));
        quickTranslate.setOnClickListener(v -> launchAI("Terjemahkan ke Bahasa Indonesia: "));
        quickCode.setOnClickListener(v -> launchAI("Tulis kode untuk: "));

        startRamMonitor();
    }

    private void launchAI(String prefix) {
        Intent intent = new Intent(this, ArizenAssistantActivity.class);
        intent.putExtra("prefill", prefix);
        startActivity(intent);
    }

    private void showSystemInfo() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long usedMB = (mi.totalMem - mi.availMem) / (1024 * 1024);
        long totalMB = mi.totalMem / (1024 * 1024);
        tvRam.setText(String.format(Locale.getDefault(), "RAM: %dMB / %dMB", usedMB, totalMB));
    }

    private void startRamMonitor() {
        ramUpdater = new Runnable() {
            @Override public void run() {
                showSystemInfo();
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(ramUpdater);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (ramUpdater != null) handler.removeCallbacks(ramUpdater);
    }

    @Override public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
