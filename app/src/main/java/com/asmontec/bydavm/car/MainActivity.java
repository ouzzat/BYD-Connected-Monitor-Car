package com.asmontec.bydavm.car;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
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
        title.setText("BYD Connected Monitor");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30f);
        title.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);
        status.setText("Version de test sécurisée\nLecture seule — aucune commande véhicule");
        status.setTextColor(Color.rgb(79, 195, 247));
        status.setTextSize(18f);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 24, 0, 0);

        root.addView(title);
        root.addView(status);
        setContentView(root);
    }
}
