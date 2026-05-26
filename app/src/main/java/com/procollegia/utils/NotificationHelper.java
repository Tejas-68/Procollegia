package com.procollegia.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.procollegia.R;
import com.procollegia.TimetableUploadActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper for showing local (on-device) notifications.
 * Does NOT require FCM or any server — runs entirely on-device.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID   = "timetable_reminder";
    private static final String CHANNEL_NAME = "Timetable Reminders";
    private static final int    NOTIF_ID     = 1001;

    // Pref key to ensure we only show the reminder once per day
    private static final String PREF_LAST_NOTIF_DATE = "last_timetable_notif_date";

    /**
     * Shows a local notification reminding the teacher/HOD to upload today's timetable.
     * Only fires ONCE per day (won't spam on every app open).
     *
     * @param context    Activity/Fragment context
     * @param department Department name (e.g. "BCA")
     */
    public static void notifyUploadReminder(Context context, String department) {
        if (context == null) return;

        // Check if we already notified today
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());
        SharedPreferences prefs = context.getSharedPreferences("procollegia_notif", Context.MODE_PRIVATE);
        String lastNotified = prefs.getString(PREF_LAST_NOTIF_DATE, "");

        if (today.equals(lastNotified)) return; // Already sent today

        // Create notification channel (Android O+)
        createChannel(context);

        // Tap notification → opens TimetableUploadActivity
        Intent intent = new Intent(context, TimetableUploadActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String dept = (department != null) ? department.toUpperCase() : "your department";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Upload Today's Timetable")
                .setContentText("No timetable uploaded yet for " + dept + " today. Tap to upload now.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("No timetable has been uploaded for " + dept + " today.\n"
                                + "Students are waiting! Tap here to upload today's timetable."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build());
            // Mark today as notified so we don't send again
            prefs.edit().putString(PREF_LAST_NOTIF_DATE, today).apply();
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS permission not granted — silently skip
        }
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminds teachers to upload today's timetable");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
