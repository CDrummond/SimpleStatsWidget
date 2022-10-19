package de.simplestatswidget;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class UpdateWidgetService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // check permissions and get data
        String smsText;
        String callText;
        if ((checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) &&
                (checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) &&
                (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)) {
            smsText = String.valueOf(getSmsCount());
            callText = getCalls();
        } else {
            smsText = null;
            callText = null;
        }

        // update widgets with new data
        updateWidgets(intent, smsText, callText);
        stopSelf();

        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateWidgets(Intent intent, String smsText, String callText) {
        // get ids
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
                .getApplicationContext());
        int[] allWidgetIds = intent
                .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

        // update all widgets
        for (int widgetId : allWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(this
                    .getApplicationContext().getPackageName(),
                    R.layout.widget_layout);
            // set data
            if ((smsText == null) && (callText == null)) {
                remoteViews.setViewVisibility(R.id.widget_sms_header, View.GONE);
                remoteViews.setTextViewText(R.id.sms_text, "missing");
                remoteViews.setViewVisibility(R.id.widget_calls_header, View.GONE);
                remoteViews.setTextViewText(R.id.calls_text, "permissions");
            } else {
                if (smsText != null) {
                    // set text color
                    if (smsText.startsWith("-")) {
                        remoteViews.setTextColor(R.id.sms_text, Color.RED);
                    } else {
                        remoteViews.setTextColor(R.id.sms_text, Color.WHITE);
                    }
                    remoteViews.setTextViewText(R.id.sms_text, smsText);
                }
                if (callText != null) {
                    // set text color
                    if (callText.startsWith("- ")) {
                        remoteViews.setTextColor(R.id.calls_text, Color.RED);
                    } else {
                        remoteViews.setTextColor(R.id.calls_text, Color.WHITE);
                    }
                    remoteViews.setTextViewText(R.id.calls_text, callText);
                }
            }

            // register an onClickListener
            Intent clickIntent = new Intent(this.getApplicationContext(),
                    SimpleStatsWidgetProvider.class);

            clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    allWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(), 0, clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private Date getStartDate(SharedPreferences prefs) {
        int billingStart = Integer.parseInt(prefs.getString("startOfBillingPeriod", "1"));
        Calendar calendar = Calendar.getInstance();
        Calendar gc = new GregorianCalendar(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), billingStart);
        if (gc.getTime().getTime()>calendar.getTime().getTime()) {
            gc.add(Calendar.MONTH, -1);
        }
        return gc.getTime();
    }

    private int getSmsCount() {
        // get settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int smsCharCount = Integer.parseInt(prefs.getString("smsCharCount", "160"));
        Date startDate = getStartDate(prefs);

        // get count of sent sms for this month
        Uri sentMessage = Uri.parse("content://sms/sent/");
        ContentResolver cr = this.getContentResolver();
        int smsCount = 0;
        Cursor c = cr.query(sentMessage, null, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                Date date = new Date(Long.valueOf(c.getString(c.getColumnIndexOrThrow("date"))));
                if (date.after(startDate)) {
                    String message = c.getString(c.getColumnIndexOrThrow("body"));
                    smsCount += (int) Math.round((double) message.length() / smsCharCount + 0.5);
                }
            }
            c.close();
        }

        // count sms reverse if checked in preferences
        boolean countReverse = prefs.getBoolean("countReverse", false);
        if (countReverse) {
            int smsMax = Integer.parseInt(prefs.getString("smsMax", "0"));
            smsCount = smsMax - smsCount;
        }

        return smsCount;
    }

    private String getCalls() {
        // get settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean round = prefs.getBoolean("round", false);
        Date startDate = getStartDate(prefs);

        // get duration of outgoing calls for this month
        Uri allCalls = Uri.parse("content://call_log/calls");
        ContentResolver cr = this.getContentResolver();
        int callDuration = 0;
        Cursor c = cr.query(allCalls, null, "type = " + CallLog.Calls.OUTGOING_TYPE, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                Date date = new Date(Long.valueOf(c.getString(c.getColumnIndexOrThrow("date"))));
                if (date.after(startDate)) {
                    int duration = Integer.parseInt(c.getString(c.getColumnIndexOrThrow("duration")));
                    if (duration > 0) {
                        if (round) {
                            int minutes = duration/60;
                            callDuration += (minutes * 60) + (duration%60>0 ? 60 : 0);
                        } else {
                            callDuration += duration;
                        }
                    }
                }
            }
            c.close();
        }

        // count calls reverse if checked in preferences
        boolean countReverse = prefs.getBoolean("countReverse", false);
        boolean negative = false;
        if (countReverse) {
            int callMax = Integer.parseInt(prefs.getString("callMax", "0"));
            callDuration = callMax * 60 - callDuration;
            if (callDuration < 0) {
                negative = true;
                callDuration = callDuration * -1;
            }
        }

        // Set the text
        int minutes = callDuration / 60;
        String minutesString = String.valueOf(minutes);
        if (negative) {
            minutesString = "- " + minutesString;
        }

        if (round) {
            return minutesString;
        }

        int seconds = callDuration % 60;
        String secondsString = String.valueOf(seconds);
        if (seconds < 10) {
            secondsString = "0" + secondsString;
        }

        return minutesString + ":" + secondsString;
    }
}
