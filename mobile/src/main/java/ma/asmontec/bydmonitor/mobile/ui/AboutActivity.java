package ma.asmontec.bydmonitor.mobile.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiKit.BACKGROUND);
        LinearLayout root = UiKit.screen(this);
        root.addView(UiKit.sectionTitle(this, "À propos"));

        LinearLayout card = UiKit.card(this);
        addParagraph(card, "BYD Monitor Mobile — version 1.0.0");
        addParagraph(card, "Application de surveillance en lecture seule, connectée à l'agent voiture "
                + "via MQTT/TLS sur HiveMQ Cloud. Aucune commande n'est envoyée au véhicule.");
        addParagraph(card, "Fonctions actives : télémétrie GPS, batterie et réseau, historique de trajets, "
                + "journal d'événements, notifications configurables.");
        addParagraph(card, "Fonctions préparées mais non disponibles : lecture des données BYD/DiLink "
                + "(SOC, autonomie, puissance, couple, température batterie réels), flux caméra, "
                + "commandes distantes, carte en ligne (aucune clé Maps/Mapbox configurée), scan QR.");
        root.addView(card);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void addParagraph(LinearLayout card, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(UiKit.TEXT_MUTED);
        view.setTextSize(14f);
        view.setPadding(0, 0, 0, UiKit.dp(this, 14));
        card.addView(view);
    }
}
