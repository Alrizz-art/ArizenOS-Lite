package com.arizen.launcher;

import android.content.Context;
import android.content.SharedPreferences;

public class ArizenSettings {
    private static final String PREFS = "arizen_prefs";
    private final SharedPreferences prefs;

    public ArizenSettings(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ── AI Configuration ─────────────────────────────────────────────────────
    public void saveApiKey(String key)  { prefs.edit().putString("api_key", key).apply(); }
    public String getApiKey()           { return prefs.getString("api_key", ""); }
    public boolean hasApiKey()          { String k = getApiKey(); return k != null && !k.isEmpty(); }

    public void setProvider(String p)   { prefs.edit().putString("provider", p).apply(); }
    public String getProvider()         { return prefs.getString("provider", "groq"); }

    public void setModel(String m)      { prefs.edit().putString("model", m).apply(); }
    public String getModel()            { return prefs.getString("model", "llama-3.1-8b-instant"); }

    public void setBaseUrl(String url)  { prefs.edit().putString("base_url", url).apply(); }
    public String getBaseUrl() {
        String saved = prefs.getString("base_url", "");
        if (saved != null && !saved.isEmpty()) return saved;
        switch (getProvider()) {
            case "groq":        return "https://api.groq.com/openai/v1";
            case "together_ai": return "https://api.together.xyz/v1";
            case "openai":      return "https://api.openai.com/v1";
            case "cloudflare":  return "https://api.cloudflare.com/client/v4/accounts/YOUR_ACCOUNT/ai/v1";
            case "ollama":      return "http://localhost:11434/v1";
            default:            return "https://api.groq.com/openai/v1";
        }
    }

    // ── Voice ─────────────────────────────────────────────────────────────────
    public void setWakeWordEnabled(boolean v) { prefs.edit().putBoolean("wake_word", v).apply(); }
    public boolean isWakeWordEnabled()        { return prefs.getBoolean("wake_word", false); }

    public void setAutoSpeakEnabled(boolean v){ prefs.edit().putBoolean("auto_speak", v).apply(); }
    public boolean isAutoSpeakEnabled()       { return prefs.getBoolean("auto_speak", true); }

    // ── Ambient / Labs ────────────────────────────────────────────────────────
    public void setAmbienceEnabled(boolean v) { prefs.edit().putBoolean("ambience", v).apply(); }
    public boolean isAmbienceEnabled()        { return prefs.getBoolean("ambience", false); }

    // ── Performance ───────────────────────────────────────────────────────────
    public void setZramEnabled(boolean v)     { prefs.edit().putBoolean("zram", v).apply(); }
    public boolean isZramEnabled()            { return prefs.getBoolean("zram", true); }

    public void setAnimSpeedReduced(boolean v){ prefs.edit().putBoolean("anim_speed", v).apply(); }
    public boolean isAnimSpeedReduced()       { return prefs.getBoolean("anim_speed", true); }

    public void setBgLimitEnabled(boolean v)  { prefs.edit().putBoolean("bg_limit", v).apply(); }
    public boolean isBgLimitEnabled()         { return prefs.getBoolean("bg_limit", true); }

    /** Performance profile: "balanced" | "performance" | "saver" */
    public void setPerformanceProfile(String p) { prefs.edit().putString("perf_profile", p).apply(); }
    public String getPerformanceProfile()       { return prefs.getString("perf_profile", "balanced"); }

    // ── Workspace ─────────────────────────────────────────────────────────────
    public void setWorkspaceModeEnabled(boolean v) { prefs.edit().putBoolean("workspace_mode", v).apply(); }
    public boolean isWorkspaceModeEnabled()        { return prefs.getBoolean("workspace_mode", false); }

    /** Free-text workspace focus tag, e.g. "Coding", "Writing" */
    public void setWorkspaceFocus(String f) { prefs.edit().putString("workspace_focus", f).apply(); }
    public String getWorkspaceFocus()       { return prefs.getString("workspace_focus", ""); }

    // ── Command Palette ───────────────────────────────────────────────────────
    public void setCommandPaletteEnabled(boolean v) { prefs.edit().putBoolean("cmd_palette", v).apply(); }
    public boolean isCommandPaletteEnabled()        { return prefs.getBoolean("cmd_palette", true); }

    // ── UI / Launcher ─────────────────────────────────────────────────────────
    /** Grid columns per row: 4 or 5 */
    public void setGridColumns(int n) { prefs.edit().putInt("grid_cols", n).apply(); }
    public int getGridColumns()       { return prefs.getInt("grid_cols", 5); }

    /** Show live RAM/CPU stat bar on home screen */
    public void setShowStatBar(boolean v) { prefs.edit().putBoolean("stat_bar", v).apply(); }
    public boolean isShowStatBar()        { return prefs.getBoolean("stat_bar", true); }
}
