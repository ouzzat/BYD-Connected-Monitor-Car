package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;
import ma.asmontec.bydmonitor.mobile.db.AppDatabaseHelper;

/**
 * Écran Trajets : historique et analyse télémétrique. Le retour au
 * Tableau de bord se fait via le menu latéral ; le détail d'un trajet
 * (graphiques vitesse/puissance/altitude) ouvre {@link TripDetailActivity}.
 */
public final class TripsView extends ScrollView {

    public static final String EXTRA_TRIP_ID = "trip_id";

    public TripsView(Context context) {
        super(context);
        setBackgroundColor(UiKit.BACKGROUND);
        AppDatabaseHelper db = VehicleStateRepository.getInstance(context).database();

        LinearLayout root = UiKit.screen(context);
        root.addView(UiKit.subtle(context, "Analyse télémétrique : énergie, consommation, distance, graphiques."));

        List<AppDatabaseHelper.TripRow> trips = db.listTrips(100);
        if (trips.isEmpty()) {
            root.addView(UiKit.subtle(context, "Aucun trajet enregistré pour le moment."));
        }
        for (AppDatabaseHelper.TripRow trip : trips) {
            LinearLayout card = UiKit.card(context);

            TextView title = new TextView(context);
            title.setText(UiKit.fmtTimestamp(trip.startTime)
                    + (trip.endTime == null ? " (en cours)" : " → " + UiKit.fmtTimestamp(trip.endTime)));
            title.setTextColor(UiKit.TEXT_PRIMARY);
            title.setTextSize(15f);
            card.addView(title);

            card.addView(UiKit.row(context, "Distance", String.format(Locale.FRANCE, "%.1f km", trip.distanceKm)));
            card.addView(UiKit.row(context, "Vitesse moyenne",
                    trip.avgSpeedKmh == null ? "—" : String.format(Locale.FRANCE, "%.0f km/h", trip.avgSpeedKmh)));
            card.addView(UiKit.row(context, "Vitesse maximale",
                    trip.maxSpeedKmh == null ? "—" : String.format(Locale.FRANCE, "%.0f km/h", trip.maxSpeedKmh)));
            card.addView(UiKit.row(context, "Consommation moyenne", "—"));
            card.addView(UiKit.row(context, "Énergie utilisée", "—"));

            TextView details = new TextView(context);
            details.setText("Détails du trajet ›");
            details.setTextColor(UiKit.ACCENT);
            details.setTextSize(13f);
            details.setPadding(0, UiKit.dp(context, 8), 0, 0);
            card.addView(details);

            card.setOnClickListener(v -> {
                Intent intent = new Intent(context, TripDetailActivity.class);
                intent.putExtra(EXTRA_TRIP_ID, trip.id);
                context.startActivity(intent);
            });
            root.addView(card);
        }

        addView(root);
    }
}
