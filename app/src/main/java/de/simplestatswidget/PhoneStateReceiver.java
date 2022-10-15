package de.simplestatswidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.intent.action.PHONE_STATE".equals(intent.getAction())) {
            return;
        }
        String state = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            ComponentName thisWidget = new ComponentName(context, SimpleStatsWidgetProvider.class);
            int[] allWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(thisWidget);
            Intent serviceIntent = new Intent(context.getApplicationContext(), UpdateWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
            context.startService(serviceIntent);
        }
    }
}
