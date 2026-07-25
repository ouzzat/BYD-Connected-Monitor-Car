package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.List;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;
import ma.asmontec.bydmonitor.mobile.db.AppDatabaseHelper;
import ma.asmontec.bydmonitor.mobile.notify.NotificationPrefs;

/** Journal : historique des événements et réglages des alertes (switches), sous forme d'onglets. */
public final class LogsView extends ScrollView {

    private LinearLayout journalTab;
    private LinearLayout alertsTab;
    private TextView tabJournal;
    private TextView tabAlerts;

    public LogsView(Context context) {
        super(context);
        setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout root = UiKit.screen(context);

        LinearLayout tabsRow = new LinearLayout(context);
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        tabJournal = UiKit.tabLabel(context, "Journal", true);
        tabAlerts = UiKit.tabLabel(context, "Alertes", false);
        tabsRow.addView(tabJournal);
        tabsRow.addView(tabAlerts);
        root.addView(tabsRow);

        journalTab = buildJournalTab(context);
        alertsTab = buildAlertsTab(context);
        alertsTab.setVisibility(View.GONE);
        root.addView(journalTab);
        root.addView(alertsTab);

        tabJournal.setOnClickListener(v -> showTab(true));
        tabAlerts.setOnClickListener(v -> showTab(false));

        addView(root);
    }

    private void showTab(boolean journal) {
        journalTab.setVisibility(journal ? View.VISIBLE : View.GONE);
        alertsTab.setVisibility(journal ? View.GONE : View.VISIBLE);
        tabJournal.setTextColor(journal ? UiKit.ACCENT : UiKit.TEXT_MUTED);
        tabAlerts.setTextColor(journal ? UiKit.TEXT_MUTED : UiKit.ACCENT);
    }

    private LinearLayout buildJournalTab(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        AppDatabaseHelper db = VehicleStateRepository.getInstance(context).database();
        List<AppDatabaseHelper.EventRow> events = db.listEvents(200);
        if (events.isEmpty()) {
            container.addView(UiKit.subtle(context, "Aucun événement enregistré pour le moment."));
        }
        for (AppDatabaseHelper.EventRow event : events) {
            LinearLayout card = UiKit.card(context);
            TextView title = new TextView(context);
            title.setText(event.type);
            title.setTextColor(UiKit.TEXT_PRIMARY);
            title.setTextSize(15f);
            card.addView(title);

            if (event.message != null && !event.message.isEmpty()) {
                TextView message = new TextView(context);
                message.setText(event.message);
                message.setTextColor(UiKit.TEXT_MUTED);
                message.setTextSize(13f);
                card.addView(message);
            }

            TextView time = new TextView(context);
            time.setText(UiKit.fmtTimestamp(event.timestamp));
            time.setTextColor(UiKit.TEXT_MUTED);
            time.setTextSize(12f);
            card.addView(time);

            container.addView(card);
        }
        return container;
    }

    private LinearLayout buildAlertsTab(Context context) {
        LinearLayout card = UiKit.card(context);
        card.addView(UiKit.sectionTitle(context, "Réglages des alertes"));
        NotificationPrefs prefs = new NotificationPrefs(context);
        addToggle(context, card, prefs, NotificationPrefs.Kind.DOOR, "Ouverture / fermeture des portes");
        addToggle(context, card, prefs, NotificationPrefs.Kind.TRIP_START_END, "Début / fin de trajet");
        addToggle(context, card, prefs, NotificationPrefs.Kind.LOW_BATTERY, "Batterie faible");
        addToggle(context, card, prefs, NotificationPrefs.Kind.CHARGE_COMPLETE, "Fin de charge");
        addToggle(context, card, prefs, NotificationPrefs.Kind.VEHICLE_OFFLINE, "Véhicule hors ligne");
        addToggle(context, card, prefs, NotificationPrefs.Kind.VEHICLE_MOVING, "Déplacement du véhicule");
        addToggle(context, card, prefs, NotificationPrefs.Kind.GPS_LOST, "Problème de localisation");
        addToggle(context, card, prefs, NotificationPrefs.Kind.SERVICE_ERROR, "Erreur du service voiture");

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(card);
        return container;
    }

    private void addToggle(Context context, LinearLayout card, NotificationPrefs prefs,
                            NotificationPrefs.Kind kind, String label) {
        Switch toggle = new Switch(context);
        toggle.setText(label);
        toggle.setTextColor(UiKit.TEXT_PRIMARY);
        toggle.setChecked(prefs.isEnabled(kind));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setEnabled(kind, isChecked));
        card.addView(toggle);
    }
}
