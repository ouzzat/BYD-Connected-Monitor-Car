package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import ma.asmontec.bydmonitor.mobile.data.VehicleState;
import ma.asmontec.bydmonitor.mobile.data.VehicleStateRepository;

/**
 * Aucune clé Google Maps / Mapbox n'étant fournie, cet écran affiche une
 * vue hors ligne claire avec les coordonnées plutôt qu'une fausse carte.
 */
public final class MapView extends ScrollView implements VehicleStateRepository.Listener {

    private final VehicleStateRepository repository;
    private final LinearLayout card;

    public MapView(Context context) {
        super(context);
        this.repository = VehicleStateRepository.getInstance(context);
        setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout root = UiKit.screen(context);
        root.addView(UiKit.sectionTitle(context, "Carte et localisation"));
        root.addView(UiKit.subtle(context,
                "Aucune clé Google Maps / Mapbox configurée : affichage des coordonnées uniquement."));

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
        card.addView(UiKit.row(getContext(), "Latitude", UiKit.fmtCoord(state.latitude)));
        card.addView(UiKit.row(getContext(), "Longitude", UiKit.fmtCoord(state.longitude)));
        card.addView(UiKit.row(getContext(), "Altitude", UiKit.fmt0(state.altitude, "m")));
        card.addView(UiKit.row(getContext(), "Précision GPS", UiKit.fmt0(state.accuracy, "m")));
        card.addView(UiKit.row(getContext(), "Vitesse", UiKit.fmt1(state.speedKmh, "km/h")));
        card.addView(UiKit.row(getContext(), "Adresse", "—"));
        card.addView(UiKit.row(getContext(), "Dernière mise à jour", UiKit.fmtTimestamp(state.lastUpdateAt)));
    }
}
