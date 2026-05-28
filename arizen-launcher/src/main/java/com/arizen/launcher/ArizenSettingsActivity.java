package com.arizen.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.widget.*;
import com.arizen.launcher.tools.RamBoosterTool;
import com.arizen.launcher.tools.SystemInfoTool;

public class ArizenSettingsActivity extends Activity {

    private ArizenSettings settings;

    private TextView btnBack;
    private EditText etApiKey, etCustomUrl;
    private Spinner  spProvider, spModel;
    private TextView tvStatus, tvAboutBuild, tvRamInfo;
    private Switch   swZram, swAnimSpeed, swBgLimit, swWakeWord, swAutoSpeak;
    private Button   btnSave, btnTest, btnClearHistory, btnRamBoost;

    private static final String[] PROVIDERS = {
        "groq", "together_ai", "openai", "cloudflare", "ollama"
    };
    private static final String[] PROVIDER_LABELS = {
        "Groq  (Gratis, Cepat — Recommended)",
        "Together AI  (Gratis tier)",
        "OpenAI  (GPT-4o, berbayar)",
        "Cloudflare AI  (Workers AI)",
        "Ollama  (Lokal / server sendiri)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        settings = new ArizenSettings(this);
        bindViews();
        loadValues();
        setupListeners();
        updateAboutInfo();
    }

