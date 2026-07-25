package com.asmontec.bydavm.car;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.asmontec.bydavm.car.config.DeveloperConfigActivity;
import com.asmontec.bydavm.car.service.CarAgentService;
import com.asmontec.bydavm.car.service.ServiceStatus;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity implements ServiceStatus.Listener {

    private static final int REQUEST_PERMISSIONS = 100;

    private TextView statusText;
    private TextView detailText;
    private TextView updatedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.rgb(11, 17, 24));

        TextView title = new TextView(this);
        title.setText("BYD Connected Monitor — Car Agent");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26f);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("Lecture seule — aucune commande véhicule");
        subtitle.setTextColor(Color.rgb(79, 195, 247));
        subtitle.setTextSize(16f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 32);

        statusText = new TextView(this);
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(20f);
        statusText.setGravity(Gravity.CENTER);

        detailText = new TextView(this);
        detailText.setTextColor(Color.rgb(150, 150, 150));
        detailText.setTextSize(14f);
        detailText.setGravity(Gravity.CENTER);
        detailText.setPadding(0, 8, 0, 8);

        updatedText = new TextView(this);
        updatedText.setTextColor(Color.rgb(100, 100, 100));
        updatedText.setTextSize(12f);
        updatedText.setGravity(Gravity.CENTER);
        updatedText.setPadding(0, 0, 0, 32);

        Button configButton = new Button(this);
        configButton.setText("Configuration développeur");
        configButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DeveloperConfigActivity.class)));

        root.addView(title);
        root.addView(subtitle);
        root.addView(statusText);
        root.addView(detailText);
        root.addView(updatedText);
        root.addView(configButton);
        setContentView(root);

        requestRequiredPermissions();
        CarAgentService.start(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ServiceStatus.register(this);
    }

    @Override
    protected void onStop() {
        ServiceStatus.unregister(this);
        super.onStop();
    }

    @Override
    public void onStatusChanged(ServiceStatus.State state, String detail, long timestamp) {
        statusText.setText(labelFor(state));
        detailText.setText(detail == null ? "" : detail);
        if (timestamp > 0) {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(new Date(timestamp));
            updatedText.setText("Dernière mise à jour : " + time);
        }
    }

    private String labelFor(ServiceStatus.State state) {
        switch (state) {
            case CONFIG_REQUIRED:
                return "Configuration requise";
            case CONNECTING:
                return "Connexion en cours…";
            case CONNECTED:
                return "Connecté à HiveMQ";
            case RECONNECTING:
                return "Reconnexion en cours…";
            case GPS_UNAVAILABLE:
                return "Connecté — GPS indisponible";
            case IDLE:
            default:
                return "Service actif";
        }
    }

    private void requestRequiredPermissions() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }
}
