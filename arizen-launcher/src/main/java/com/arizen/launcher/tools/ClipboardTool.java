package com.arizen.launcher.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardTool implements ArizenTool {
    private final Context context;
    public ClipboardTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        ClipboardManager cm =
            (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;
        if (params.startsWith("copy:")) {
            String text = params.substring(5);
            cm.setPrimaryClip(ClipData.newPlainText("Arizen", text));
        } else if (params.equals("read")) {
            ClipData clip = cm.getPrimaryClip();
            // Result available via ArizenAIBridge callback
        }
    }

    @Override
    public String describe() {
        return "ClipboardTool: copy text or read clipboard. Usage: [ClipboardTool:copy:hello world] | [ClipboardTool:read]";
    }
}
