package com.asmontec.bydavm.car;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;

import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.*;

public final class CarMqttService extends Service implements LocationListener {
    private static final String CHANNEL = "car_mqtt";
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile SSLSocket socket;
    private volatile Location lastLocation;
    private Thread worker;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(41, notification("Connexion MQTT…"));
        requestLocation();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running.getAndSet(true)) {
            worker = new Thread(this::loop, "car-mqtt");
            worker.start();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        running.set(false);
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        try { lm.removeUpdates(this); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public android.os.IBinder onBind(Intent intent) { return null; }

    private void requestLocation() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, this); } catch (SecurityException ignored) {}
    }

    @Override public void onLocationChanged(Location location) { lastLocation = location; }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}

    private void loop() {
        int retry = 2;
        while (running.get()) {
            try {
                String user = SecureConfig.user(this);
                String pass = SecureConfig.password(this);
                String prefix = SecureConfig.topic(this);
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, null, new SecureRandom());
                SSLSocket ssl = (SSLSocket) context.getSocketFactory().createSocket(SecureConfig.HOST, SecureConfig.PORT);
                SSLParameters p = ssl.getSSLParameters();
                p.setEndpointIdentificationAlgorithm("HTTPS");
                ssl.setSSLParameters(p);
                ssl.startHandshake();
                socket = ssl;
                DataInputStream in = new DataInputStream(ssl.getInputStream());
                OutputStream out = ssl.getOutputStream();
                sendConnect(out, user, pass);
                int header = in.readUnsignedByte();
                int len = readRemainingLength(in);
                byte[] connack = new byte[len];
                in.readFully(connack);
                if ((header >> 4) != 2 || len < 2 || connack[1] != 0) throw new IOException("MQTT refusé");
                updateNotification("Connecté à HiveMQ");
                retry = 2;
                while (running.get()) {
                    publish(out, prefix + "/telemetry/live", telemetryJson());
                    publish(out, prefix + "/status", "{\"online\":true}");
                    Thread.sleep(5000L);
                }
            } catch (Exception e) {
                updateNotification("Reconnexion dans " + retry + " s");
                try { Thread.sleep(retry * 1000L); } catch (InterruptedException ignored) {}
                retry = Math.min(30, retry * 2);
            } finally {
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
                socket = null;
            }
        }
    }

    private String telemetryJson() throws Exception {
        JSONObject j = new JSONObject();
        j.put("timestamp", System.currentTimeMillis());
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level >= 0) j.put("androidBattery", Math.round(level * 100f / Math.max(1, scale)));
        }
        Location l = lastLocation;
        if (l != null) {
            j.put("latitude", l.getLatitude());
            j.put("longitude", l.getLongitude());
            j.put("speedKmh", l.hasSpeed() ? l.getSpeed() * 3.6 : 0.0);
            j.put("altitude", l.hasAltitude() ? l.getAltitude() : JSONObject.NULL);
            j.put("accuracy", l.hasAccuracy() ? l.getAccuracy() : JSONObject.NULL);
        }
        return j.toString();
    }

    private static void sendConnect(OutputStream out, String user, String pass) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeUtf(body, "MQTT"); body.write(4);
        int flags = 0x02 | (user.isEmpty() ? 0 : 0x80) | (pass.isEmpty() ? 0 : 0x40);
        body.write(flags); body.write(0); body.write(30);
        writeUtf(body, "byd-car-" + Long.toHexString(System.nanoTime()));
        if (!user.isEmpty()) writeUtf(body, user);
        if (!pass.isEmpty()) writeUtf(body, pass);
        writePacket(out, 0x10, body.toByteArray());
    }

    private static void publish(OutputStream out, String topic, String payload) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeUtf(body, topic);
        body.write(payload.getBytes(StandardCharsets.UTF_8));
        writePacket(out, 0x30, body.toByteArray());
    }

    private static void writePacket(OutputStream out, int header, byte[] body) throws Exception {
        out.write(header); writeRemainingLength(out, body.length); out.write(body); out.flush();
    }
    private static void writeUtf(OutputStream out, String value) throws Exception {
        byte[] b = value.getBytes(StandardCharsets.UTF_8); out.write((b.length >> 8) & 255); out.write(b.length & 255); out.write(b);
    }
    private static void writeRemainingLength(OutputStream out, int value) throws Exception {
        do { int d = value % 128; value /= 128; if (value > 0) d |= 128; out.write(d); } while (value > 0);
    }
    private static int readRemainingLength(DataInputStream in) throws Exception {
        int m = 1, v = 0, d; do { d = in.readUnsignedByte(); v += (d & 127) * m; m *= 128; } while ((d & 128) != 0); return v;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "BYD MQTT", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }
    private Notification notification(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setContentTitle("BYD Connected Monitor Car").setContentText(text).setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
    }
    private void updateNotification(String text) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(41, notification(text));
    }
}