    private void bindViews() {
        btnBack       = findViewById(R.id.btn_back);
        etApiKey      = findViewById(R.id.et_api_key);
        etCustomUrl   = findViewById(R.id.et_custom_url);
        spProvider    = findViewById(R.id.sp_provider);
        spModel       = findViewById(R.id.sp_model);
        tvStatus      = findViewById(R.id.tv_status);
        tvAboutBuild  = findViewById(R.id.tv_about_build);
        tvRamInfo     = findViewById(R.id.tv_ram_info);
        swZram        = findViewById(R.id.sw_zram);
        swAnimSpeed   = findViewById(R.id.sw_anim_speed);
        swBgLimit     = findViewById(R.id.sw_bg_limit);
        swWakeWord    = findViewById(R.id.sw_wake_word);
        swAutoSpeak   = findViewById(R.id.sw_auto_speak_settings);
        btnSave       = findViewById(R.id.btn_save);
        btnTest       = findViewById(R.id.btn_test);
        btnClearHistory = findViewById(R.id.btn_clear_history);
        btnRamBoost   = findViewById(R.id.btn_ram_boost);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, PROVIDER_LABELS);
        spProvider.setAdapter(adapter);
    }

    private void loadValues() {
        String key = settings.getApiKey();
        if (!key.isEmpty())
            etApiKey.setHint("Tersimpan: " + key.substring(0, Math.min(8, key.length())) + "****");

        String cur = settings.getProvider();
        for (int i = 0; i < PROVIDERS.length; i++)
            if (PROVIDERS[i].equals(cur)) { spProvider.setSelection(i); break; }

        updateModelList(cur);

        swZram.setChecked(settings.isZramEnabled());
        swAnimSpeed.setChecked(settings.isAnimSpeedReduced());
        swBgLimit.setChecked(settings.isBgLimitEnabled());
        swWakeWord.setChecked(settings.isWakeWordEnabled());
        swAutoSpeak.setChecked(settings.isAutoSpeakEnabled());

        tvRamInfo.setText(new SystemInfoTool(this).getInfo());
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        spProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                settings.setProvider(PROVIDERS[pos]);
                updateModelList(PROVIDERS[pos]);
                etCustomUrl.setHint(settings.getBaseUrl());
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnSave.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            if (!key.isEmpty()) settings.saveApiKey(key);
            String sel = (String) spModel.getSelectedItem();
            if (sel != null) settings.setModel(sel);
            String url = etCustomUrl.getText().toString().trim();
            if (!url.isEmpty()) settings.setBaseUrl(url);
            tvStatus.setText(getString(R.string.status_saved));
            tvStatus.setTextColor(0xFF44FF88);
        });

        btnTest.setOnClickListener(v -> {
            tvStatus.setText("Menguji koneksi…");
            tvStatus.setTextColor(0xFFFF9F0A);
            ArizenAIBridge bridge = new ArizenAIBridge(this);
            bridge.ask("Reply with exactly: ARIZEN_OK", new ArizenAIBridge.AIResponseCallback() {
                @Override public void onResponse(String r) {
                    runOnUiThread(() -> {
                        tvStatus.setText(getString(R.string.status_ok));
                        tvStatus.setTextColor(0xFF44FF88);
                        bridge.shutdown();
                    });
                }
                @Override public void onError(String e) {
                    runOnUiThread(() -> {
                        tvStatus.setText(getString(R.string.status_fail) + e);
                        tvStatus.setTextColor(0xFFFF4444);
                        bridge.shutdown();
                    });
                }
            });
        });

        btnClearHistory.setOnClickListener(v -> {
            ArizenAPIClient.clearHistory();
            tvStatus.setText("Chat history dibersihkan");
            tvStatus.setTextColor(0x88FFFFFF);
        });

        btnRamBoost.setOnClickListener(v -> {
            RamBoosterTool rbt = new RamBoosterTool(this);
            rbt.execute("boost");
            tvRamInfo.setText(rbt.getLastResult(this));
        });

        // Performance toggles
        swZram.setOnCheckedChangeListener((b, c) -> settings.setZramEnabled(c));
        swAnimSpeed.setOnCheckedChangeListener((b, c) -> settings.setAnimSpeedReduced(c));
        swBgLimit.setOnCheckedChangeListener((b, c) -> settings.setBgLimitEnabled(c));
        swAutoSpeak.setOnCheckedChangeListener((b, c) -> settings.setAutoSpeakEnabled(c));

        // Wake word toggle — starts/stops foreground service
        swWakeWord.setOnCheckedChangeListener((b, checked) -> {
            settings.setWakeWordEnabled(checked);
            Intent svc = new Intent(this, ArizenWakeService.class);
            if (checked) {
                startForegroundService(svc);
                tvStatus.setText("Hey Arizen aktif — mendengarkan di background");
                tvStatus.setTextColor(0xFF44FF88);
            } else {
                stopService(svc);
                tvStatus.setText("Hey Arizen dinonaktifkan");
                tvStatus.setTextColor(0x88FFFFFF);
            }
        });
    }

    private void updateAboutInfo() {
        tvAboutBuild.setText(
            "ArizenOS Lite 1.0  |  Zenith\n" +
            "Build: " + "20260528" +
            "  |  SM-T295\n" +
            "Android " + Build.VERSION.RELEASE +
            "  |  " + Build.SUPPORTED_ABIS[0] + "\n" +
            "by ArizenLabs"
        );
    }

    private void updateModelList(String provider) {
        String[] models;
        switch (provider) {
            case "groq":        models = new String[]{"llama-3.1-8b-instant","llama-3.3-70b-versatile","mixtral-8x7b-32768","gemma2-9b-it"}; break;
            case "together_ai": models = new String[]{"meta-llama/Llama-3-8b-chat-hf","mistralai/Mistral-7B-Instruct-v0.1"}; break;
            case "openai":      models = new String[]{"gpt-4o-mini","gpt-3.5-turbo","gpt-4o"}; break;
            case "cloudflare":  models = new String[]{"@cf/meta/llama-3-8b-instruct","@cf/mistral/mistral-7b-instruct-v0.1"}; break;
            default:            models = new String[]{"llama3","mistral","phi3","qwen2"};
        }
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, models);
        spModel.setAdapter(a);
        String saved = settings.getModel();
        for (int i = 0; i < models.length; i++)
            if (models[i].equals(saved)) { spModel.setSelection(i); break; }
    }

    @Override public void onBackPressed() { finish(); }
}
