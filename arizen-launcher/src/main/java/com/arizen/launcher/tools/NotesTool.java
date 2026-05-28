package com.arizen.launcher.tools;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotesTool implements ArizenTool {
    private final Context context;
    private static final String PREFS = "arizen_notes";

    public NotesTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        // params: "save:content" or "list" or "clear"
        if (params.startsWith("save:")) {
            saveNote(params.substring(5).trim());
        } else if (params.equals("list")) {
            // result stored in prefs for UI
        } else if (params.equals("clear")) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        }
    }

    public void saveNote(String content) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String existing = prefs.getString("notes_json", "[]");
        try {
            JSONArray arr = new JSONArray(existing);
            JSONObject note = new JSONObject();
            note.put("content", content);
            note.put("time", new SimpleDateFormat("dd MMM HH:mm", new Locale("id","ID")).format(new Date()));
            arr.put(note);
            // Keep last 50 notes
            if (arr.length() > 50) arr.remove(0);
            prefs.edit().putString("notes_json", arr.toString()).apply();
        } catch (Exception e) { /* ignore */ }
    }

    public String getNotesList() {
        String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("notes_json", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) return "Belum ada catatan.";
            StringBuilder sb = new StringBuilder();
            for (int i = arr.length()-1; i >= Math.max(0, arr.length()-10); i--) {
                JSONObject n = arr.getJSONObject(i);
                sb.append("[").append(n.optString("time")).append("] ")
                  .append(n.optString("content")).append("\n");
            }
            return sb.toString();
        } catch (Exception e) { return "Error membaca catatan."; }
    }

    @Override
    public String describe() {
        return "NotesTool: save or list quick notes. Usage: [NotesTool:save:catatan kamu] or [NotesTool:list]";
    }
}
