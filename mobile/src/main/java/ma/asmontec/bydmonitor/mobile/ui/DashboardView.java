package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ma.asmontec.bydmonitor.mobile.data.VehicleState;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;

/**
 * Tableau de bord : fusionne l'ancien Accueil, l'écran État (jauges) et la
 * carte hors ligne, conformément à la nouvelle maquette à menu latéral.
 * Le widget batterie ouvre l'écran Trajets (historique).
 */
public final class DashboardView extends ScrollView implements VehicleStateRepository.Listener {

    private final VehicleStateRepository repository;
    private final TextView connectionBadge;
    private final TextView socValue;
    private final TextView rangeValue;
    private final LinearLayout quickStatsCard;
    private final LinearLayout gpsCard;
    private final LinearLayout gaugesCard;
    private final LinearLayout detailsCard;

    public DashboardView(Context context, Runnable onOpenTrips) {
        super(context);
        this.repository = VehicleStateRepository.getInstance(context);
        setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout root = UiKit.screen(context);

        connectionBadge = new TextView(context);
        connectionBadge.setTextSize(14f);
        connectionBadge.setPadding(0, 0, 0, UiKit.dp(context, 16));
        root.addView(connectionBadge);

        LinearLayout heroCard = UiKit.card(context);
        LinearLayout heroRow = new LinearLayout(context);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

        LinearLayout socBlock = new LinearLayout(context);
        socBlock.setOrientation(LinearLayout.VERTICAL);
        socBlock.setClickable(true);
        socBlock.setFocusable(true);
        socBlock.setOnClickListener(v -> onOpenTrips.run());
        TextView socLabel = UiKit.subtle(context, "Batterie · voir historique ›");
        socValue = new TextView(context);
        socValue.setTextColor(UiKit.ACCENT);
        socValue.setTextSize(30f);
        socBlock.addView(socLabel);
        socBlock.addView(socValue);
        heroRow.addView(socBlock, half);

        LinearLayout rangeBlock = new LinearLayout(context);
        rangeBlock.setOrientation(LinearLayout.VERTICAL);
        TextView rangeLabel = UiKit.subtle(context, "Autonomie EV");
        rangeValue = new TextView(context);
        rangeValue.setTextColor(Color.WHITE);
        rangeValue.setTextSize(30f);
        rangeBlock.addView(rangeLabel);
        rangeBlock.addView(rangeValue);
        heroRow.addView(rangeBlock, half);

        heroCard.addView(heroRow);
        root.addView(heroCard);

        quickStatsCard = UiKit.card(context);
        quickStatsCard.addView(UiKit.sectionTitle(context, "Puissance & vitesse"));
        root.addView(quickStatsCard);

        gpsCard = UiKit.card(context);
        gpsCard.addView(UiKit.sectionTitle(context, "GPS temps réel"));
        root.addView(gpsCard);

        gaugesCard = UiKit.card(context);
        gaugesCard.addView(UiKit.sectionTitle(context, "Télémétrie détaillée"));
        root.addView(gaugesCard);

        LinearLayout commandsCard = UiKit.card(context);
        commandsCard.addView(UiKit.sectionTitle(context, "Commandes rapides"));
        commandsCard.addView(UiKit.subtle(context,
                "Désactivées par défaut : aucune API officielle authentifiée n'est disponible."));
        LinearLayout commandsRow = new LinearLayout(context);
        commandsRow.setOrientation(LinearLayout.HORIZONTAL);
        commandsRow.setPadding(0, UiKit.dp(context, 12), 0, 0);
        for (String label : new String[]{"Verrouiller", "Déverrouiller", "Coffre", "Klaxon"}) {
            android.widget.Button button = UiKit.disabledButton(context, label);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(UiKit.dp(context, 2), 0, UiKit.dp(context, 2), 0);
            commandsRow.addView(button, params);
        }
        commandsCard.addView(commandsRow);
        root.addView(commandsCard);

        detailsCard = UiKit.card(context);
        detailsCard.addView(UiKit.sectionTitle(context, "État général"));
        root.addView(detailsCard);

        addView(root);
        onStateChanged(repository.currentState());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        repository.addListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        repository.removeListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onStateChanged(VehicleState state) {
        boolean online = Boolean.TRUE.equals(state.online);
        connectionBadge.setText(online ? "● Connecté" : "○ Hors ligne");
        connectionBadge.setTextColor(online ? UiKit.ACCENT : UiKit.ALERT_ORANGE);

        socValue.setText(state.soc == null ? "—" : UiKit.fmt0(state.soc, "%"));
        rangeValue.setText(state.rangeKm == null ? "—" : UiKit.fmt0(state.rangeKm, "km"));

        replaceRows(quickStatsCard, 1,
                UiKit.row(getContext(), "Vitesse", UiKit.fmt1(state.speedKmh, "km/h")),
                UiKit.row(getContext(), "Puissance", UiKit.fmt1(state.powerKw, "kW")),
                UiKit.row(getContext(), "Couple", UiKit.fmt1(state.torqueNm, "N·m")),
                UiKit.row(getContext(), "Température batterie", UiKit.fmt1(state.batteryTemp, "°C")),
                UiKit.row(getContext(), "Batterie unité Android", UiKit.fmtInt(state.androidBattery, "%")));

        replaceRows(gpsCard, 1,
                UiKit.row(getContext(), "Latitude", UiKit.fmtCoord(state.latitude)),
                UiKit.row(getContext(), "Longitude", UiKit.fmtCoord(state.longitude)),
                UiKit.row(getContext(), "Altitude", UiKit.fmt0(state.altitude, "m")),
                UiKit.row(getContext(), "Précision GPS", UiKit.fmt0(state.accuracy, "m")),
                UiKit.row(getContext(), "Adresse", "—"));

        replaceRows(gaugesCard, 1,
                UiKit.row(getContext(), "Régime moteur", "—"),
                UiKit.row(getContext(), "Température liquide", "—"),
                UiKit.row(getContext(), "Tension haute tension", "—"),
                UiKit.row(getContext(), "Tension cellule min/max", "—"),
                UiKit.row(getContext(), "Consommation instantanée", "—"),
                UiKit.row(getContext(), "Énergie récupérée au freinage", "—"));

        replaceRows(detailsCard, 1,
                UiKit.row(getContext(), "Portes", "—"),
                UiKit.row(getContext(), "Climatisation", "—"),
                UiKit.row(getContext(), "Réseau", UiKit.fmtText(state.networkType)),
                UiKit.row(getContext(), "Dernière mise à jour", UiKit.fmtTimestamp(state.lastUpdateAt)));
    }

    private void replaceRows(LinearLayout card, int keepFirstN, android.view.View... rows) {
        while (card.getChildCount() > keepFirstN) {
            card.removeViewAt(keepFirstN);
        }
        for (android.view.View row : rows) {
            card.addView(row);
        }
    }
}
