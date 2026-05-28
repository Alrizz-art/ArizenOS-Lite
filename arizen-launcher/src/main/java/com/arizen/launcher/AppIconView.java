package com.arizen.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * ArizenOS Lite — AppIconView
 * Single app icon tile for the home screen grid
 */
public class AppIconView extends LinearLayout {

    public AppIconView(Context context, ResolveInfo info, PackageManager pm) {
        super(context);

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);

        int dp = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());

        int iconDp = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 52, context.getResources().getDisplayMetrics());

        setPadding(dp * 2, dp * 2, dp * 2, dp * 2);

        LayoutParams params = new LayoutParams(
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80,
                context.getResources().getDisplayMetrics()),
            ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp, dp, dp, dp);
        setLayoutParams(params);

        // App icon
        ImageView icon = new ImageView(context);
        LayoutParams iconParams = new LayoutParams(iconDp, iconDp);
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        icon.setLayoutParams(iconParams);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        try {
            Drawable drawable = info.loadIcon(pm);
            icon.setImageDrawable(drawable);
        } catch (Exception e) {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // App label
        TextView label = new TextView(context);
        label.setLayoutParams(new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        label.setTextColor(0xFFFFFFFF);
        try {
            label.setText(info.loadLabel(pm));
        } catch (Exception e) {
            label.setText("App");
        }

        addView(icon);
        addView(label);

        // Launch app on click
        setOnClickListener(v -> {
            try {
                Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(info.activityInfo.packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                } else {
                    Toast.makeText(context, "Cannot open " + info.loadLabel(pm),
                        Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(context, "Failed to launch app", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
