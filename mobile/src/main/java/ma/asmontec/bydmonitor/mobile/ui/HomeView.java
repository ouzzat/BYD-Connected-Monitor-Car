package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ma.asmontec.bydmonitor.mobile.data.VehicleState;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;

public final class HomeView extends ScrollView implements VehicleStateRepository.Listener {

    private final VehicleStateRepository repository;
    private final TextView connectionBadge;
    private final TextView socValue;
    private final TextView rangeValue;
    private final LinearLayout detailsCard;

    public HomeView(Context context) {
        super(context);
        this.repository = VehicleStateRepository.getInstance(context);
        setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout root = UiKit.screen(context);

        TextView vehicleName = new TextView(context);
        vehicleName.setText("BYD SEAL U DM-i");
        vehicleName.setTextColor(Color.WHITE);
        vehicleName.setTextSize(24f);
        root.addView(vehicleName);

        connectionBadge = new TextView(context);
        connectionBadge.setTextSize(14f);
        connectionBadge.setPadding(0, UiKit.dp(context, 4), 0, UiKit.dp(context, 16));
        root.addView(connectionBadge);

        LinearLayout heroCard = UiKit.card(context);
        LinearLayout heroRow = new LinearLayout(context);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout socBlock = new LinearLayout(context);
        socBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

        TextView socLabel = UiKit.subtle(context, "Batterie");
        socValue = new TextView(context);
        socValue.setTextColor(Color.WHITE);
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

        // Reconstruit les lignes de détail (nombre fixe et léger : pas de recyclage nécessaire).
        while (detailsCard.getChildCount() > 1) {
            detailsCard.removeViewAt(1);
        }
        detailsCard.addView(UiKit.row(getContext(), "Autonomie totale", "—"));
        detailsCard.addView(UiKit.row(getContext(), "Vitesse", UiKit.fmt1(state.speedKmh, "km/h")));
        detailsCard.addView(UiKit.row(getContext(), "Puissance", UiKit.fmt1(state.powerKw, "kW")));
        detailsCard.addView(UiKit.row(getContext(), "Température batterie", UiKit.fmt1(state.batteryTemp, "°C")));
        detailsCard.addView(UiKit.row(getContext(), "Batterie unité Android", UiKit.fmtInt(state.androidBattery, "%")));
        detailsCard.addView(UiKit.row(getContext(), "Portes", "—"));
        detailsCard.addView(UiKit.row(getContext(), "Climatisation", "—"));
        detailsCard.addView(UiKit.row(getContext(), "Réseau", UiKit.fmtText(state.networkType)));
        detailsCard.addView(UiKit.row(getContext(), "Dernière mise à jour", UiKit.fmtTimestamp(state.lastUpdateAt)));
    }
}
