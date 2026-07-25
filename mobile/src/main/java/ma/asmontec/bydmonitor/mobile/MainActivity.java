package ma.asmontec.bydmonitor.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import ma.asmontec.bydmonitor.mobile.security.SecureCredentialStore;
import ma.asmontec.bydmonitor.mobile.service.MqttSubscriberService;
import ma.asmontec.bydmonitor.mobile.ui.AboutActivity;
import ma.asmontec.bydmonitor.mobile.ui.CameraDirectView;
import ma.asmontec.bydmonitor.mobile.ui.DashboardView;
import ma.asmontec.bydmonitor.mobile.ui.LogsView;
import ma.asmontec.bydmonitor.mobile.ui.SeatsView;
import ma.asmontec.bydmonitor.mobile.ui.SettingsActivity;
import ma.asmontec.bydmonitor.mobile.ui.TripsView;
import ma.asmontec.bydmonitor.mobile.ui.UiKit;

/**
 * Coquille de navigation : barre supérieure + menu latéral (aucune
 * dépendance AndroidX/DrawerLayout, panneau coulissant fait à la main)
 * donnant accès aux cinq écrans principaux ainsi qu'aux réglages.
 */
public final class MainActivity extends Activity {

    private static final int REQUEST_PERMISSIONS = 200;
    private static final String[] SCREEN_TITLES = {
            "Tableau de bord", "Trajets", "Caméra directe", "Sièges", "Journal"
    };

    private FrameLayout content;
    private TextView topBarTitle;
    private LinearLayout drawerPanel;
    private View scrim;
    private final TextView[] menuItems = new TextView[SCREEN_TITLES.length];
    private final View[] menuIndicators = new View[SCREEN_TITLES.length];
    private int drawerWidthPx;
    private boolean drawerOpen;
    private int currentScreen = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(UiKit.BACKGROUND);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(buildTopBar());

        content = new FrameLayout(this);
        column.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(column, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        scrim = new View(this);
        scrim.setBackgroundColor(Color.argb(140, 0, 0, 0));
        scrim.setAlpha(0f);
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(v -> closeDrawer());
        root.addView(scrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        drawerWidthPx = (int) (UiKit.dp(this, 100) * 2.72f); // ~272dp
        drawerPanel = buildDrawer();
        root.addView(drawerPanel, new FrameLayout.LayoutParams(drawerWidthPx, FrameLayout.LayoutParams.MATCH_PARENT));

        drawerPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                drawerPanel.setTranslationX(-drawerWidthPx);
                drawerPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        setContentView(root);

        requestRequiredPermissions();
        selectScreen(0);
        ensureConnectionConfigured();
    }

    private LinearLayout buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(UiKit.SURFACE);
        bar.setPadding(UiKit.dp(this, 8), UiKit.dp(this, 14), UiKit.dp(this, 16), UiKit.dp(this, 14));

        TextView hamburger = new TextView(this);
        hamburger.setText("≡");
        hamburger.setTextColor(UiKit.ACCENT);
        hamburger.setTextSize(24f);
        hamburger.setPadding(UiKit.dp(this, 12), 0, UiKit.dp(this, 16), 0);
        hamburger.setOnClickListener(v -> openDrawer());
        bar.addView(hamburger);

        topBarTitle = new TextView(this);
        topBarTitle.setTextColor(UiKit.TEXT_PRIMARY);
        topBarTitle.setTextSize(18f);
        topBarTitle.setTypeface(android.graphics.Typeface.create(UiKit.FONT_FAMILY_MEDIUM, android.graphics.Typeface.NORMAL));
        bar.addView(topBarTitle);

        return bar;
    }

    private LinearLayout buildDrawer() {
        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(UiKit.SURFACE);
        drawer.setPadding(0, UiKit.dp(this, 32), 0, UiKit.dp(this, 24));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawer.setElevation(UiKit.dp(this, 12));
        }

