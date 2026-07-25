package ma.asmontec.bydmonitor.mobile.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Interface préparée pour de futures commandes distantes. Aucun bouton
 * n'est activé : aucune API officielle BYD/DiLink authentifiée n'est
 * disponible dans cette version. Rien n'est jamais envoyé au véhicule.
 */
public final class RemoteCommandsActivity extends Activity {

    private static final String[] COMMANDS = {
            "Verrouiller", "Déverrouiller", "Coffre", "Klaxon",
            "Climatisation", "Ventilation des sièges", "Chauffage des sièges"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiKit.BACKGROUND);
        LinearLayout root = UiKit.screen(this);

        root.addView(UiKit.sectionTitle(this, "Commandes distantes"));
        TextView warning = new TextView(this);
        warning.setText("Désactivées par défaut. Aucune commande n'est envoyée sans API officielle, "
                + "authentification forte, confirmation explicite et validation de l'état sécurisé du véhicule.");
        warning.setTextColor(UiKit.ALERT_ORANGE);
        warning.setTextSize(13f);
        warning.setPadding(0, 0, 0, UiKit.dp(this, 16));
        root.addView(warning);

        LinearLayout card = UiKit.card(this);
        for (String command : COMMANDS) {
            Button button = new Button(this);
            button.setText(command);
            button.setEnabled(false);
            button.setAlpha(0.5f);
            button.setOnClickListener(v -> Toast.makeText(this,
                    "Indisponible : aucune interface véhicule autorisée", Toast.LENGTH_SHORT).show());
            card.addView(button);
        }
        root.addView(card);

        scroll.addView(root);
        setContentView(scroll);
    }
}
