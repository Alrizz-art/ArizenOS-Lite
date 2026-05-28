package com.arizen.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.View;

/**
 * ArizenOS Lite — AI Settings Activity
 */
public class ArizenSettingsActivity extends Activity {

    private ArizenSettings settings;
    private EditText etApiKey;
    private Spinner spProvider;
    private Spinner spModel;
    private TextView tvStatus;

    private static final String[] PROVIDERS = {
        "groq", "together_ai", "openai", "cloudflare", "ollama"
    };
    private static final String[] PROVIDER_LABELS = {
        "Groq (Recommended — Free)",
        "Together AI (Free tier)",
        "OpenAI Compatible",
        "Cloudflare AI",
        "Ollama (Remote server)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = new ArizenSettings(this);

        etApiKey   = findViewById(R.id.et_api_key);
        spProvider = findViewById(R.id.sp_provider);
        spModel    = findViewById(R.id.sp_model);
        tvStatus   = findViewById(R.id.tv_status);
        Button btnSave = findViewById(R.id.btn_save);
        Button btnTest = findViewById(R.id.btn_test);

        // Provider spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, PROVIDER_LABELS);
        spProvider.setAdapter(adapter);

        // Load saved values
        String currentProvider = settings.getProvider();
        for (int i = 0; i < PROVIDERS.length; i++) {
            if (PROVIDERS[i].equals(currentProvider)) {
                spProvider.setSelection(i);
                break;
            }
        }

        spProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                settings.setProvider(PROVIDERS[pos]);
                updateModelList(PROVIDERS[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnSave.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            if (!key.isEmpty()) {
                settings.saveApiKey(key);
                settings.setProvider(PROVIDERS[spProvider.getSelectedItemPosition()]);
                tvStatus.setText("✓ Tersimpan");
                tvStatus.setTextColor(0xFF4CAF50);
            }
        });

        btnTest.setOnClickListener(v -> {
            tvStatus.setText("Menguji koneksi AI…");
            ArizenAIBridge bridge = new ArizenAIBridge(this);
            bridge.ask("Reply with exactly: OK", new ArizenAIBridge.AIResponseCallback() {
                @Override public void onResponse(String r) {
                    runOnUiThread(() -> {
                        tvStatus.setText("✓ Koneksi berhasil!");
                        tvStatus.setTextColor(0xFF4CAF50);
                        bridge.shutdown();
                    });
                }
                @Override public void onError(String e) {
                    runOnUiThread(() -> {
                        tvStatus.setText("✗ Gagal: " + e);
                        tvStatus.setTextColor(0xFFf44336);
                        bridge.shutdown();
                    });
                }
            });
        });
    }

    private void updateModelList(String provider) {
        String[] models;
        switch (provider) {
            case "groq":       models = new String[]{"llama-3.1-8b-instant","llama-3.3-70b-versatile","mixtral-8x7b-32768"}; break;
            case "together_ai":models = new String[]{"meta-llama/Llama-3-8b-chat-hf","mistralai/Mistral-7B-Instruct-v0.1"}; break;
            case "openai":     models = new String[]{"gpt-4o-mini","gpt-3.5-turbo","gpt-4o"}; break;
            case "cloudflare": models = new String[]{"@cf/meta/llama-3-8b-instruct","@cf/mistral/mistral-7b-instruct-v0.1"}; break;
            default:           models = new String[]{"llama3","mistral","phi3"};
        }
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, models);
        spModel.setAdapter(a);
        // Set saved model
        String saved = settings.getModel();
        for (int i = 0; i < models.length; i++) {
            if (models[i].equals(saved)) { spModel.setSelection(i); break; }
        }
    }
}
