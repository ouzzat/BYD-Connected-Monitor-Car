package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ma.asmontec.bydmonitor.mobile.security.SecureCredentialStore;

public final class MoreView extends ScrollView {

    public MoreView(Context context) {
        super(context);
        setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout root = UiKit.screen(context);
        root.addView(UiKit.sectionTitle(context, "Plus"));

        LinearLayout menuCard = UiKit.card(context);
        addMenuRow(menuCard, "Paramètres de connexion", () -> context.startActivity(new Intent(context, SettingsActivity.class)));
        addMenuRow(menuCard, "Notifications", () -> context.startActivity(new Intent(context, NotificationsActivity.class)));
        addMenuRow(menuCard, "Historique des trajets", () -> context.startActivity(new Intent(context, TripHistoryActivity.class)));
        addMenuRow(menuCard, "Événements", () -> context.startActivity(new Intent(context, EventsActivity.class)));
        addMenuRow(menuCard, "Commandes distantes", () -> context.startActivity(new Intent(context, RemoteCommandsActivity.class)));
        addMenuRow(menuCard, "À propos", () -> context.startActivity(new Intent(context, AboutActivity.class)));
        addMenuRow(menuCard, "Déconnexion", () -> {
            new SecureCredentialStore(context).clear();
            android.widget.Toast.makeText(context, "Identifiants supprimés", android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(menuCard);

        LinearLayout infoCard = UiKit.card(context);
        infoCard.addView(UiKit.row(context, "Profil du véhicule", "BYD SEAL U DM-i"));
        infoCard.addView(UiKit.row(context, "Unités", "km/h · °C · km (fixes)"));
        infoCard.addView(UiKit.row(context, "Thème", "Sombre (fixe)"));
        infoCard.addView(UiKit.row(context, "Sécurité", "TLS obligatoire · Keystore AES-GCM"));
        root.addView(infoCard);

        addView(root);
    }

    private interface Action {
        void run();
    }

    private void addMenuRow(LinearLayout card, String label, Action action) {
        TextView row = new TextView(getContext());
        row.setText(label);
        row.setTextColor(UiKit.TEXT_PRIMARY);
        row.setTextSize(15f);
        row.setPadding(0, UiKit.dp(getContext(), 12), 0, UiKit.dp(getContext(), 12));
        row.setOnClickListener(v -> action.run());
        card.addView(row);
    }
}
