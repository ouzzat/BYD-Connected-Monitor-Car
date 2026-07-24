package com.asmontec.bydavm.car.service;

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
import com.asmontec.bydavm.car.MainActivity;
import com.asmontec.bydavm.car.mqtt.MiniMqttClient;
import com.asmontec.bydavm.car.mqtt.MqttConfig;
import com.asmontec.bydavm.car.mqtt.MqttListener;
import com.asmontec.bydavm.car.mqtt.OfflineQueue;
import com.asmontec.bydavm.car.security.SecureCredentialStore;
import com.asmontec.bydavm.car.telemetry.TelemetryCollector;
import com.asmontec.bydavm.car.telemetry.VehicleTelemetry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Service de premier plan de l'agent voiture. Boucle unique : connexion MQTT
 * (avec temporisation exponentielle), publication périodique de la
 * télémétrie, file d'attente hors-ligne, mise à jour de la notification
 * permanente et de {@link ServiceStatus} pour l'interface.
 */
public final class CarAgentService extends Service {

    private static final String CHANNEL_ID = "car_agent_status";
    private static final int NOTIFICATION_ID = 1001;
    private static final int MAX_QUEUE_SIZE = 200;
    private static final int ACTIVE_INTERVAL_MS = 5_000;
    private static final int PARKED_INTERVAL_MS = 15_000;
    private static final double PARKED_SPEED_THRESHOLD_KMH = 5.0;
    private static final int HEALTH_PUBLISH_EVERY_N_CYCLES = 12;

    private SecureCredentialStore credentialStore;
    private TelemetryCollector telemetryCollector;
    private OfflineQueue offlineQueue;
    private volatile MiniMqttClient mqttClient;
    private volatile boolean running;
    private Thread workerThread;
    private String clientId;

    @Override
    public void onCreate() {
        super.onCreate();
        credentialStore = new SecureCredentialStore(this);
        telemetryCollector = new TelemetryCollector(this);
        offlineQueue = new OfflineQueue(MAX_QUEUE_SIZE);
        clientId = "car-" + readOrGenerateDeviceSuffix();

        createNotificationChannel();
        startForegroundCompat(buildNotification("Service actif", "Démarrage en cours"));

        telemetryCollector.startLocationUpdates();

        running = true;
        workerThread = new Thread(this::runLoop, "car-agent-worker");
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
        telemetryCollector.stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runLoop() {
        int consecutiveFailures = 0;
        long cycle = 0;

        while (running) {
            if (!credentialStore.hasCredentials()) {
                ServiceStatus.publish(ServiceStatus.State.CONFIG_REQUIRED, "Identifiants MQTT non configurés");
                updateNotification("Configuration requise", "Ouvrez Configuration développeur");
                sleepQuiet(10_000);
                continue;
            }

            if (mqttClient == null || !mqttClient.isConnected()) {
                ServiceStatus.publish(consecutiveFailures == 0 ? ServiceStatus.State.CONNECTING : ServiceStatus.State.RECONNECTING,
                        "Connexion à " + MqttConfig.BROKER_HOST);
                updateNotification("Reconnexion en cours", MqttConfig.BROKER_HOST);
                try {
                    connect();
                    consecutiveFailures = 0;
                    ServiceStatus.publish(ServiceStatus.State.CONNECTED, "Connecté à HiveMQ");
                    publishRetainedStatus(true);
                    flushOfflineQueue();
                } catch (IOException e) {
                    consecutiveFailures++;
                    long backoffMs = Math.min(60_000L, 2_000L * (1L << Math.min(consecutiveFailures, 5)));
                    sleepQuiet(backoffMs);
                    continue;
                }
            }

            VehicleTelemetry telemetry = telemetryCollector.collect();
            boolean gpsAvailable = telemetryCollector.isGpsAvailable();

            try {
                publishTelemetry(telemetry);
                cycle++;
                if (cycle % HEALTH_PUBLISH_EVERY_N_CYCLES == 0) {
                    publishAndroidHealth(telemetry);
                    publishNetworkStatus(telemetry);
                }
                String detail = gpsAvailable ? "Connecté à HiveMQ" : "Connecté à HiveMQ — GPS indisponible";
                ServiceStatus.publish(gpsAvailable ? ServiceStatus.State.CONNECTED : ServiceStatus.State.GPS_UNAVAILABLE, detail);
                updateNotification(gpsAvailable ? "Connecté à HiveMQ" : "GPS indisponible", "Service actif");
            } catch (IOException e) {
                offlineQueue.offer(MqttConfig.TOPIC_TELEMETRY_LIVE, telemetry.toJson().getBytes(StandardCharsets.UTF_8));
                MiniMqttClient failed = mqttClient;
                mqttClient = null;
                if (failed != null) {
                    failed.close();
                }
                continue;
            }

            boolean parked = telemetry.speedKmh == null || telemetry.speedKmh < PARKED_SPEED_THRESHOLD_KMH;
            sleepQuiet(parked ? PARKED_INTERVAL_MS : ACTIVE_INTERVAL_MS);
        }
    }

    private void connect() throws IOException {
        MiniMqttClient client = new MiniMqttClient(MqttConfig.BROKER_HOST, MqttConfig.BROKER_PORT, 10_000, null);
        MiniMqttClient.Will will = new MiniMqttClient.Will(MqttConfig.TOPIC_STATUS, offlineStatusPayload(), true);
        client.connect(clientId, credentialStore.getUsername(), credentialStore.getPassword(), 30, will,
                new MqttListener() {
                    @Override
                    public void onConnected() {
                        // géré via la valeur de retour de connect()
                    }

                    @Override
                    public void onDisconnected(Throwable reason) {
                        ServiceStatus.publish(ServiceStatus.State.RECONNECTING,
                                reason == null ? "Connexion perdue" : "Connexion perdue : " + reason.getMessage());
                    }
                });
        mqttClient = client;
    }

    private void publishTelemetry(VehicleTelemetry telemetry) throws IOException {
        byte[] payload = telemetry.toJson().getBytes(StandardCharsets.UTF_8);
        mqttClient.publish(MqttConfig.TOPIC_TELEMETRY_LIVE, payload);

        if (telemetry.latitude != null && telemetry.longitude != null) {
            mqttClient.publish(MqttConfig.TOPIC_LOCATION_LIVE, locationPayload(telemetry));
        }
    }

    private void publishAndroidHealth(VehicleTelemetry telemetry) throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", System.currentTimeMillis());
            json.put("androidBattery", telemetry.androidBattery == null ? JSONObject.NULL : telemetry.androidBattery);
            json.put("serviceRunning", true);
            mqttClient.publish(MqttConfig.TOPIC_ANDROID_HEALTH, json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JSONException ignored) {
        }
    }

