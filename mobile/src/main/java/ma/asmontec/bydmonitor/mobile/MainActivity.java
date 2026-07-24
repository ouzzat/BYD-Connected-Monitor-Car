package ma.asmontec.bydmonitor.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(4, 9, 14);
    private static final int CARD = Color.rgb(15, 23, 31);
    private static final int ACCENT = Color.rgb(22, 214, 165);
    private LinearLayout content;
    private TextView status;
    private TextView speed;
    private TextView battery;
    private TextView power;
    private TextView range;
    private TextView position;
    private TextView lastUpdate;
    private TextView raw;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String s = intent.getStringExtra(MqttBackgroundService.EXTRA_STATUS);
            if (s != null) status.setText(s);
            String payload = intent.getStringExtra(MqttBackgroundService.EXTRA_PAYLOAD);
            String topic = intent.getStringExtra(MqttBackgroundService.EXTRA_TOPIC);
            if (payload != null) applyData(topic, payload);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        setContentView(buildUi());
        showHome();
        if (SecureConfig.isEnabled(this)) startMqttService();
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter(MqttBackgroundService.ACTION_DATA);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, f);
    }

    @Override protected void onStop() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        super.onStop();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(12));
        TextView title = label("BYD SEAL U DM-i", 23, Color.WHITE, true);
        status = label(SecureConfig.isEnabled(this) ? "Connexion automatique active" : "Configuration requise", 14, ACCENT, false);
        header.addView(title);
        header.addView(status);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(4), dp(14), dp(20));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(4), dp(6), dp(4), dp(8));
        nav.setBackgroundColor(Color.rgb(8, 14, 20));
        addNav(nav, "Accueil", v -> showHome());
        addNav(nav, "État", v -> showTelemetry());
        addNav(nav, "Carte", v -> showMap());
        addNav(nav, "Caméras", v -> showCameras());
        addNav(nav, "Plus", v -> showMore());
        root.addView(nav);
        return root;
    }

    private void addNav(LinearLayout nav, String text, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setOnClickListener(click);
        nav.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1f));
    }

    private void showHome() {
        content.removeAllViews();
        content.addView(hero("Véhicule connecté", "Vue en temps réel sécurisée"));
        LinearLayout row = row();
        battery = metric("Batterie", "— %");
        range = metric("Autonomie EV", "— km");
        row.addView(battery, weighted()); row.addView(range, weighted());
        content.addView(row);
        LinearLayout row2 = row();
        speed = metric("Vitesse", "— km/h");
        power = metric("Puissance", "— kW");
        row2.addView(speed, weighted()); row2.addView(power, weighted());
        content.addView(row2);
        content.addView(card("État du véhicule", "En attente des données BYD DiLink"));
        lastUpdate = card("Dernière mise à jour", "—");
        content.addView(lastUpdate);
    }

    private void showTelemetry() {
        content.removeAllViews();
        content.addView(section("État en temps réel"));
        speed = metric("Vitesse GPS", "— km/h");
        power = metric("Puissance", "— kW");
        battery = metric("Batterie SOC", "— %");
        range = metric("Autonomie", "— km");
        content.addView(speed); content.addView(power); content.addView(battery); content.addView(range);
        raw = card("Dernier message MQTT", "Aucune donnée reçue");
        content.addView(raw);
    }

    private void showMap() {
        content.removeAllViews();
        content.addView(section("Localisation"));
        TextView map = card("Carte sombre", "Le tracé GPS apparaîtra ici dès réception des coordonnées.");
        map.setMinHeight(dp(320)); map.setGravity(Gravity.CENTER);
        content.addView(map);
        position = card("Position actuelle", "—");
        content.addView(position);
    }

    private void showCameras() {
        content.removeAllViews();
        content.addView(section("Caméras"));
        TextView live = card("Flux en direct", "Aucune caméra compatible détectée pour le moment.");
        live.setMinHeight(dp(300)); live.setGravity(Gravity.CENTER);
        content.addView(live);
        content.addView(card("Sécurité", "Le flux vidéo ne sera activé qu’après validation sur le véhicule."));
    }

    private void showMore() {
        content.removeAllViews();
        content.addView(section("Plus"));
        content.addView(action("Paramètres de connexion", v -> showConfig()));
        content.addView(action("Notifications", v -> message("Notifications intelligentes activées via le service MQTT.")));
        content.addView(action("Événements", v -> message("Les événements portes, trajets et charge seront affichés dès leur réception.")));
        content.addView(action("Sécurité", v -> message("TLS obligatoire, aucun trafic HTTP clair, mot de passe chiffré par Android Keystore.")));
        content.addView(action("Déconnecter", v -> disconnect()));
    }

    private void showConfig() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), 0);
        EditText user = input("Utilisateur HiveMQ", InputType.TYPE_CLASS_TEXT);
        EditText pass = input("Mot de passe HiveMQ", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText topic = input("Préfixe MQTT", InputType.TYPE_CLASS_TEXT);
        user.setText(SecureConfig.user(this));
        topic.setText(SecureConfig.topic(this));
        box.addView(user); box.addView(pass); box.addView(topic);
        new AlertDialog.Builder(this)
                .setTitle("Connexion HiveMQ Cloud")
                .setMessage(SecureConfig.DEFAULT_HOST + ":" + SecureConfig.DEFAULT_PORT)
                .setView(box)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    try {
                        SecureConfig.save(this, user.getText().toString(), pass.getText().toString(), topic.getText().toString());
                        startMqttService();
                        status.setText("Connexion automatique active");
                    } catch (Exception e) { message("Erreur de chiffrement : " + e.getMessage()); }
                }).show();
    }

    private void startMqttService() {
        Intent i = new Intent(this, MqttBackgroundService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void disconnect() {
        SecureConfig.disable(this);
        stopService(new Intent(this, MqttBackgroundService.class));
        status.setText("Déconnecté");
        message("Connexion automatique désactivée.");
    }

    private void applyData(String topic, String payload) {
        try {
            JSONObject j = new JSONObject(payload);
            Double s = number(j, "speedKmh", "speed_kmh", "speed");
            Double b = number(j, "soc", "batterySoc", "battery");
            Double p = number(j, "powerKw", "power_kw", "power");
            Double r = number(j, "rangeKm", "range_km", "range");
            Double lat = number(j, "latitude", "lat");
            Double lon = number(j, "longitude", "lon", "lng");
            if (speed != null && s != null) speed.setText("Vitesse\n" + String.format(Locale.getDefault(), "%.1f km/h", s));
            if (battery != null && b != null) battery.setText("Batterie\n" + String.format(Locale.getDefault(), "%.0f %%", b));
            if (power != null && p != null) power.setText("Puissance\n" + String.format(Locale.getDefault(), "%.1f kW", p));
            if (range != null && r != null) range.setText("Autonomie\n" + String.format(Locale.getDefault(), "%.0f km", r));
            if (position != null && lat != null && lon != null) position.setText(String.format(Locale.US, "Position actuelle\n%.6f, %.6f", lat, lon));
            if (lastUpdate != null) lastUpdate.setText("Dernière mise à jour\n" + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
            if (raw != null) raw.setText("Dernier message MQTT\n" + topic + "\n" + payload);
        } catch (Exception ignored) {
            if (raw != null) raw.setText("Dernier message MQTT\n" + payload);
        }
    }

    private static Double number(JSONObject j, String... keys) {
        for (String k : keys) if (j.has(k) && !j.isNull(k)) try { return j.getDouble(k); } catch (Exception ignored) {}
        return null;
    }

    private TextView hero(String a, String b) {
        TextView v = card(a, b + "\n\nBYD SEAL U DM-i");
        v.setMinHeight(dp(190)); v.setGravity(Gravity.CENTER);
        return v;
    }

    private TextView section(String text) {
        TextView v = label(text, 24, Color.WHITE, true);
        v.setPadding(dp(4), dp(12), 0, dp(12));
        return v;
    }

    private TextView metric(String title, String value) {
        TextView v = label(title + "\n" + value, 20, Color.WHITE, true);
        styleCard(v); v.setGravity(Gravity.CENTER); v.setMinHeight(dp(110));
        return v;
    }

    private TextView card(String title, String value) {
        TextView v = label(title + "\n" + value, 16, Color.WHITE, false);
        styleCard(v); return v;
    }

    private Button action(String text, View.OnClickListener l) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(16); b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); b.setBackgroundColor(CARD); b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(60)); lp.setMargins(0, dp(5), 0, dp(5)); b.setLayoutParams(lp); return b;
    }

    private void styleCard(TextView v) {
        v.setBackgroundColor(CARD); v.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(dp(4), dp(5), dp(4), dp(5)); v.setLayoutParams(lp);
    }

    private LinearLayout row() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); return r; }
    private LinearLayout.LayoutParams weighted() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f); p.setMargins(dp(3), 0, dp(3), 0); return p; }
    private TextView label(String t, int s, int c, boolean bold) { TextView v = new TextView(this); v.setText(t); v.setTextSize(s); v.setTextColor(c); if (bold) v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    private EditText input(String hint, int type) { EditText e = new EditText(this); e.setHint(hint); e.setInputType(type); e.setSingleLine(true); return e; }
    private void message(String s) { new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK", null).show(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
