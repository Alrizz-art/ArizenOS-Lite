package com.arizen.launcher.tools;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class BluetoothTool implements ArizenTool {
    private final Context context;
    public BluetoothTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null) return;
        if (params.equalsIgnoreCase("on") || params.equalsIgnoreCase("enable")) {
            bt.enable();
        } else if (params.equalsIgnoreCase("off") || params.equalsIgnoreCase("disable")) {
            bt.disable();
        }
    }

    @Override
    public String describe() {
        return "BluetoothTool: toggle Bluetooth. Usage: [BluetoothTool:on] or [BluetoothTool:off]";
    }
}
