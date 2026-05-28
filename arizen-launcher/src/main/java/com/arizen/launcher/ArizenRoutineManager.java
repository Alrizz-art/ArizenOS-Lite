package com.arizen.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.arizen.launcher.tools.ArizenToolManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ArizenOS Lite — Routine Manager
 * Handles CRUD, persistence, execution, history, and AI-triggered routines.
 */
public class ArizenRoutineManager {

    private static final String TAG       = "ArizenRoutines";
    private static final String PREFS     = "arizen_routines";
    private static final String KEY_LIST  = "routines_json";
    private static final String KEY_LOG   = "run_log";

    private final Context context;
    private final SharedPreferences prefs;
    private final ArizenToolManager toolManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<ArizenRoutine> routines = new ArrayList<>();

    /** Callback for step-by-step execution progress */
    public interface ExecutionCallback {
        void onStepStart(int index, RoutineStep step);
        void onStepDone(int index, RoutineStep step);
        void onFinished(ArizenRoutine routine);
        void onError(int index, String msg);
    }

    public ArizenRoutineManager(Context ctx) {
        this.context     = ctx;
        this.prefs       = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.toolManager = new ArizenToolManager(ctx);
        load();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────
    public void addRoutine(ArizenRoutine r) {
        // Ensure unique ID
        for (ArizenRoutine existing : routines)
            if (existing.id.equals(r.id)) { existing.name = r.name; existing.steps = r.steps;
                existing.wakeTrigger = r.wakeTrigger; save(); return; }
        routines.add(r);
        save();
    }

    public void removeRoutine(String id) {
        routines.removeIf(r -> r.id.equals(id));
        save();
    }

    public List<ArizenRoutine> getAll() { return Collections.unmodifiableList(routines); }

    public ArizenRoutine getById(String id) {
        for (ArizenRoutine r : routines) if (r.id.equals(id)) return r;
        return null;
    }

    /** Find routine by wake trigger phrase (fuzzy) */
    public ArizenRoutine findByTrigger(String phrase) {
        if (phrase == null) return null;
        String lower = phrase.toLowerCase().trim();
        for (ArizenRoutine r : routines) {
            if (!r.enabled || r.wakeTrigger == null || r.wakeTrigger.isEmpty()) continue;
            if (lower.contains(r.wakeTrigger.toLowerCase())) return r;
        }
        return null;
    }

    /** Load preset routines if list is empty */
    public void loadPresetsIfEmpty() {
        if (routines.isEmpty()) {
            for (ArizenRoutine preset : ArizenRoutine.getPresets()) addRoutine(preset);
        }
    }

    // ── Execution ─────────────────────────────────────────────────────────
    /**
     * Execute all steps in a routine sequentially, with per-step delays.
     * Runs on a background thread; callbacks dispatched to main thread.
     */
    public void execute(ArizenRoutine routine, ExecutionCallback cb) {
        if (routine == null || routine.steps.isEmpty()) return;
        Log.i(TAG, "Executing routine: " + routine.name);

        new Thread(() -> {
            for (int i = 0; i < routine.steps.size(); i++) {
                final int index = i;
                final RoutineStep step = routine.steps.get(i);
                if (cb != null) mainHandler.post(() -> cb.onStepStart(index, step));

                try {
                    // Execute via tool manager on main thread for tools that need it
                    mainHandler.post(() -> {
                        try {
                            toolManager.processAndExecute("[" + step.tool + ":" + step.params + "]");
                        } catch (Exception e) {
                            Log.e(TAG, "Step error: " + e.getMessage());
                            if (cb != null) cb.onError(index, e.getMessage());
                        }
                    });
                    // Wait for step to settle + configured delay
                    Thread.sleep(Math.max(300, step.delayAfterMs));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (cb != null) mainHandler.post(() -> cb.onStepDone(index, step));
            }

            // Update stats
            routine.lastRunMs = System.currentTimeMillis();
            routine.runCount++;
            save();
            appendLog(routine.name, routine.steps.size());

            if (cb != null) mainHandler.post(() -> cb.onFinished(routine));
            Log.i(TAG, "Routine complete: " + routine.name);
        }).start();
    }

    /** Quick execute by name or trigger (for AI tool + wake service) */
    public boolean executeByNameOrTrigger(String query, ExecutionCallback cb) {
        if (query == null) return false;
        String lower = query.toLowerCase().trim();
        // Try exact name first
        for (ArizenRoutine r : routines) {
            if (r.enabled && r.name.toLowerCase().equals(lower)) { execute(r, cb); return true; }
        }
        // Try trigger
        ArizenRoutine byTrigger = findByTrigger(query);
        if (byTrigger != null) { execute(byTrigger, cb); return true; }
        // Fuzzy name match
        for (ArizenRoutine r : routines) {
            if (r.enabled && r.name.toLowerCase().contains(lower)) { execute(r, cb); return true; }
        }
        return false;
    }

    // ── AI-driven routine creation ────────────────────────────────────────
    /**
     * Parse a routine from AI-generated text like:
     * [CreateRoutine:Mode Gaming|mode gaming|WifiTool:on|RamBoosterTool:boost]
     */
    public ArizenRoutine parseFromAI(String spec) {
        try {
            String[] parts = spec.split("\\|");
            String name    = parts.length > 0 ? parts[0].trim() : "Routine Baru";
            String trigger = parts.length > 1 ? parts[1].trim() : "";
            String id = "ai_" + System.currentTimeMillis();
            ArizenRoutine r = new ArizenRoutine(id, name, trigger, "🤖", "#4A9EFF");
            for (int i = 2; i < parts.length; i++) {
                String[] kv = parts[i].split(":", 2);
                String tool   = kv.length > 0 ? kv[0].trim() : "";
                String params = kv.length > 1 ? kv[1].trim() : "";
                r.steps.add(new RoutineStep(tool, params, tool.replace("Tool",""), 400));
            }
            return r;
        } catch (Exception e) { return null; }
    }

    // ── Run Log ───────────────────────────────────────────────────────────
    private void appendLog(String name, int steps) {
        String time = new SimpleDateFormat("dd MMM HH:mm", new Locale("id","ID")).format(new Date());
        String existing = prefs.getString(KEY_LOG, "");
        String entry = "[" + time + "] " + name + " (" + steps + " langkah)\n";
        String updated = entry + existing;
        // Keep last 50 lines
        String[] lines = updated.split("\n");
        if (lines.length > 50) updated = String.join("\n", Arrays.copyOf(lines, 50));
        prefs.edit().putString(KEY_LOG, updated).apply();
    }

    public String getRunLog() {
        String log = prefs.getString(KEY_LOG, "");
        return log.isEmpty() ? "Belum ada rutinitas yang dijalankan." : log;
    }

    // ── Persistence ───────────────────────────────────────────────────────
    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (ArizenRoutine r : routines) arr.put(r.toJson());
            prefs.edit().putString(KEY_LIST, arr.toString()).apply();
        } catch (Exception e) { Log.e(TAG, "Save error: " + e.getMessage()); }
    }

    private void load() {
        routines.clear();
        try {
            String json = prefs.getString(KEY_LIST, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) routines.add(ArizenRoutine.fromJson(o));
            }
        } catch (Exception e) { Log.e(TAG, "Load error: " + e.getMessage()); }
    }

    // ── Summary for AI ────────────────────────────────────────────────────
    public String describeForAI() {
        if (routines.isEmpty()) return "Belum ada rutinitas.";
        StringBuilder sb = new StringBuilder("Rutinitas tersimpan:\n");
        for (ArizenRoutine r : routines) {
            sb.append("• ").append(r.name);
            if (!r.wakeTrigger.isEmpty()) sb.append(" (trigger: '").append(r.wakeTrigger).append("')");
            sb.append(" — ").append(r.steps.size()).append(" langkah\n");
        }
        return sb.toString();
    }
}
