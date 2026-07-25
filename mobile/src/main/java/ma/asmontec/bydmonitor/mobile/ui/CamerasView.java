package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Aucun flux vidéo n'est simulé. Les six emplacements caméra restent en
 * attente tant qu'aucune interface BYD/DiLink autorisée n'est disponible.
 */
public final class CamerasView extends ScrollView {

    private static final String[] LABELS = {
            "Caméra avant", "Caméra arrière", "Caméra gauche", "Caméra droite", "Vue 360°", "Caméra intérieure"
    };

    public CamerasView(Context context) {
        super(context);
        setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout root = UiKit.screen(context);
        root.addView(UiKit.sectionTitle(context, "Caméras"));

        for (String label : LABELS) {
            LinearLayout card = UiKit.card(context);
            TextView title = new TextView(context);
            title.setText(label);
            title.setTextColor(UiKit.TEXT_PRIMARY);
            title.setTextSize(16f);
            card.addView(title);

            TextView state = new TextView(context);
            state.setText("Module caméra en attente d'une interface véhicule autorisée");
            state.setTextColor(UiKit.TEXT_MUTED);
            state.setTextSize(13f);
            state.setPadding(0, UiKit.dp(context, 6), 0, 0);
            card.addView(state);

            root.addView(card);
        }

        addView(root);
    }
}
