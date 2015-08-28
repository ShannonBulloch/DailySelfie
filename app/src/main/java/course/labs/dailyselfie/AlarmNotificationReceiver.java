package course.labs.dailyselfie;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmNotificationReceiver extends BroadcastReceiver {

    private static final int MY_NOTIFICATION_ID = 1;
    public static final String END_TIME = "EndTime";
    public static final String INTERVAL_MILLIS = "IntervalMillis";
    private String tickerText = "Selfie Alert :)";
    private String contentTitle = "Daily Selfie";
    private String contentText = "Time to take another selfie!";

    public AlarmNotificationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // The Intent to be used when the user clicks on the Notification View
        Intent restartMainActivityIntent = new Intent(context, MainActivity.class);
        restartMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, restartMainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Build the Notification
        Notification.Builder notificationBuilder = new Notification.Builder(
                context).setTicker(tickerText)
                .setSmallIcon(R.drawable.ic_camera_alt_white_24dp)
                .setAutoCancel(true)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL);

        // Get the NotificationManager
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        // Pass the Notification to the NotificationManager:
        mNotificationManager.notify(MY_NOTIFICATION_ID,
                notificationBuilder.build());

        long endTime = intent.getLongExtra(END_TIME, -1);
        long interval = intent.getLongExtra(INTERVAL_MILLIS, 0);
        if (endTime != -1) {
            if (System.currentTimeMillis() + interval > endTime ) {
                cancelAlarm(context);
            }
        }
    }

    private void cancelAlarm(Context context) {
        // Get the AlarmManager Service
        AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create an Intent to broadcast to the AlarmNotificationReceiver
        Intent mNotificationReceiverIntent = new Intent(context,
                AlarmNotificationReceiver.class);

        // Create an PendingIntent that holds the NotificationReceiverIntent
        PendingIntent mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(
                context, 0, mNotificationReceiverIntent, 0);

        mAlarmManager.cancel(mNotificationReceiverPendingIntent);
    }
}
