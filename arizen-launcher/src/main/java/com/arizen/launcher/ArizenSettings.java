package com.arizen.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * ArizenOS Lite — Secure Settings Storage
 * Stores API keys encrypted using AES
 */
public class ArizenSettings {
    private static final String PREFS_NAME = "arizen_secure";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_MODEL = "model";
    private static final String KEY_BASE_URL = "base_url";
    private static final String CIPHER_KEY = "ArizenOS2024Lite";

    private final SharedPreferences prefs;

    public ArizenSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, encrypt(apiKey)).apply();
    }

    public String getApiKey() {
        String encrypted = prefs.getString(KEY_API_KEY, null);
        return (encrypted != null) ? decrypt(encrypted) : null;
    }

    public void setProvider(String provider) {
        prefs.edit().putString(KEY_PROVIDER, provider).apply();
        // Set default base URL for known providers
        switch (provider) {
            case "groq":
                setBaseUrl("https://api.groq.com/openai/v1");
                setModel("llama-3.1-8b-instant");
                break;
            case "together_ai":
                setBaseUrl("https://api.together.xyz/v1");
                setModel("meta-llama/Llama-3-8b-chat-hf");
                break;
            case "openai":
                setBaseUrl("https://api.openai.com/v1");
                setModel("gpt-4o-mini");
                break;
            case "cloudflare":
                setBaseUrl("https://api.cloudflare.com/client/v4/accounts/YOUR_ACCOUNT_ID/ai/v1");
                setModel("@cf/meta/llama-3-8b-instruct");
                break;
        }
    }

    public String getProvider() { return prefs.getString(KEY_PROVIDER, "groq"); }
    public void setBaseUrl(String url) { prefs.edit().putString(KEY_BASE_URL, url).apply(); }
    public String getBaseUrl() { return prefs.getString(KEY_BASE_URL, "https://api.groq.com/openai/v1"); }
    public void setModel(String model) { prefs.edit().putString(KEY_MODEL, model).apply(); }
    public String getModel() { return prefs.getString(KEY_MODEL, "llama-3.1-8b-instant"); }

    private String encrypt(String data) {
        try {
            SecretKeySpec key = new SecretKeySpec(CIPHER_KEY.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.encodeToString(cipher.doFinal(data.getBytes("UTF-8")), Base64.NO_WRAP);
        } catch (Exception e) { return data; }
    }

    private String decrypt(String data) {
        try {
            SecretKeySpec key = new SecretKeySpec(CIPHER_KEY.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), "UTF-8");
        } catch (Exception e) { return data; }
    }
}
