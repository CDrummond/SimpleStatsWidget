package de.simplestatswidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class SimpleStatsWidgetProvider extends AppWidgetProvider {
    private static long lastClick = 0;
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        long now = SystemClock.elapsedRealtime();

        // update widget data
        ComponentName thisWidget = new ComponentName(context, SimpleStatsWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        Intent intent = new Intent(context.getApplicationContext(), UpdateWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
        context.startService(intent);

        if ((now-lastClick)<1000) {
            Intent settingsIntent = new Intent (context, SettingsActivity.class);
            settingsIntent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
            settingsIntent.putExtra(SettingsActivity.UPDATE, true);
            context.startActivity(settingsIntent);
        }
        lastClick = now;
    }
}
