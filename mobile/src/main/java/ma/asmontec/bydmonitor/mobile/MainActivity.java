package ma.asmontec.bydmonitor.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import ma.asmontec.bydmonitor.mobile.security.SecureCredentialStore;
import ma.asmontec.bydmonitor.mobile.service.MqttSubscriberService;
import ma.asmontec.bydmonitor.mobile.ui.CamerasView;
import ma.asmontec.bydmonitor.mobile.ui.HomeView;
import ma.asmontec.bydmonitor.mobile.ui.MapView;
import ma.asmontec.bydmonitor.mobile.ui.MoreView;
import ma.asmontec.bydmonitor.mobile.ui.SettingsActivity;
import ma.asmontec.bydmonitor.mobile.ui.StatusView;
import ma.asmontec.bydmonitor.mobile.ui.UiKit;

public final class MainActivity extends Activity {

    private static final int REQUEST_PERMISSIONS = 200;
    private static final String[] TAB_LABELS = {"Accueil", "État", "Carte", "Caméras", "Plus"};

    private FrameLayout content;
    private final TextView[] tabViews = new TextView[TAB_LABELS.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiKit.BACKGROUND);

        content = new FrameLayout(this);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(content, contentParams);

        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setBackgroundColor(UiKit.CARD);
        bottomNav.setPadding(0, UiKit.dp(this, 10), 0, UiKit.dp(this, 10));

        for (int i = 0; i < TAB_LABELS.length; i++) {
            final int index = i;
            TextView tab = new TextView(this);
            tab.setText(TAB_LABELS[i]);
            tab.setTextColor(UiKit.TEXT_MUTED);
            tab.setTextSize(13f);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setOnClickListener(v -> selectTab(index));
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            bottomNav.addView(tab, tabParams);
            tabViews[i] = tab;
        }
        root.addView(bottomNav);
        setContentView(root);

        requestRequiredPermissions();
        selectTab(0);
        ensureConnectionConfigured();
    }

    private void selectTab(int index) {
        for (int i = 0; i < tabViews.length; i++) {
            tabViews[i].setTextColor(i == index ? UiKit.ACCENT : UiKit.TEXT_MUTED);
        }
        content.removeAllViews();
        switch (index) {
            case 0:
                content.addView(new HomeView(this));
                break;
            case 1:
                content.addView(new StatusView(this));
                break;
            case 2:
                content.addView(new MapView(this));
                break;
            case 3:
                content.addView(new CamerasView(this));
                break;
            case 4:
            default:
                content.addView(new MoreView(this));
                break;
        }
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
