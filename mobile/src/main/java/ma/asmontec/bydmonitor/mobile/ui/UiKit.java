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

    public static final int BACKGROUND = Color.rgb(11, 17, 24);
    public static final int CARD = Color.rgb(24, 32, 41);
    public static final int ACCENT = Color.rgb(46, 230, 184);
    public static final int TEXT_PRIMARY = Color.WHITE;
    public static final int TEXT_MUTED = Color.rgb(148, 160, 172);
    public static final int ALERT_RED = Color.rgb(255, 92, 92);
    public static final int ALERT_ORANGE = Color.rgb(255, 167, 38);

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

    public static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
