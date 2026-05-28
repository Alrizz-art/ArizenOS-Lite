package com.arizen.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.arizen.launcher.tools.*;
import java.util.*;

public class ArizenCommandPaletteActivity extends Activity {

    private EditText etCommand;
    private LinearLayout listContainer;
    private TextView tvHint;

    private static final String[][] BUILTIN_COMMANDS = {
        {">ai",         "Buka Arizen AI",           "assistant"},
        {">labs",       "Buka ArizenLabs",           "labs"},
        {">settings",   "Buka Arizen Settings",      "settings"},
        {">routines",   "Buka Routines",             "routines"},
        {">monitor",    "System Monitor",            "monitor"},
        {">workspace",  "Mode Workspace",            "workspace"},
        {">ram",        "Boost RAM sekarang",        "ram_boost"},
        {">sysinfo",    "Info sistem device",        "sysinfo"},
        {">battery",    "Status baterai detail",     "battery"},
        {">wifi on",    "Aktifkan WiFi",             "wifi_on"},
        {">wifi off",   "Matikan WiFi",              "wifi_off"},
        {">bt on",      "Aktifkan Bluetooth",        "bt_on"},
        {">bt off",     "Matikan Bluetooth",         "bt_off"},
        {">calc",       "Kalkulator cepat",          "calc"},
        {">note",       "Buat catatan baru",         "note"},
        {">alarm",      "Set alarm",                 "alarm"},
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_palette);

        etCommand     = findViewById(R.id.et_command);
        listContainer = findViewById(R.id.list_container);
        tvHint        = findViewById(R.id.tv_hint);

        setupInput();
        renderCommands("");

        etCommand.requestFocus();
    }

    private void setupInput() {
        etCommand.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                renderCommands(s.toString().trim().toLowerCase(Locale.ROOT));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String text = etCommand.getText().toString().trim();
                if (!text.isEmpty()) executeCommand(text);
                return true;
            }
            return false;
        });

        TextView btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.fade_out);
        });
    }

    private void renderCommands(String query) {
        listContainer.removeAllViews();

        List<String[]> results = new ArrayList<>();

        if (query.isEmpty() || query.startsWith(">")) {
            for (String[] cmd : BUILTIN_COMMANDS) {
                if (query.isEmpty() || cmd[0].startsWith(query) || cmd[1].toLowerCase(Locale.ROOT).contains(query)) {
                    results.add(cmd);
                }
            }
        }

        if (!query.startsWith(">") && !query.isEmpty()) {
            PackageManager pm = getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo ri : apps) {
                String appName = ri.loadLabel(pm).toString();
                if (appName.toLowerCase(Locale.ROOT).contains(query)) {
                    results.add(new String[]{"app:" + ri.activityInfo.packageName, appName, "launch_app"});
                    if (results.size() >= 8) break;
                }
            }
        }

        if (results.isEmpty()) {
            tvHint.setText("Tidak ditemukan — tekan Enter untuk tanya AI");
            tvHint.setVisibility(View.VISIBLE);
        } else {
            tvHint.setVisibility(View.GONE);
        }

        for (String[] cmd : results) {
            addCommandRow(cmd);
        }
    }

    private void addCommandRow(String[] cmd) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(14));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(getDrawable(R.drawable.cmd_row_bg));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(1));
        row.setLayoutParams(rowParams);

        View dot = new View(this);
        int dotColor = cmd[2].startsWith("launch_app") ? 0xFF00D4AA :
                       cmd[2].startsWith("ram") || cmd[2].startsWith("sys") ? 0xFF44FF88 :
                       cmd[2].startsWith("wifi") || cmd[2].startsWith("bt") ? 0xFF4A9EFF :
                       cmd[2].equals("assistant") ? 0xFF4A9EFF :
                       cmd[2].equals("monitor") ? 0xFFFF9F0A : 0xFF888888;
        dot.setBackgroundColor(dotColor);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(6), dpToPx(6));
        dotParams.setMargins(0, 0, dpToPx(14), 0);
        dot.setLayoutParams(dotParams);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(cmd[1]);
        tvLabel.setTextColor(0xFFFFFFFF);
        tvLabel.setTextSize(14);

        TextView tvCmd = new TextView(this);
        tvCmd.setText(cmd[0]);
        tvCmd.setTextColor(dotColor & 0x99FFFFFF);
        tvCmd.setTextSize(10);
        tvCmd.setTypeface(android.graphics.Typeface.MONOSPACE);

        textCol.addView(tvLabel);
        textCol.addView(tvCmd);

        row.addView(dot);
        row.addView(textCol);

        row.setOnClickListener(v -> executeCommand(cmd[2].startsWith("launch_app") ? cmd[0] : cmd[2]));
        listContainer.addView(row);
    }

    private void executeCommand(String cmd) {
        switch (cmd) {
            case "assistant":
                startActivity(new Intent(this, ArizenAssistantActivity.class));
                break;
            case "labs":
                startActivity(new Intent(this, ArizenLabsActivity.class));
                break;
            case "settings":
                startActivity(new Intent(this, ArizenSettingsActivity.class));
                break;
            case "routines":
                startActivity(new Intent(this, ArizenRoutinesActivity.class));
                break;
            case "monitor":
                startActivity(new Intent(this, ArizenSystemMonitorActivity.class));
                break;
            case "workspace":
                startActivity(new Intent(this, ArizenWorkspaceActivity.class));
                break;
            case "ram_boost":
                RamBoosterTool rbt = new RamBoosterTool(this);
                rbt.execute("boost");
                showToast("RAM boost: " + rbt.getLastResult(this));
                finish();
                break;
            case "sysinfo":
                showToast(new SystemInfoTool(this).getInfo());
                break;
            case "battery":
                BatteryTool bt = new BatteryTool(this);
                bt.execute("");
                showToast(bt.getInfo());
                break;
            case "wifi_on":
                new WifiTool(this).execute("on");
                showToast("WiFi diaktifkan");
                finish();
                break;
            case "wifi_off":
                new WifiTool(this).execute("off");
                showToast("WiFi dimatikan");
                finish();
                break;
            case "bt_on":
                new BluetoothTool(this).execute("on");
                showToast("Bluetooth diaktifkan");
                finish();
                break;
            case "bt_off":
                new BluetoothTool(this).execute("off");
                showToast("Bluetooth dimatikan");
                finish();
                break;
            case "calc":
                finish();
                startActivity(new Intent(this, ArizenLabsActivity.class));
                break;
            case "note":
                finish();
                Intent noteIntent = new Intent(this, ArizenLabsActivity.class);
                noteIntent.putExtra("action", "note");
                startActivity(noteIntent);
                break;
            case "alarm":
                finish();
                Intent alarmIntent = new Intent(this, ArizenLabsActivity.class);
                alarmIntent.putExtra("action", "alarm");
                startActivity(alarmIntent);
                break;
            default:
                if (cmd.startsWith("app:")) {
                    String pkg = cmd.substring(4);
                    Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (launch != null) startActivity(launch);
                    finish();
                } else {
                    Intent aiIntent = new Intent(this, ArizenAssistantActivity.class);
                    aiIntent.putExtra("prefill", cmd);
                    startActivity(aiIntent);
                    finish();
                }
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override public void onBackPressed() {
        finish();
        overridePendingTransition(0, R.anim.fade_out);
    }
}
