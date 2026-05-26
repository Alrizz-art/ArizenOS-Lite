package com.arizen.launcher.tools;

import android.content.Context;
import android.net.wifi.WifiManager;

public class WifiTool implements ArizenTool {
    private final Context context;
    public WifiTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wm == null) return;
        boolean enable = params.equalsIgnoreCase("on") || params.equalsIgnoreCase("enable");
        wm.setWifiEnabled(enable);
    }

    @Override
    public String describe() {
        return "WifiTool: toggle WiFi. Usage: [WifiTool:on] or [WifiTool:off]";
    }
}