        TextView brand = new TextView(this);
        brand.setText("BYD SEAL U DM-i");
        brand.setTextColor(UiKit.ACCENT);
        brand.setTextSize(16f);
        brand.setTypeface(android.graphics.Typeface.create(UiKit.FONT_FAMILY_MEDIUM, android.graphics.Typeface.NORMAL));
        brand.setPadding(UiKit.dp(this, 20), 0, UiKit.dp(this, 20), UiKit.dp(this, 24));
        drawer.addView(brand);

        for (int i = 0; i < SCREEN_TITLES.length; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, UiKit.dp(this, 14), UiKit.dp(this, 20), UiKit.dp(this, 14));
            row.setOnClickListener(v -> selectScreen(index));

            View indicator = new View(this);
            LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(UiKit.dp(this, 4), UiKit.dp(this, 22));
            indicatorParams.setMargins(0, 0, UiKit.dp(this, 16), 0);
            indicator.setLayoutParams(indicatorParams);
            menuIndicators[i] = indicator;
            row.addView(indicator);

            TextView label = new TextView(this);
            label.setText(SCREEN_TITLES[i]);
            label.setTextSize(15f);
            row.addView(label);
            menuItems[i] = label;

            drawer.addView(row);
        }

        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 1));
        dividerParams.setMargins(UiKit.dp(this, 20), UiKit.dp(this, 16), UiKit.dp(this, 20), UiKit.dp(this, 8));
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(UiKit.DIVIDER);
        drawer.addView(divider);

        drawer.addView(drawerAction("Paramètres de connexion", () -> {
            closeDrawer();
            startActivity(new Intent(this, SettingsActivity.class));
        }));
        drawer.addView(drawerAction("À propos", () -> {
            closeDrawer();
            startActivity(new Intent(this, AboutActivity.class));
        }));
        drawer.addView(drawerAction("Déconnexion", () -> {
            closeDrawer();
            new SecureCredentialStore(this).clear();
            Toast.makeText(this, "Identifiants supprimés", Toast.LENGTH_SHORT).show();
        }));

        return drawer;
    }

    private interface Action {
        void run();
    }

    private TextView drawerAction(String label, Action action) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextColor(UiKit.TEXT_MUTED);
        view.setTextSize(14f);
        view.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 10), UiKit.dp(this, 20), UiKit.dp(this, 10));
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private void selectScreen(int index) {
        currentScreen = index;
        topBarTitle.setText(SCREEN_TITLES[index]);
        for (int i = 0; i < menuItems.length; i++) {
            boolean active = i == index;
            menuItems[i].setTextColor(active ? UiKit.ACCENT : UiKit.TEXT_MUTED);
            menuIndicators[i].setBackgroundColor(active ? UiKit.ACCENT : Color.TRANSPARENT);
        }

        content.removeAllViews();
        switch (index) {
            case 0:
                content.addView(new DashboardView(this, () -> selectScreen(1)));
                break;
            case 1:
                content.addView(new TripsView(this));
                break;
            case 2:
                content.addView(new CameraDirectView(this));
                break;
            case 3:
                content.addView(new SeatsView(this));
                break;
            case 4:
            default:
                content.addView(new LogsView(this));
                break;
        }
        closeDrawer();
    }

    private void openDrawer() {
        drawerOpen = true;
        scrim.setVisibility(View.VISIBLE);
        scrim.animate().alpha(1f).setDuration(200).start();
        drawerPanel.animate().translationX(0).setDuration(220).start();
    }

    private void closeDrawer() {
        if (!drawerOpen) {
            return;
        }
        drawerOpen = false;
        scrim.animate().alpha(0f).setDuration(200).withEndAction(() -> scrim.setVisibility(View.GONE)).start();
        drawerPanel.animate().translationX(-drawerWidthPx).setDuration(220).start();
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            closeDrawer();
            return;
        }
        super.onBackPressed();
    }

    private void ensureConnectionConfigured() {
        SecureCredentialStore store = new SecureCredentialStore(this);
        if (store.hasCredentials()) {
            MqttSubscriberService.start(this);
        } else {
            startActivity(new Intent(this, SettingsActivity.class));
        }
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_PERMISSIONS);
        }
    }
}
