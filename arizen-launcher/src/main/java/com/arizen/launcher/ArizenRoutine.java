package com.arizen.launcher;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Single step in a routine */
class RoutineStep {
    public String tool;   // e.g. "AppLauncherTool"
    public String params; // e.g. "com.google.android.youtube"
    public String label;  // human-readable, e.g. "Buka YouTube"
    public int    delayAfterMs; // wait after executing this step (default 0)

    public RoutineStep(String tool, String params, String label, int delayAfterMs) {
        this.tool         = tool;
        this.params       = params;
        this.label        = label;
        this.delayAfterMs = delayAfterMs;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                .put("tool", tool)
                .put("params", params)
                .put("label", label)
                .put("delay", delayAfterMs);
        } catch (Exception e) { return new JSONObject(); }
    }

    public static RoutineStep fromJson(JSONObject o) {
        return new RoutineStep(
            o.optString("tool",""),
            o.optString("params",""),
            o.optString("label","Step"),
            o.optInt("delay", 0)
        );
    }
}

/** A Routine — named sequence of steps with optional wake phrase */
public class ArizenRoutine {
    public String id;          // UUID-ish
    public String name;        // e.g. "Mode Pagi"
    public String wakeTrigger; // custom voice trigger, e.g. "mode pagi", "" if none
    public String icon;        // emoji icon
    public String color;       // hex accent
    public List<RoutineStep> steps = new ArrayList<>();
    public long lastRunMs = 0;
    public int  runCount  = 0;
    public boolean enabled = true;

    public ArizenRoutine(String id, String name, String wakeTrigger, String icon, String color) {
        this.id          = id;
        this.name        = name;
        this.wakeTrigger = wakeTrigger;
        this.icon        = icon;
        this.color       = color;
    }

    public JSONObject toJson() {
        try {
            JSONArray stepsArr = new JSONArray();
            for (RoutineStep s : steps) stepsArr.put(s.toJson());
            return new JSONObject()
                .put("id", id)
                .put("name", name)
                .put("wake", wakeTrigger)
                .put("icon", icon)
                .put("color", color)
                .put("steps", stepsArr)
                .put("lastRun", lastRunMs)
                .put("runCount", runCount)
                .put("enabled", enabled);
        } catch (Exception e) { return new JSONObject(); }
    }

    public static ArizenRoutine fromJson(JSONObject o) {
        ArizenRoutine r = new ArizenRoutine(
            o.optString("id",""),
            o.optString("name","Routine"),
            o.optString("wake",""),
            o.optString("icon","⚡"),
            o.optString("color","#4A9EFF")
        );
        r.lastRunMs = o.optLong("lastRun", 0);
        r.runCount  = o.optInt("runCount", 0);
        r.enabled   = o.optBoolean("enabled", true);
        JSONArray arr = o.optJSONArray("steps");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject so = arr.optJSONObject(i);
                if (so != null) r.steps.add(RoutineStep.fromJson(so));
            }
        }
        return r;
    }

    // --- Built-in preset routines ------------------------------------------------
    public static List<ArizenRoutine> getPresets() {
        List<ArizenRoutine> presets = new ArrayList<>();

        // 1. Mode Pagi
        ArizenRoutine morning = new ArizenRoutine("preset_morning", "Mode Pagi",
            "mode pagi", "🌅", "#FF9F0A");
        morning.steps.add(new RoutineStep("WifiTool",      "on",       "Aktifkan WiFi",        500));
        morning.steps.add(new RoutineStep("BluetoothTool", "off",      "Matikan Bluetooth",    200));
        morning.steps.add(new RoutineStep("AlarmTool",     "07:00:Bangun!", "Set alarm 07:00", 500));
        morning.steps.add(new RoutineStep("SettingsTool",  "sound",    "Buka Sound Settings",  0));
        presets.add(morning);

        // 2. Mode Malam
        ArizenRoutine night = new ArizenRoutine("preset_night", "Mode Malam",
            "mode malam", "🌙", "#BF5AF2");
        night.steps.add(new RoutineStep("WifiTool",        "off",      "Matikan WiFi",         300));
        night.steps.add(new RoutineStep("BluetoothTool",   "off",      "Matikan Bluetooth",    300));
        night.steps.add(new RoutineStep("SettingsTool",    "sound",    "Buka Sound",           0));
        presets.add(night);

        // 3. Mode Fokus
        ArizenRoutine focus = new ArizenRoutine("preset_focus", "Mode Fokus",
            "mode fokus", "🎯", "#00D4AA");
        focus.steps.add(new RoutineStep("WifiTool",        "off",      "Matikan WiFi",         300));
        focus.steps.add(new RoutineStep("BluetoothTool",   "off",      "Matikan Bluetooth",    300));
        focus.steps.add(new RoutineStep("NotificationTool","silent",   "Mode Senyap",          200));
        focus.steps.add(new RoutineStep("AlarmTool",       "25:00:Pomodoro selesai!", "Timer Pomodoro 25m", 0));
        presets.add(focus);

        // 4. Mode Gaming
        ArizenRoutine gaming = new ArizenRoutine("preset_gaming", "Mode Gaming",
            "mode gaming", "🎮", "#FF453A");
        gaming.steps.add(new RoutineStep("WifiTool",       "on",       "Aktifkan WiFi",        300));
        gaming.steps.add(new RoutineStep("BluetoothTool",  "off",      "Matikan Bluetooth",    300));
        gaming.steps.add(new RoutineStep("NotificationTool","silent",  "Mode Senyap",          200));
        gaming.steps.add(new RoutineStep("RamBoosterTool", "boost",    "Boost RAM",            500));
        presets.add(gaming);

        // 5. Mode Kerja
        ArizenRoutine work = new ArizenRoutine("preset_work", "Mode Kerja",
            "mode kerja", "💼", "#4A9EFF");
        work.steps.add(new RoutineStep("WifiTool",         "on",       "Aktifkan WiFi",        300));
        work.steps.add(new RoutineStep("BluetoothTool",    "off",      "Matikan Bluetooth",    200));
        work.steps.add(new RoutineStep("RamBoosterTool",   "boost",    "Boost RAM",            400));
        work.steps.add(new RoutineStep("AppLauncherTool",  "com.google.android.gm", "Buka Gmail", 0));
        presets.add(work);

        return presets;
    }
}