    private void publishNetworkStatus(VehicleTelemetry telemetry) throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", System.currentTimeMillis());
            json.put("networkType", telemetry.networkType == null ? JSONObject.NULL : telemetry.networkType);
            json.put("signalLevel", telemetry.signalLevel == null ? JSONObject.NULL : telemetry.signalLevel);
            mqttClient.publish(MqttConfig.TOPIC_NETWORK_STATUS, json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JSONException ignored) {
        }
    }

    private void publishRetainedStatus(boolean online) throws IOException {
        mqttClient.publish(MqttConfig.TOPIC_STATUS, onlineStatusPayload(online), true);
    }

    private byte[] onlineStatusPayload(boolean online) {
        try {
            JSONObject json = new JSONObject();
            json.put("online", online);
            json.put("timestamp", System.currentTimeMillis());
            return json.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JSONException e) {
            return "{\"online\":false}".getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] offlineStatusPayload() {
        return onlineStatusPayload(false);
    }

    private byte[] locationPayload(VehicleTelemetry telemetry) {
        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", telemetry.timestamp);
            json.put("latitude", telemetry.latitude);
            json.put("longitude", telemetry.longitude);
            json.put("altitude", telemetry.altitude == null ? JSONObject.NULL : telemetry.altitude);
            json.put("accuracy", telemetry.accuracy == null ? JSONObject.NULL : telemetry.accuracy);
            json.put("speedKmh", telemetry.speedKmh == null ? JSONObject.NULL : telemetry.speedKmh);
            return json.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JSONException e) {
            return "{}".getBytes(StandardCharsets.UTF_8);
        }
    }

    private void flushOfflineQueue() {
        OfflineQueue.Pending pending;
        while ((pending = offlineQueue.poll()) != null) {
            try {
                mqttClient.publish(pending.topic, pending.payload);
            } catch (IOException e) {
                offlineQueue.offer(pending.topic, pending.payload);
                break;
            }
        }
    }

    private String readOrGenerateDeviceSuffix() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId == null || androidId.isEmpty() ? "unit" : androidId;
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
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "État de l'agent BYD",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Notification permanente et discrète de l'état de connexion MQTT");
            manager.createNotificationChannel(channel);
        }
    }

    private void startForegroundCompat(Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
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
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent);
        return builder.build();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, CarAgentService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
