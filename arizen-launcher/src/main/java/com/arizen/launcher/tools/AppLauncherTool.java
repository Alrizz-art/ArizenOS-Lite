package com.arizen.launcher.tools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class AppLauncherTool implements ArizenTool {
    private static final String TAG = "AppLauncherTool";
    private final Context context;

    public AppLauncherTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        // params = app name or package
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(params);

        if (launchIntent == null) {
            // Try by app name fuzzy match
            launchIntent = findByName(params, pm);
        }

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            Log.d(TAG, "Launched: " + params);
        } else {
            Log.w(TAG, "App not found: " + params);
        }
    }

    private Intent findByName(String name, PackageManager pm) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        for (android.content.pm.ResolveInfo info : pm.queryIntentActivities(mainIntent, 0)) {
            String label = info.loadLabel(pm).toString().toLowerCase();
            if (label.contains(name.toLowerCase())) {
                return pm.getLaunchIntentForPackage(info.activityInfo.packageName);
            }
        }
        return null;
    }

    @Override
    public String describe() {
        return "AppLauncherTool: launch an app by name or package. Usage: [AppLauncherTool:youtube]";
    }
}
