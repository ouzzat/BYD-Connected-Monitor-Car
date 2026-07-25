package ma.asmontec.bydmonitor.mobile.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;
import ma.asmontec.bydmonitor.mobile.db.AppDatabaseHelper;
import java.util.Locale;

public final class TripHistoryActivity extends Activity {

    public static final String EXTRA_TRIP_ID = "trip_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppDatabaseHelper db = VehicleStateRepository.getInstance(this).database();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiKit.BACKGROUND);
        LinearLayout root = UiKit.screen(this);
        root.addView(UiKit.sectionTitle(this, "Historique des trajets"));

        java.util.List<AppDatabaseHelper.TripRow> trips = db.listTrips(100);
        if (trips.isEmpty()) {
            root.addView(UiKit.subtle(this, "Aucun trajet enregistré pour le moment."));
        }
        for (AppDatabaseHelper.TripRow trip : trips) {
            LinearLayout card = UiKit.card(this);

            TextView title = new TextView(this);
            title.setText(UiKit.fmtTimestamp(trip.startTime)
                    + (trip.endTime == null ? " (en cours)" : " → " + UiKit.fmtTimestamp(trip.endTime)));
            title.setTextColor(UiKit.TEXT_PRIMARY);
            title.setTextSize(15f);
            card.addView(title);

            card.addView(UiKit.row(this, "Distance", String.format(Locale.FRANCE, "%.1f km", trip.distanceKm)));
            card.addView(UiKit.row(this, "Vitesse moyenne",
                    trip.avgSpeedKmh == null ? "—" : String.format(Locale.FRANCE, "%.0f km/h", trip.avgSpeedKmh)));
            card.addView(UiKit.row(this, "Vitesse maximale",
                    trip.maxSpeedKmh == null ? "—" : String.format(Locale.FRANCE, "%.0f km/h", trip.maxSpeedKmh)));

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, TripDetailActivity.class);
                intent.putExtra(EXTRA_TRIP_ID, trip.id);
                startActivity(intent);
            });
            root.addView(card);
        }

        scroll.addView(root);
        setContentView(scroll);
    }
}
