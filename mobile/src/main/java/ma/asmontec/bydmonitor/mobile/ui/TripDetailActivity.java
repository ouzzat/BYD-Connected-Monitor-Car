package ma.asmontec.bydmonitor.mobile.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;
import ma.asmontec.bydmonitor.mobile.db.AppDatabaseHelper;
import java.util.ArrayList;
import java.util.List;

public final class TripDetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long tripId = getIntent().getLongExtra(TripsView.EXTRA_TRIP_ID, -1);
        AppDatabaseHelper db = VehicleStateRepository.getInstance(this).database();
        List<AppDatabaseHelper.TripPointRow> points = db.listTripPoints(tripId);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiKit.BACKGROUND);
        LinearLayout root = UiKit.screen(this);
        root.addView(UiKit.sectionTitle(this, "Détail du trajet"));

        if (points.isEmpty()) {
            root.addView(UiKit.subtle(this, "Aucun point enregistré pour ce trajet."));
        } else {
            root.addView(UiKit.subtle(this, "Vitesse / temps"));
            root.addView(chart(extract(points, p -> p.speedKmh)));

            root.addView(UiKit.subtle(this, "Puissance / temps"));
            root.addView(chart(extract(points, p -> p.powerKw)));

            root.addView(UiKit.subtle(this, "Altitude / temps"));
            root.addView(chart(extract(points, p -> p.altitude)));
        }

        scroll.addView(root);
        setContentView(scroll);
    }

    private SimpleLineChartView chart(List<Float> values) {
        SimpleLineChartView chart = new SimpleLineChartView(this);
        chart.setValues(values);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 140));
        params.setMargins(0, 0, 0, UiKit.dp(this, 20));
        chart.setLayoutParams(params);
        return chart;
    }

    private interface FieldExtractor {
        Double get(AppDatabaseHelper.TripPointRow point);
    }

    private List<Float> extract(List<AppDatabaseHelper.TripPointRow> points, FieldExtractor extractor) {
        List<Float> values = new ArrayList<>();
        for (AppDatabaseHelper.TripPointRow point : points) {
            Double value = extractor.get(point);
            values.add(value == null ? 0f : value.floatValue());
        }
        return values;
    }
}
