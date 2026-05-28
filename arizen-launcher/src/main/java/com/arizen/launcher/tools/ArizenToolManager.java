package com.arizen.launcher.tools;

import android.content.Context;
import java.util.LinkedHashMap;
import java.util.Map;

public class ArizenToolManager {
    private final Context context;
    private final Map<String, ArizenTool> tools;

    public ArizenToolManager(Context context) {
        this.context = context;
        this.tools = new LinkedHashMap<>();
        // Core OS tools
        tools.put("AppLauncherTool",  new AppLauncherTool(context));
        tools.put("SettingsTool",     new SettingsTool(context));
        tools.put("WifiTool",         new WifiTool(context));
        tools.put("BluetoothTool",    new BluetoothTool(context));
        tools.put("ClipboardTool",    new ClipboardTool(context));
        tools.put("BrowserTool",      new BrowserTool(context));
        tools.put("NotificationTool", new NotificationTool(context));
        tools.put("AutomationTool",   new AutomationTool(context));
        // System tools
        tools.put("RamBoosterTool",   new RamBoosterTool(context));
        tools.put("SystemInfoTool",   new SystemInfoTool(context));
        tools.put("BatteryTool",      new BatteryTool(context));
        tools.put("TimeTool",         new TimeTool(context));
        // Productivity tools
        tools.put("AlarmTool",        new AlarmTool(context));
        tools.put("CalculatorTool",   new CalculatorTool(context));
        tools.put("NotesTool",        new NotesTool(context));
        tools.put("FileSearchTool",   new FileSearchTool(context));
    }

    /**
     * Process AI response: execute all tool calls found, return cleaned text
     */
    public String processAndExecute(String aiResponse) {
        String cleaned = aiResponse;
        for (Map.Entry<String, ArizenTool> entry : tools.entrySet()) {
            String tag = "[" + entry.getKey() + ":";
            while (cleaned.contains(tag)) {
                int start = cleaned.indexOf(tag);
                int paramStart = start + tag.length();
                int end = cleaned.indexOf("]", paramStart);
                if (end < 0) break;
                String params = cleaned.substring(paramStart, end);
                entry.getValue().execute(params);
                // Remove tool call from display text
                cleaned = cleaned.substring(0, start) + cleaned.substring(end + 1);
            }
        }
        return cleaned.trim();
    }

    public String getToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        for (ArizenTool tool : tools.values()) {
            sb.append(tool.describe()).append("; ");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public <T extends ArizenTool> T getTool(Class<T> cls) {
        for (ArizenTool t : tools.values()) {
            if (cls.isInstance(t)) return (T) t;
        }
        return null;
    }
}
