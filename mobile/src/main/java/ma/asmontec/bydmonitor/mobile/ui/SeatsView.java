package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Sièges : mémoires de position et confort thermique. Toutes les
 * commandes restent désactivées par défaut — aucune API officielle
 * BYD/DiLink authentifiée n'est branchée dans cette version.
 */
public final class SeatsView extends ScrollView {

    private LinearLayout positionTab;
    private LinearLayout comfortTab;
    private TextView tabPosition;
    private TextView tabComfort;

    public SeatsView(Context context) {
        super(context);
        setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout root = UiKit.screen(context);
        root.addView(UiKit.subtle(context,
                "Désactivé par défaut : nécessite une API officielle, une authentification forte et une "
                        + "confirmation utilisateur avant tout envoi de commande."));

        LinearLayout tabsRow = new LinearLayout(context);
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        tabPosition = UiKit.tabLabel(context, "Position", true);
        tabComfort = UiKit.tabLabel(context, "Confort", false);
        tabsRow.addView(tabPosition);
        tabsRow.addView(tabComfort);
        root.addView(tabsRow);

        positionTab = buildPositionTab(context);
        comfortTab = buildComfortTab(context);
        comfortTab.setVisibility(android.view.View.GONE);
        root.addView(positionTab);
        root.addView(comfortTab);

        tabPosition.setOnClickListener(v -> showTab(true));
        tabComfort.setOnClickListener(v -> showTab(false));

        addView(root);
    }

    private void showTab(boolean position) {
        positionTab.setVisibility(position ? android.view.View.VISIBLE : android.view.View.GONE);
        comfortTab.setVisibility(position ? android.view.View.GONE : android.view.View.VISIBLE);
        tabPosition.setTextColor(position ? UiKit.ACCENT : UiKit.TEXT_MUTED);
        tabComfort.setTextColor(position ? UiKit.TEXT_MUTED : UiKit.ACCENT);
    }

    private LinearLayout buildPositionTab(Context context) {
        LinearLayout card = UiKit.card(context);
        card.addView(UiKit.sectionTitle(context, "Mémoires de position"));
        for (int slot = 1; slot <= 3; slot++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, UiKit.dp(context, 6), 0, UiKit.dp(context, 6));

            TextView label = new TextView(context);
            label.setText("Mémoire " + slot);
            label.setTextColor(UiKit.TEXT_PRIMARY);
            label.setTextSize(14f);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(label, labelParams);

            Button recall = UiKit.disabledButton(context, "Rappeler");
            row.addView(recall);
            card.addView(row);
        }
        Button save = UiKit.disabledButton(context, "Enregistrer la position actuelle");
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        saveParams.topMargin = UiKit.dp(context, 8);
        card.addView(save, saveParams);
        return card;
    }

    private LinearLayout buildComfortTab(Context context) {
        LinearLayout card = UiKit.card(context);
        card.addView(UiKit.sectionTitle(context, "Confort thermique"));
        card.addView(comfortRow(context, "Climatisation automatique"));
        card.addView(comfortRow(context, "Chauffage siège avant gauche"));
        card.addView(comfortRow(context, "Chauffage siège avant droit"));
        card.addView(comfortRow(context, "Ventilation siège avant gauche"));
        card.addView(comfortRow(context, "Ventilation siège avant droit"));
        return card;
    }

    private LinearLayout comfortRow(Context context, String label) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, UiKit.dp(context, 6), 0, UiKit.dp(context, 6));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(UiKit.TEXT_PRIMARY);
        labelView.setTextSize(14f);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(labelView, labelParams);

        android.widget.Switch toggle = new android.widget.Switch(context);
        toggle.setEnabled(false);
        toggle.setAlpha(0.45f);
        row.addView(toggle);
        return row;
    }
}
