package com.arizen.launcher.tools;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

/**
 * ArizenOS Lite — Tool Manager
 * Routes AI responses to device control tools
 */
public class ArizenToolManager {

    private final Context context;
    private final Map<String, ArizenTool> tools;

    public ArizenToolManager(Context context) {
        this.context = context;
        this.tools = new HashMap<>();

        // Register all tools
        tools.put("AppLauncherTool", new AppLauncherTool(context));
        tools.put("NotificationTool", new NotificationTool(context));
        tools.put("WifiTool", new WifiTool(context));
        tools.put("BluetoothTool", new BluetoothTool(context));
        tools.put("ClipboardTool", new ClipboardTool(context));
        tools.put("BrowserTool", new BrowserTool(context));
        tools.put("FileSearchTool", new FileSearchTool(context));
        tools.put("SettingsTool", new SettingsTool(context));
        tools.put("AutomationTool", new AutomationTool(context));
    }

    public void processResponse(String aiResponse) {
        // Parse tool calls from AI response and execute
        for (Map.Entry<String, ArizenTool> entry : tools.entrySet()) {
            if (aiResponse.contains("[" + entry.getKey() + ":")) {
                String params = extractParams(aiResponse, entry.getKey());
                entry.getValue().execute(params);
            }
        }
    }

    public String getToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ArizenTool> entry : tools.entrySet()) {
            sb.append(entry.getValue().describe()).append("; ");
        }
        return sb.toString();
    }

    private String extractParams(String response, String toolName) {
        int start = response.indexOf("[" + toolName + ":") + toolName.length() + 2;
        int end = response.indexOf("]", start);
        return (end > start) ? response.substring(start, end) : "";
    }
}
