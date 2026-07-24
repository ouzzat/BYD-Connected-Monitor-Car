package ma.asmontec.bydmonitor.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class MainActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final AtomicBoolean running = new AtomicBoolean(false);

    private EditText hostField;
    private EditText portField;
    private EditText userField;
    private EditText passField;
    private EditText topicField;
    private TextView connectionView;
    private TextView speedView;
    private TextView batteryView;
    private TextView positionView;
    private TextView updatedView;
    private TextView rawView;
    private Button connectButton;

    private volatile SSLSocket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createUi());
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    private View createUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(30));
        root.setBackgroundColor(Color.rgb(10, 16, 24));
        scroll.addView(root);

        TextView title = text("BYD Monitor Mobile", 28, Color.WHITE, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView subtitle = text("Compagnon sécurisé en lecture seule", 15, Color.rgb(105, 210, 255), false);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        root.addView(subtitle);

        root.addView(section("Connexion MQTT TLS"));
        hostField = field("Serveur MQTT (ex. mqtt.example.com)", InputType.TYPE_CLASS_TEXT);
        portField = field("Port TLS", InputType.TYPE_CLASS_NUMBER);
        userField = field("Utilisateur", InputType.TYPE_CLASS_TEXT);
        passField = field("Mot de passe (non enregistré)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        topicField = field("Préfixe topic (ex. asmontec/byd/vehicule1)", InputType.TYPE_CLASS_TEXT);

        android.content.SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        hostField.setText(prefs.getString("host", ""));
        portField.setText(prefs.getString("port", "8883"));
        userField.setText(prefs.getString("user", ""));
        topicField.setText(prefs.getString("topic", "asmontec/byd/vehicle"));

        root.addView(hostField);
        root.addView(portField);
        root.addView(userField);
        root.addView(passField);
        root.addView(topicField);

        connectButton = new Button(this);
        connectButton.setText("SE CONNECTER");
        connectButton.setOnClickListener(v -> {
            if (running.get()) disconnect(); else connect();
        });
        root.addView(connectButton);

        connectionView = card("Déconnecté", 20);
        root.addView(connectionView);

        root.addView(section("Données du véhicule"));
        speedView = card("Vitesse GPS\n— km/h", 22);
        batteryView = card("Batterie unité Android\n— %", 22);
        positionView = card("Position\n—", 17);
        updatedView = card("Dernière mise à jour\n—", 15);
        rawView = card("Dernier message brut\n—", 13);
        root.addView(speedView);
        root.addView(batteryView);
        root.addView(positionView);
        root.addView(updatedView);
        root.addView(rawView);

        return scroll;
    }

    private TextView section(String value) {
        TextView v = text(value, 18, Color.WHITE, true);
        v.setPadding(0, dp(18), 0, dp(8));
        return v;
    }

    private EditText field(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(145, 158, 171));
        e.setTextColor(Color.WHITE);
        e.setInputType(type);
        e.setSingleLine(true);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }

    private TextView card(String value, int size) {
        TextView v = text(value, size, Color.WHITE, false);
        v.setBackgroundColor(Color.rgb(22, 32, 45));
        v.setPadding(dp(15), dp(14), dp(15), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        v.setLayoutParams(lp);
        return v;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    private void connect() {
        final String host = hostField.getText().toString().trim();
        final String portText = portField.getText().toString().trim();
        final String user = userField.getText().toString();
        final String pass = passField.getText().toString();
        final String prefix = normalizeTopic(topicField.getText().toString());

        if (host.isEmpty() || portText.isEmpty() || prefix.isEmpty()) {
            setStatus("Configuration incomplète", Color.rgb(255, 183, 77));
            return;
        }

        final int port;
        try { port = Integer.parseInt(portText); }
        catch (NumberFormatException e) {
            setStatus("Port invalide", Color.rgb(255, 107, 107));
            return;
        }

        getSharedPreferences("config", MODE_PRIVATE).edit()
                .putString("host", host)
                .putString("port", portText)
                .putString("user", user)
                .putString("topic", prefix)
                .apply();

        running.set(true);
        connectButton.setText("DÉCONNECTER");
        setStatus("Connexion…", Color.rgb(255, 213, 79));

        new Thread(() -> mqttLoop(host, port, user, pass, prefix), "mqtt-mobile").start();
    }

    private void disconnect() {
        running.set(false);
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        socket = null;
        ui.post(() -> {
            connectButton.setText("SE CONNECTER");
            setStatus("Déconnecté", Color.LTGRAY);
        });
    }

    private void mqttLoop(String host, int port, String user, String pass, String prefix) {
        int retrySeconds = 2;
        while (running.get()) {
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, null, new SecureRandom());
                SSLSocketFactory factory = context.getSocketFactory();
                SSLSocket ssl = (SSLSocket) factory.createSocket(host, port);
                SSLParameters parameters = ssl.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                ssl.setSSLParameters(parameters);
                ssl.setEnabledProtocols(selectTls(ssl.getSupportedProtocols()));
                ssl.setSoTimeout(45000);
                ssl.startHandshake();
                socket = ssl;

                OutputStream out = ssl.getOutputStream();
                DataInputStream in = new DataInputStream(ssl.getInputStream());
                sendConnect(out, user, pass);
                int type = in.readUnsignedByte();
                int len = readRemainingLength(in);
                byte[] response = new byte[len];
                in.readFully(response);
                if ((type >> 4) != 2 || len < 2 || response[1] != 0) {
                    throw new IllegalStateException("Connexion MQTT refusée");
                }

                sendSubscribe(out, prefix + "/#");
                ui.post(() -> setStatus("Connecté — TLS actif", Color.rgb(102, 255, 178)));
                retrySeconds = 2;

                while (running.get()) {
                    int header = in.readUnsignedByte();
                    int remaining = readRemainingLength(in);
                    byte[] packet = new byte[remaining];
                    in.readFully(packet);
                    int packetType = header >> 4;
                    if (packetType == 3) handlePublish(packet);
                    else if (packetType == 13) { /* PINGRESP */ }
                }
            } catch (Exception e) {
                if (!running.get()) break;
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                ui.post(() -> setStatus("Reconnexion: " + message, Color.rgb(255, 138, 101)));
                try { Thread.sleep(retrySeconds * 1000L); } catch (InterruptedException ignored) {}
                retrySeconds = Math.min(30, retrySeconds * 2);
            } finally {
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
                socket = null;
            }
        }
    }

    private void handlePublish(byte[] packet) {
        if (packet.length < 2) return;
        int topicLength = ((packet[0] & 0xff) << 8) | (packet[1] & 0xff);
        int start = 2 + topicLength;
        if (topicLength < 0 || start > packet.length) return;
        String topic = new String(packet, 2, topicLength, StandardCharsets.UTF_8);
        String payload = new String(packet, start, packet.length - start, StandardCharsets.UTF_8);
        ui.post(() -> updateDashboard(topic, payload));
    }

    private void updateDashboard(String topic, String payload) {
        rawView.setText("Dernier message brut\n" + topic + "\n" + limit(payload, 500));
        updatedView.setText("Dernière mise à jour\n" + new java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));

        try {
            JSONObject json = new JSONObject(payload);
            Double speed = number(json, "speedKmh", "speed_kmh", "speed");
            Double battery = number(json, "androidBattery", "android_battery", "battery");
            Double lat = number(json, "latitude", "lat");
            Double lon = number(json, "longitude", "lon", "lng");

            if (speed != null) speedView.setText(String.format(Locale.getDefault(), "Vitesse GPS\n%.1f km/h", speed));
            if (battery != null) batteryView.setText(String.format(Locale.getDefault(), "Batterie unité Android\n%.0f %%", battery));
            if (lat != null && lon != null) positionView.setText(String.format(Locale.US, "Position\n%.6f, %.6f", lat, lon));
        } catch (Exception ignored) {
            // Les messages texte restent visibles dans la carte brute.
        }
    }

    private static Double number(JSONObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key) && !json.isNull(key)) {
                try { return json.getDouble(key); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private void sendConnect(OutputStream out, String user, String pass) throws Exception {
        ByteArrayOutputStream variable = new ByteArrayOutputStream();
        writeUtf(variable, "MQTT");
        variable.write(4);
        int flags = 0x02;
        if (!user.isEmpty()) flags |= 0x80;
        if (!pass.isEmpty()) flags |= 0x40;
        variable.write(flags);
        variable.write(0);
        variable.write(30);
        writeUtf(variable, "byd-mobile-" + Long.toHexString(System.nanoTime()));
        if (!user.isEmpty()) writeUtf(variable, user);
        if (!pass.isEmpty()) writeUtf(variable, pass);
        writePacket(out, 0x10, variable.toByteArray());
    }

    private void sendSubscribe(OutputStream out, String topic) throws Exception {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(0);
        payload.write(1);
        writeUtf(payload, topic);
        payload.write(0);
        writePacket(out, 0x82, payload.toByteArray());
    }

    private static void writePacket(OutputStream out, int header, byte[] body) throws Exception {
        out.write(header);
        writeRemainingLength(out, body.length);
        out.write(body);
        out.flush();
    }

    private static void writeUtf(OutputStream out, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.write((bytes.length >> 8) & 0xff);
        out.write(bytes.length & 0xff);
        out.write(bytes);
    }

    private static void writeRemainingLength(OutputStream out, int value) throws Exception {
        do {
            int digit = value % 128;
            value /= 128;
            if (value > 0) digit |= 0x80;
            out.write(digit);
        } while (value > 0);
    }

    private static int readRemainingLength(DataInputStream in) throws Exception {
        int multiplier = 1;
        int value = 0;
        int digit;
        do {
            digit = in.readUnsignedByte();
            value += (digit & 127) * multiplier;
            multiplier *= 128;
            if (multiplier > 128 * 128 * 128 * 128) throw new IllegalStateException("Paquet MQTT invalide");
        } while ((digit & 128) != 0);
        return value;
    }

    private static String[] selectTls(String[] supported) {
        java.util.ArrayList<String> selected = new java.util.ArrayList<>();
        for (String protocol : supported) {
            if ("TLSv1.3".equals(protocol) || "TLSv1.2".equals(protocol)) selected.add(protocol);
        }
        return selected.toArray(new String[0]);
    }

    private void setStatus(String text, int color) {
        connectionView.setText(text);
        connectionView.setTextColor(color);
    }

    private static String normalizeTopic(String value) {
        String t = value == null ? "" : value.trim();
        while (t.startsWith("/")) t = t.substring(1);
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    private static String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
