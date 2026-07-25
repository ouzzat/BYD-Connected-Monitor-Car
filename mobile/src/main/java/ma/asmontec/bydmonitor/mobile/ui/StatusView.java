package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import ma.asmontec.bydmonitor.mobile.data.VehicleState;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;

/** État temps réel : vitesse, puissance, couple, températures, tension — "—" tant que non reçu. */
public final class StatusView extends ScrollView implements VehicleStateRepository.Listener {

    private final VehicleStateRepository repository;
    private final LinearLayout card;

    public StatusView(Context context) {
        super(context);
        this.repository = VehicleStateRepository.getInstance(context);
        setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout root = UiKit.screen(context);
        root.addView(UiKit.sectionTitle(context, "État en temps réel"));

        card = UiKit.card(context);
        root.addView(card);
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
        card.removeAllViews();
        card.addView(UiKit.row(getContext(), "Vitesse", UiKit.fmt1(state.speedKmh, "km/h")));
        card.addView(UiKit.row(getContext(), "Régime moteur", "—"));
        card.addView(UiKit.row(getContext(), "Puissance électrique", UiKit.fmt1(state.powerKw, "kW")));
        card.addView(UiKit.row(getContext(), "Couple", UiKit.fmt1(state.torqueNm, "N·m")));
        card.addView(UiKit.row(getContext(), "Température liquide", "—"));
        card.addView(UiKit.row(getContext(), "Température batterie", UiKit.fmt1(state.batteryTemp, "°C")));
        card.addView(UiKit.row(getContext(), "Tension haute tension", "—"));
        card.addView(UiKit.row(getContext(), "Tension cellule min/max", "—"));
        card.addView(UiKit.row(getContext(), "État de charge (SOC)", state.soc == null ? "—" : UiKit.fmt0(state.soc, "%")));
        card.addView(UiKit.row(getContext(), "Consommation instantanée", "—"));
        card.addView(UiKit.row(getContext(), "Énergie récupérée au freinage", "—"));
    }
}
