package com.arizen.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.widget.*;
import com.arizen.launcher.tools.RamBoosterTool;
import com.arizen.launcher.tools.SystemInfoTool;
import com.arizen.launcher.tools.BatteryTool;

public class ArizenSettingsActivity extends Activity {

    private ArizenSettings settings;

    // AI config
    private EditText etApiKey, etCustomUrl;
    private Spinner  spProvider, spModel;
    private TextView tvStatus, tvAboutBuild;

    // Performance toggles
    private Switch swZram, swAnimSpeed, swBgLimit;
    private TextView tvRamInfo;

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
        etApiKey    = findViewById(R.id.et_api_key);
        etCustomUrl = findViewById(R.id.et_custom_url);
        spProvider  = findViewById(R.id.sp_provider);
        spModel     = findViewById(R.id.sp_model);
        tvStatus    = findViewById(R.id.tv_status);
        tvAboutBuild= findViewById(R.id.tv_about_build);
        tvRamInfo   = findViewById(R.id.tv_ram_info);
        swZram      = findViewById(R.id.sw_zram);
        swAnimSpeed = findViewById(R.id.sw_anim_speed);
        swBgLimit   = findViewById(R.id.sw_bg_limit);

        // Provider spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, PROVIDER_LABELS);
        spProvider.setAdapter(adapter);
    }

    private void loadValues() {
        // Load API key (masked)
        String key = settings.getApiKey();
        if (!key.isEmpty()) {
            etApiKey.setHint("Key tersimpan: " + key.substring(0, Math.min(8, key.length())) + "****");
        }

        // Load provider selection
        String currentProvider = settings.getProvider();
        for (int i = 0; i < PROVIDERS.length; i++) {
            if (PROVIDERS[i].equals(currentProvider)) { spProvider.setSelection(i); break; }
        }

        // Load model list for current provider
        updateModelList(currentProvider);

        // Performance switches
        swZram.setChecked(settings.isZramEnabled());
        swAnimSpeed.setChecked(settings.isAnimSpeedReduced());
        swBgLimit.setChecked(settings.isBgLimitEnabled());
    }

    private void setupListeners() {
        // Provider change
        spProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                settings.setProvider(PROVIDERS[pos]);
                updateModelList(PROVIDERS[pos]);
                // Auto-fill base URL
                etCustomUrl.setHint(settings.getBaseUrl());
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Save AI config
        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            if (!key.isEmpty()) settings.saveApiKey(key);

            String selectedModel = (String) spModel.getSelectedItem();
            if (selectedModel != null) settings.setModel(selectedModel);

            String customUrl = etCustomUrl.getText().toString().trim();
            if (!customUrl.isEmpty()) settings.setBaseUrl(customUrl);

            tvStatus.setText(getString(R.string.status_saved));
            tvStatus.setTextColor(0xFF44FF88);
        });

        // Test AI connection
        Button btnTest = findViewById(R.id.btn_test);
        btnTest.setOnClickListener(v -> {
            tvStatus.setText("Menguji koneksi...");
            tvStatus.setTextColor(0xFFFF9F0A);
            ArizenAIBridge bridge = new ArizenAIBridge(this);
            bridge.ask("Reply with: ARIZEN_OK", new ArizenAIBridge.AIResponseCallback() {
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

        // Clear AI history
        Button btnClear = findViewById(R.id.btn_clear_history);
        btnClear.setOnClickListener(v -> {
            ArizenAPIClient.clearHistory();
            tvStatus.setText("Chat history dibersihkan");
            tvStatus.setTextColor(0xFF99FFFFFF);
        });

        // Performance switches
        swZram.setOnCheckedChangeListener((b, checked) -> settings.setZramEnabled(checked));
        swAnimSpeed.setOnCheckedChangeListener((b, checked) -> settings.setAnimSpeedReduced(checked));
        swBgLimit.setOnCheckedChangeListener((b, checked) -> settings.setBgLimitEnabled(checked));

        // RAM Boost button
        Button btnBoost = findViewById(R.id.btn_ram_boost);
        btnBoost.setOnClickListener(v -> {
            RamBoosterTool rbt = new RamBoosterTool(this);
            rbt.execute("boost");
            tvRamInfo.setText(rbt.getLastResult(this));
        });

        // Refresh RAM info
        updateRamInfo();
    }

    private void updateRamInfo() {
        SystemInfoTool si = new SystemInfoTool(this);
        tvRamInfo.setText(si.getInfo());
    }

    private void updateAboutInfo() {
        String buildDate = android.os.SystemProperties.get("ro.arizen.build.date", "20260528");
        String version   = android.os.SystemProperties.get("ro.arizen.version", "1.0");
        String codename  = android.os.SystemProperties.get("ro.arizen.codename", "Zenith");

        tvAboutBuild.setText(
            "ArizenOS Lite " + version + "  |  " + codename + "\n" +
            "Build: " + buildDate + "  |  Device: SM-T295\n" +
            "Android " + Build.VERSION.RELEASE + "  |  Kernel: " + System.getProperty("os.version","?") + "\n" +
            "by ArizenLabs"
        );
    }

    private void updateModelList(String provider) {
        String[] models;
        switch (provider) {
            case "groq":        models = new String[]{"llama-3.1-8b-instant","llama-3.3-70b-versatile","mixtral-8x7b-32768","gemma2-9b-it"}; break;
            case "together_ai": models = new String[]{"meta-llama/Llama-3-8b-chat-hf","mistralai/Mistral-7B-Instruct-v0.1","togethercomputer/alpaca-7b"}; break;
            case "openai":      models = new String[]{"gpt-4o-mini","gpt-3.5-turbo","gpt-4o"}; break;
            case "cloudflare":  models = new String[]{"@cf/meta/llama-3-8b-instruct","@cf/mistral/mistral-7b-instruct-v0.1"}; break;
            default:            models = new String[]{"llama3","mistral","phi3","qwen2"};
        }
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, models);
        spModel.setAdapter(a);
        String saved = settings.getModel();
        for (int i = 0; i < models.length; i++) {
            if (models[i].equals(saved)) { spModel.setSelection(i); break; }
        }
    }

    @Override public void onBackPressed() { finish(); }
}
