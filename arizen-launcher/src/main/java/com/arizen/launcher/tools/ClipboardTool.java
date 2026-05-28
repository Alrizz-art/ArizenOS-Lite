package com.arizen.launcher.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class ClipboardTool implements ArizenTool {
    private final Context context;
    public ClipboardTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        new Handler(Looper.getMainLooper()).post(() -> {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return;
            if (params.startsWith("copy:")) {
                String text = params.substring(5);
                cm.setPrimaryClip(ClipData.newPlainText("Arizen", text));
            }
        });
    }

    public String getClipboard() {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return "";
        ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
        return item != null ? item.coerceToText(context).toString() : "";
    }

    @Override
    public String describe() {
        return "ClipboardTool: copy text to clipboard. Usage: [ClipboardTool:copy:teks yang mau disalin]";
    }
}
