package com.asmontec.bydavm.car;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;

public final class MainActivity extends Activity {
    private EditText user;
    private EditText pass;
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        requestRuntimePermissions();
        if (SecureConfig.enabled(this)) startAgent();
    }

    private LinearLayout buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 28, 36, 28);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(8, 14, 20));

        TextView title = label("BYD Connected Monitor — Car Agent", 28, Color.WHITE);
        TextView server = label("HiveMQ Cloud intégré • TLS 8883\nasmontec/byd/seal-u", 17, Color.rgb(55, 220, 170));
        server.setPadding(0, 8, 0, 22);
        root.addView(title); root.addView(server);

        user = field("Utilisateur HiveMQ", InputType.TYPE_CLASS_TEXT);
        pass = field("Mot de passe HiveMQ", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        user.setText(SecureConfig.user(this));
        root.addView(user); root.addView(pass);

        Button start = new Button(this);
        start.setText("ENREGISTRER ET DÉMARRER EN ARRIÈRE-PLAN");
        start.setOnClickListener(v -> saveAndStart());
        root.addView(start);

        Button stop = new Button(this);
        stop.setText("ARRÊTER LE SERVICE");
        stop.setOnClickListener(v -> {
            SecureConfig.disable(this);
            stopService(new Intent(this, CarMqttService.class));
            status.setText("Service arrêté");
        });
        root.addView(stop);

        status = label(SecureConfig.enabled(this) ? "Service configuré" : "Identifiants HiveMQ requis", 18, Color.LTGRAY);
        status.setPadding(0, 20, 0, 0);
        root.addView(status);
        return root;
    }

    private void saveAndStart() {
        try {
            if (user.getText().toString().trim().isEmpty()) {
                status.setText("Utilisateur HiveMQ requis");
                return;
            }
            SecureConfig.save(this, user.getText().toString(), pass.getText().toString(), SecureConfig.DEFAULT_TOPIC);
            pass.setText("");
            startAgent();
            status.setText("Agent actif • publication toutes les 5 secondes");
        } catch (Exception e) {
            status.setText("Erreur : " + e.getMessage());
        }
    }

    private void startAgent() {
        Intent i = new Intent(this, CarMqttService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 40);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41);
        }
    }

    private EditText field(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setHintTextColor(Color.GRAY); e.setTextColor(Color.WHITE); e.setInputType(type); e.setSingleLine(true);
        e.setPadding(12, 12, 12, 12);
        e.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER); return t;
    }
}
