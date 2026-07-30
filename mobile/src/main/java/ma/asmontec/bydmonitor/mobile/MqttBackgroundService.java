package ma.asmontec.bydmonitor.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

public final class MqttBackgroundService extends Service {
    public static final String ACTION_DATA = "ma.asmontec.bydmonitor.mobile.DATA";
    public static final String EXTRA_TOPIC = "topic";
    public static final String EXTRA_PAYLOAD = "payload";
    public static final String EXTRA_STATUS = "status";
    private static final String CHANNEL = "mqtt_background";
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile SSLSocket socket;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(7, notification("Connexion MQTT…"));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (running.compareAndSet(false, true)) {
            new Thread(this::loop, "mqtt-background").start();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        running.set(false);
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void loop() {
        int retry = 2;
        while (running.get() && SecureConfig.isEnabled(this)) {
            try {
                String user = SecureConfig.user(this);
                String pass = SecureConfig.password(this);
                String prefix = SecureConfig.topic(this);
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, null, new SecureRandom());
                SSLSocket ssl = (SSLSocket) ctx.getSocketFactory().createSocket(SecureConfig.DEFAULT_HOST, SecureConfig.DEFAULT_PORT);
                SSLParameters p = ssl.getSSLParameters();
                p.setEndpointIdentificationAlgorithm("HTTPS");
                ssl.setSSLParameters(p);
                ssl.setSoTimeout(45000);
                ssl.startHandshake();
                socket = ssl;

                OutputStream out = ssl.getOutputStream();
                DataInputStream in = new DataInputStream(ssl.getInputStream());
                sendConnect(out, user, pass);
                int header = in.readUnsignedByte();
                int len = readRemainingLength(in);
                byte[] conn = new byte[len];
                in.readFully(conn);
                if ((header >> 4) != 2 || len < 2 || conn[1] != 0) throw new IllegalStateException("Accès MQTT refusé");
                sendSubscribe(out, prefix + "/#");
                sendStatus("Connecté — TLS actif");
                updateNotification("Connecté à HiveMQ Cloud");
                retry = 2;

                long lastPing = System.currentTimeMillis();
                while (running.get()) {
                    if (System.currentTimeMillis() - lastPing > 20000) {
                        out.write(new byte[]{(byte) 0xC0, 0x00});
                        out.flush();
                        lastPing = System.currentTimeMillis();
                    }
                    int h = in.readUnsignedByte();
                    int remaining = readRemainingLength(in);
                    byte[] packet = new byte[remaining];
                    in.readFully(packet);
                    if ((h >> 4) == 3) publish(packet, (h & 0x06) >> 1);
                }
            } catch (Exception e) {
                if (!running.get()) break;
                sendStatus("Reconnexion automatique…");
                updateNotification("Reconnexion MQTT…");
                try { Thread.sleep(retry * 1000L); } catch (InterruptedException ignored) {}
                retry = Math.min(30, retry * 2);
            } finally {
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
                socket = null;
            }
        }
        stopSelf();
    }

    private void publish(byte[] packet, int qos) {
        if (packet.length < 2) return;
        int topicLength = ((packet[0] & 255) << 8) | (packet[1] & 255);
        int start = 2 + topicLength + (qos > 0 ? 2 : 0);
        if (topicLength <= 0 || start > packet.length) return;
        Intent i = new Intent(ACTION_DATA);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_TOPIC, new String(packet, 2, topicLength, StandardCharsets.UTF_8));
        i.putExtra(EXTRA_PAYLOAD, new String(packet, start, packet.length - start, StandardCharsets.UTF_8));
        sendBroadcast(i);
    }

    private void sendStatus(String status) {
        Intent i = new Intent(ACTION_DATA);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_STATUS, status);
        sendBroadcast(i);
    }

    private void sendConnect(OutputStream out, String user, String pass) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        writeUtf(b, "MQTT"); b.write(4);
        int flags = 0x02;
        if (!user.isEmpty()) flags |= 0x80;
        if (!pass.isEmpty()) flags |= 0x40;
        b.write(flags); b.write(0); b.write(30);
        writeUtf(b, "byd-mobile-" + Long.toHexString(System.nanoTime()));
        if (!user.isEmpty()) writeUtf(b, user);
        if (!pass.isEmpty()) writeUtf(b, pass);
        writePacket(out, 0x10, b.toByteArray());
    }

    private void sendSubscribe(OutputStream out, String topic) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write(0); b.write(1); writeUtf(b, topic); b.write(0);
        writePacket(out, 0x82, b.toByteArray());
    }

    private static void writePacket(OutputStream out, int h, byte[] body) throws Exception {
        out.write(h); writeRemainingLength(out, body.length); out.write(body); out.flush();
    }
    private static void writeUtf(OutputStream out, String v) throws Exception {
        byte[] bytes = v.getBytes(StandardCharsets.UTF_8);
        out.write((bytes.length >> 8) & 255); out.write(bytes.length & 255); out.write(bytes);
    }
    private static void writeRemainingLength(OutputStream out, int value) throws Exception {
        do { int d = value % 128; value /= 128; if (value > 0) d |= 128; out.write(d); } while (value > 0);
    }
    private static int readRemainingLength(DataInputStream in) throws Exception {
        int m = 1, value = 0, d, count = 0;
        do { d = in.readUnsignedByte(); value += (d & 127) * m; m *= 128; if (++count > 4) throw new IllegalStateException("Paquet MQTT invalide"); } while ((d & 128) != 0);
        return value;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Connexion véhicule", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Maintient la connexion MQTT sécurisée en arrière-plan");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setContentTitle("BYD Monitor Mobile").setContentText(text).setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
    }

    private void updateNotification(String text) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(7, notification(text));
    }
}
