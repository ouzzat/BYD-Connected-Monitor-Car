package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Caméra directe : consolide les six angles caméra et un radar de
 * proximité décoratif. Aucun flux vidéo ni donnée radar n'est simulé —
 * tout reste à l'état "en attente" tant qu'aucune interface BYD/DiLink
 * autorisée n'est disponible.
 */
public final class CameraDirectView extends ScrollView {

    private static final String[] ANGLES = {
            "Avant", "Arrière", "Gauche", "Droite", "Vue 360°", "Intérieur"
    };
    private static final String WAITING_MESSAGE = "Module caméra en attente d'une interface véhicule autorisée";

    public CameraDirectView(Context context) {
        super(context);
        setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout root = UiKit.screen(context);
        root.addView(UiKit.subtle(context,
                "Aucun flux vidéo simulé. Les caméras et le radar de proximité restent inactifs tant "
                        + "qu'aucune interface véhicule officielle n'est disponible."));

        LinearLayout radarCard = UiKit.card(context);
        radarCard.setGravity(Gravity.CENTER);
        radarCard.addView(buildRadarPlaceholder(context));
        TextView radarLabel = new TextView(context);
        radarLabel.setText("Radar de proximité — hors service");
        radarLabel.setTextColor(UiKit.TEXT_MUTED);
        radarLabel.setTextSize(13f);
        radarLabel.setGravity(Gravity.CENTER);
        radarLabel.setPadding(0, UiKit.dp(context, 12), 0, 0);
        radarCard.addView(radarLabel);
        root.addView(radarCard);

        LinearLayout alertsCard = UiKit.card(context);
        alertsCard.addView(UiKit.sectionTitle(context, "Alertes de mouvement"));
        alertsCard.addView(UiKit.subtle(context,
                "Nécessitent le radar de proximité et les caméras ci-dessous : indisponibles pour le moment."));
        root.addView(alertsCard);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(2);
        for (String angle : ANGLES) {
            LinearLayout tile = UiKit.card(context);
            TextView title = new TextView(context);
            title.setText(angle);
            title.setTextColor(UiKit.TEXT_PRIMARY);
            title.setTextSize(15f);
            tile.addView(title);

            TextView state = new TextView(context);
            state.setText(WAITING_MESSAGE);
            state.setTextColor(UiKit.TEXT_MUTED);
            state.setTextSize(12f);
            state.setPadding(0, UiKit.dp(context, 6), 0, 0);
            tile.addView(state);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(UiKit.dp(context, 4), UiKit.dp(context, 4), UiKit.dp(context, 4), UiKit.dp(context, 4));
            tile.setLayoutParams(params);
            grid.addView(tile);
        }
        root.addView(grid);

        addView(root);
    }

    private FrameLayout buildRadarPlaceholder(Context context) {
        FrameLayout frame = new FrameLayout(context);
        int[] sizesDp = {180, 130, 80};
        for (int sizeDp : sizesDp) {
            android.view.View ring = new android.view.View(context);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setStroke(UiKit.dp(context, 1), UiKit.DIVIDER);
            ring.setBackground(drawable);
            int size = UiKit.dp(context, sizeDp);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.gravity = Gravity.CENTER;
            frame.addView(ring, params);
        }
        TextView center = new TextView(context);
        center.setText("—");
        center.setTextColor(UiKit.TEXT_MUTED);
        center.setTextSize(16f);
        FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        centerParams.gravity = Gravity.CENTER;
        frame.addView(center, centerParams);

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                UiKit.dp(context, 200), UiKit.dp(context, 200));
        frame.setLayoutParams(frameParams);
        return frame;
    }
}
