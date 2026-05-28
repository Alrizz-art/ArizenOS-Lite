package com.arizen.launcher.tools;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeTool implements ArizenTool {
    private final Context context;
    public TimeTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {}

    public String getCurrentDateTime() {
        Date now = new Date();
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now);
        String date = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id","ID")).format(now);
        return "Waktu: " + time + "\nTanggal: " + date;
    }

    @Override
    public String describe() {
        return "TimeTool: get current date and time. Usage: [TimeTool:now]";
    }
}
