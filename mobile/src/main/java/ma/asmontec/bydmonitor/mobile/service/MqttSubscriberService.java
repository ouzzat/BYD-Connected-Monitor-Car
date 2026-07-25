package ma.asmontec.bydmonitor.mobile.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import ma.asmontec.bydmonitor.mobile.MainActivity;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;
import ma.asmontec.bydmonitor.mobile.mqtt.MiniMqttClient;
import ma.asmontec.bydmonitor.mobile.mqtt.MqttConfig;
import ma.asmontec.bydmonitor.mobile.mqtt.MqttListener;
import ma.asmontec.bydmonitor.mobile.security.SecureCredentialStore;
import java.io.IOException;

/**
 * Service de premier plan qui reste abonné au broker MQTT en arrière-plan,
 * reconnecte automatiquement et transmet chaque message reçu au
 * {@link VehicleStateRepository}. Aucune commande n'est jamais publiée
 * par cette application : le compte mobile est limité à l'abonnement.
 */
public final class MqttSubscriberService extends Service {

    private static final String CHANNEL_ID = "mobile_mqtt_status";
    private static final int NOTIFICATION_ID = 3001;

    private SecureCredentialStore credentialStore;
    private VehicleStateRepository repository;
    private volatile MiniMqttClient mqttClient;
    private volatile boolean running;
    private Thread workerThread;
    private String clientId;

    @Override
    public void onCreate() {
        super.onCreate();
        credentialStore = new SecureCredentialStore(this);
        repository = VehicleStateRepository.getInstance(this);
        clientId = "mobile-" + readOrGenerateDeviceSuffix();

        createNotificationChannel();
        startForegroundCompat(buildNotification("Surveillance BYD active", "Connexion en cours"));

        running = true;
        workerThread = new Thread(this::runLoop, "mobile-mqtt-worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
        MiniMqttClient client = mqttClient;
        if (client != null) {
            client.disconnectGracefully();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runLoop() {
        int consecutiveFailures = 0;
        while (running) {
            if (!credentialStore.hasCredentials()) {
                updateNotification("Configuration requise", "Ouvrez Plus > Paramètres de connexion");
                sleepQuiet(10_000);
                continue;
            }
            if (mqttClient == null || !mqttClient.isConnected()) {
                updateNotification(consecutiveFailures == 0 ? "Connexion en cours" : "Reconnexion en cours",
                        MqttConfig.BROKER_HOST);
                try {
                    connect();
                    consecutiveFailures = 0;
                    updateNotification("Surveillance BYD active", "Abonné à " + MqttConfig.TOPIC_PREFIX);
                } catch (IOException e) {
                    consecutiveFailures++;
                    long backoffMs = Math.min(60_000L, 2_000L * (1L << Math.min(consecutiveFailures, 5)));
                    sleepQuiet(backoffMs);
                }
            } else {
                sleepQuiet(2_000);
            }
        }
    }

    private void connect() throws IOException {
        MiniMqttClient client = new MiniMqttClient(MqttConfig.BROKER_HOST, MqttConfig.BROKER_PORT, 10_000, null);
        client.connect(clientId, credentialStore.getUsername(), credentialStore.getPassword(), 30,
                new MqttListener() {
                    @Override
                    public void onConnected() {
                        try {
                            client.subscribe(MqttConfig.SUBSCRIBE_ALL);
                        } catch (IOException e) {
                            client.close();
                        }
                    }

                    @Override
                    public void onMessage(String topic, byte[] payload) {
                        repository.onMqttMessage(topic, payload);
                    }

                    @Override
                    public void onDisconnected(Throwable reason) {
                        repository.onMqttDisconnected();
                        updateNotification("Reconnexion en cours", MqttConfig.BROKER_HOST);
                    }
                });
        mqttClient = client;
    }

    private String readOrGenerateDeviceSuffix() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId == null || androidId.isEmpty() ? "phone" : androidId;
    }

    private void sleepQuiet(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "État de la surveillance",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Notification permanente et discrète de l'état de connexion MQTT");
            manager.createNotificationChannel(channel);
        }
    }

    private void startForegroundCompat(Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(String title, String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(title, text));
        }
    }

    private Notification buildNotification(String title, String text) {
        Intent tapIntent = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, tapIntent, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent);
        return builder.build();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, MqttSubscriberService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
