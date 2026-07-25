package ma.asmontec.bydmonitor.mobile.notify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import ma.asmontec.bydmonitor.mobile.MainActivity;

/** Publie les notifications locales d'alerte (indépendantes de la notification de service). */
public final class NotificationHelper {

    public static final String ALERT_CHANNEL_ID = "vehicle_alerts";
    private static int nextId = 2000;

    private final Context context;
    private final NotificationPrefs prefs;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = new NotificationPrefs(context);
        createChannel();
    }

    public void notify(NotificationPrefs.Kind kind, String title, String text) {
        if (!prefs.isEnabled(kind)) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        Intent tapIntent = new Intent(context, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, tapIntent, flags);

        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, ALERT_CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        builder.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        manager.notify(nextId++, builder.build());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(ALERT_CHANNEL_ID, "Alertes véhicule",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Portes, trajets, batterie faible, perte de connexion");
            manager.createNotificationChannel(channel);
        }
    }
}
