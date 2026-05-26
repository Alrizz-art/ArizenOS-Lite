package com.arizen.launcher.tools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class BrowserTool implements ArizenTool {
    private final Context context;
    public BrowserTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        String url = params.startsWith("http") ? params : "https://www.google.com/search?q=" + Uri.encode(params);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public String describe() {
        return "BrowserTool: open a URL or search query. Usage: [BrowserTool:OpenAI news] or [BrowserTool:https://example.com]";
    }
}
