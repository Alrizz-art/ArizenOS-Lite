package com.arizen.launcher;

import android.content.Context;
import android.content.SharedPreferences;

public class ArizenSettings {
    private static final String PREFS = "arizen_prefs";
    private static final String KEY_API_KEY   = "api_key";
    private static final String KEY_PROVIDER  = "provider";
    private static final String KEY_MODEL     = "model";
    private static final String KEY_AMBIENCE  = "ambience_enabled";
    private static final String KEY_BASE_URL  = "base_url";

    private final SharedPreferences prefs;

    public ArizenSettings(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveApiKey(String key) { prefs.edit().putString(KEY_API_KEY, key).apply(); }
    public String getApiKey() { return prefs.getString(KEY_API_KEY, ""); }
    public boolean hasApiKey() { String k = getApiKey(); return k != null && !k.isEmpty(); }

    public void setProvider(String p) { prefs.edit().putString(KEY_PROVIDER, p).apply(); }
    public String getProvider() { return prefs.getString(KEY_PROVIDER, "groq"); }

    public void setModel(String m) { prefs.edit().putString(KEY_MODEL, m).apply(); }
    public String getModel() { return prefs.getString(KEY_MODEL, "llama-3.1-8b-instant"); }

    public void setBaseUrl(String url) { prefs.edit().putString(KEY_BASE_URL, url).apply(); }
    public String getBaseUrl() {
        String saved = prefs.getString(KEY_BASE_URL, "");
        if (saved != null && !saved.isEmpty()) return saved;
        switch (getProvider()) {
            case "groq":       return "https://api.groq.com/openai/v1";
            case "together_ai":return "https://api.together.xyz/v1";
            case "openai":     return "https://api.openai.com/v1";
            case "cloudflare": return "https://api.cloudflare.com/client/v4/accounts/YOUR_ACCOUNT_ID/ai/v1";
            case "ollama":     return "http://localhost:11434/v1";
            default:           return "https://api.groq.com/openai/v1";
        }
    }

    public void setAmbienceEnabled(boolean v) { prefs.edit().putBoolean(KEY_AMBIENCE, v).apply(); }
    public boolean isAmbienceEnabled() { return prefs.getBoolean(KEY_AMBIENCE, false); }
}
