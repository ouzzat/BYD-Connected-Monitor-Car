package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Petits utilitaires partagés pour la palette sombre "premium" de l'application mobile. */
public final class UiKit {

    public static final int BACKGROUND = Color.parseColor("#0A0E12");
    public static final int SURFACE = Color.parseColor("#11161C");
    public static final int CARD = Color.parseColor("#161C24");
    public static final int ACCENT = Color.parseColor("#38D9A9");
    public static final int TEXT_PRIMARY = Color.WHITE;
    public static final int TEXT_MUTED = Color.rgb(148, 160, 172);
    public static final int ALERT_RED = Color.rgb(255, 92, 92);
    public static final int ALERT_ORANGE = Color.rgb(255, 167, 38);
    public static final int DIVIDER = Color.parseColor("#232B35");

    /**
     * Approximation de la police "Inter" demandée par la maquette : aucun
     * fichier de police n'est fourni ni téléchargé dans ce projet (pas de
     * licence embarquée), donc la police système la plus proche est
     * utilisée à la place.
     */
    public static final String FONT_FAMILY = "sans-serif";
    public static final String FONT_FAMILY_MEDIUM = "sans-serif-medium";

    private UiKit() {
    }

    public static LinearLayout screen(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(BACKGROUND);
        layout.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        return layout;
    }

    public static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(context, 14));
        card.setBackground(bg);
        card.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(context, 12));
        card.setLayoutParams(params);
        return card;
    }

    public static TextView sectionTitle(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(TEXT_PRIMARY);
        view.setTextSize(18f);
        view.setPadding(0, dp(context, 4), 0, dp(context, 10));
        return view;
    }

    public static TextView subtle(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(TEXT_MUTED);
        view.setTextSize(13f);
        return view;
    }

    /** Une ligne "libellé — valeur" alignée, réutilisée dans la plupart des écrans. */
    public static View row(Context context, String label, String value) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(context, 6), 0, dp(context, 6));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(TEXT_MUTED);
        labelView.setTextSize(14f);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(labelView, labelParams);

        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextColor(TEXT_PRIMARY);
        valueView.setTextSize(14f);
        valueView.setGravity(Gravity.END);
        row.addView(valueView);

        return row;
    }

    public static String fmt1(Double value, String unit) {
        return value == null ? "—" : String.format(Locale.FRANCE, "%.1f %s", value, unit).trim();
    }

    public static String fmt0(Double value, String unit) {
        return value == null ? "—" : String.format(Locale.FRANCE, "%.0f %s", value, unit).trim();
    }

    public static String fmtInt(Integer value, String unit) {
        return value == null ? "—" : (value + (unit.isEmpty() ? "" : " " + unit));
    }

    public static String fmtCoord(Double value) {
        return value == null ? "—" : String.format(Locale.FRANCE, "%.5f", value);
    }

    public static String fmtText(String value) {
        return value == null || value.isEmpty() ? "—" : value;
    }

    public static String fmtTimestamp(long millis) {
        if (millis <= 0) {
            return "—";
        }
        return new java.text.SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRANCE).format(new java.util.Date(millis));
    }

    /** Bouton visuellement présent mais désactivé par défaut (aucune API officielle branchée). */
    public static android.widget.Button disabledButton(Context context, String label) {
        android.widget.Button button = new android.widget.Button(context);
        button.setText(label);
        button.setEnabled(false);
        button.setAlpha(0.45f);
        return button;
    }

    /** Étiquette d'onglet simple (Position/Confort, Journal/Alertes…) sans dépendance TabLayout. */
    public static TextView tabLabel(Context context, String text, boolean selected) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(14f);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(context, 10), 0, dp(context, 10));
        view.setTextColor(selected ? BACKGROUND : TEXT_MUTED);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(context, 10));
        bg.setColor(selected ? ACCENT : SURFACE);
        view.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(context, 4), 0, dp(context, 4), dp(context, 12));
        view.setLayoutParams(params);
        return view;
    }

    public static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
