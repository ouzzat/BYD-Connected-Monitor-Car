package ma.asmontec.bydmonitor.mobile.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;
import ma.asmontec.bydmonitor.mobile.db.AppDatabaseHelper;

public final class EventsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppDatabaseHelper db = VehicleStateRepository.getInstance(this).database();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiKit.BACKGROUND);
        LinearLayout root = UiKit.screen(this);
        root.addView(UiKit.sectionTitle(this, "Journal des événements"));

        java.util.List<AppDatabaseHelper.EventRow> events = db.listEvents(200);
        if (events.isEmpty()) {
            root.addView(UiKit.subtle(this, "Aucun événement enregistré pour le moment."));
        }
        for (AppDatabaseHelper.EventRow event : events) {
            LinearLayout card = UiKit.card(this);
            TextView title = new TextView(this);
            title.setText(event.type);
            title.setTextColor(UiKit.TEXT_PRIMARY);
            title.setTextSize(15f);
            card.addView(title);

            if (event.message != null && !event.message.isEmpty()) {
                TextView message = new TextView(this);
                message.setText(event.message);
                message.setTextColor(UiKit.TEXT_MUTED);
                message.setTextSize(13f);
                card.addView(message);
            }

            TextView time = new TextView(this);
            time.setText(UiKit.fmtTimestamp(event.timestamp));
            time.setTextColor(UiKit.TEXT_MUTED);
            time.setTextSize(12f);
            card.addView(time);

            root.addView(card);
        }

        scroll.addView(root);
        setContentView(scroll);
    }
}
