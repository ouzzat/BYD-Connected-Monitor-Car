package ma.asmontec.bydmonitor.mobile.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import ma.asmontec.bydmonitor.mobile.notify.NotificationPrefs;

public final class NotificationsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationPrefs prefs = new NotificationPrefs(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiKit.BACKGROUND);
        LinearLayout root = UiKit.screen(this);
        root.addView(UiKit.sectionTitle(this, "Notifications"));

        LinearLayout card = UiKit.card(this);
        addToggle(card, prefs, NotificationPrefs.Kind.DOOR, "Ouverture / fermeture des portes");
        addToggle(card, prefs, NotificationPrefs.Kind.TRIP_START_END, "Début / fin de trajet");
        addToggle(card, prefs, NotificationPrefs.Kind.LOW_BATTERY, "Batterie faible");
        addToggle(card, prefs, NotificationPrefs.Kind.CHARGE_COMPLETE, "Fin de charge");
        addToggle(card, prefs, NotificationPrefs.Kind.VEHICLE_OFFLINE, "Véhicule hors ligne");
        addToggle(card, prefs, NotificationPrefs.Kind.VEHICLE_MOVING, "Déplacement du véhicule");
        addToggle(card, prefs, NotificationPrefs.Kind.GPS_LOST, "Problème de localisation");
        addToggle(card, prefs, NotificationPrefs.Kind.SERVICE_ERROR, "Erreur du service voiture");
        root.addView(card);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void addToggle(LinearLayout card, NotificationPrefs prefs, NotificationPrefs.Kind kind, String label) {
        Switch toggle = new Switch(this);
        toggle.setText(label);
        toggle.setTextColor(UiKit.TEXT_PRIMARY);
        toggle.setChecked(prefs.isEnabled(kind));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setEnabled(kind, isChecked));
        card.addView(toggle);
    }
}
