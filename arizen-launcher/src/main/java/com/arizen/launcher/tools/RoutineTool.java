package com.arizen.launcher.tools;

import android.content.Context;
import com.arizen.launcher.ArizenRoutine;
import com.arizen.launcher.ArizenRoutineManager;

public class RoutineTool implements ArizenTool {
    private final Context context;
    public RoutineTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        // params: "run:NamaRutinitas" or "create:Spec" or "list"
        ArizenRoutineManager rm = new ArizenRoutineManager(context);
        if (params.startsWith("run:")) {
            String name = params.substring(4).trim();
            boolean found = rm.executeByNameOrTrigger(name, null);
            saveFeedback(found
                ? "Rutinitas \"" + name + "\" dijalankan."
                : "Rutinitas \"" + name + "\" tidak ditemukan.");
        } else if (params.startsWith("create:")) {
            String spec = params.substring(7).trim();
            ArizenRoutine r = rm.parseFromAI(spec);
            if (r != null) {
                rm.addRoutine(r);
                saveFeedback("Rutinitas \"" + r.name + "\" dibuat dengan " + r.steps.size() + " langkah.");
            } else {
                saveFeedback("Gagal parse rutinitas dari spec: " + spec);
            }
        } else if (params.equals("list")) {
            saveFeedback(rm.describeForAI());
        }
    }

    private void saveFeedback(String msg) {
        context.getSharedPreferences("arizen_routine_tool", Context.MODE_PRIVATE)
            .edit().putString("last", msg).apply();
    }

    public String getLastFeedback() {
        return context.getSharedPreferences("arizen_routine_tool", Context.MODE_PRIVATE)
            .getString("last", "");
    }

    @Override
    public String describe() {
        return "RoutineTool: manage and run automated routines. " +
            "Usage: [RoutineTool:run:Mode Pagi] or [RoutineTool:list] or " +
            "[RoutineTool:create:Nama|trigger|WifiTool:on|RamBoosterTool:boost]";
    }
}
