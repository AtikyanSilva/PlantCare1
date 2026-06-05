package atikyan.silva.plantcare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class CareAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID   = "plant_reminders";
    private static final String CHANNEL_NAME = "Напоминания о растениях";

    @Override
    public void onReceive(Context context, Intent intent) {
        String name   = intent.getStringExtra("reminder_name");
        String taskId = intent.getStringExtra("TASK_ID");
        if (name == null) name = "Уход за растением";

        createChannelIfNeeded(context);

        // Тап по уведомлению → открываем RemindersActivity
        Intent openIntent = new Intent(context, RemindersActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (taskId != null) openIntent.putExtra("TASK_ID", taskId);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                taskId != null ? taskId.hashCode() : 0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Уникальный ID уведомления — чтобы несколько напоминаний не перекрывали друг друга
        int notifId = taskId != null ? taskId.hashCode() : (int) System.currentTimeMillis();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_plant_logo)
                .setContentTitle("🌿 " + name)
                .setContentText("Время позаботиться о своём растении!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(notifId, builder.build());
    }

    private void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Напоминания об уходе за растениями");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
