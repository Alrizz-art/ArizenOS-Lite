package com.arizen.launcher.tools;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSearchTool implements ArizenTool {
    private final Context context;
    private List<String> lastResults = new ArrayList<>();

    public FileSearchTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        lastResults.clear();
        File root = Environment.getExternalStorageDirectory();
        searchFiles(root, params.toLowerCase(), lastResults, 0);
    }

    private void searchFiles(File dir, String query, List<String> results, int depth) {
        if (depth > 5 || results.size() >= 20) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().toLowerCase().contains(query)) {
                results.add(f.getAbsolutePath());
            }
            if (f.isDirectory() && !f.isHidden()) {
                searchFiles(f, query, results, depth + 1);
            }
        }
    }

    public List<String> getLastResults() { return lastResults; }

    @Override
    public String describe() {
        return "FileSearchTool: search files by name. Usage: [FileSearchTool:resume.pdf]";
    }
}
