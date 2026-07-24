package ma.asmontec.bydmonitor.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

public final class MainActivity extends Activity {
    private WebView webView;
    private volatile String lastTopic = "";
    private volatile String lastPayload = "";
    private volatile String lastStatus = "Configuration requise";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra(MqttBackgroundService.EXTRA_STATUS);
            String topic = intent.getStringExtra(MqttBackgroundService.EXTRA_TOPIC);
            String payload = intent.getStringExtra(MqttBackgroundService.EXTRA_PAYLOAD);
            if (status != null) lastStatus = status;
            if (topic != null) lastTopic = topic;
            if (payload != null) lastPayload = payload;
            pushToWeb(topic, payload, status);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        webView = new WebView(this);
        configureWebView();
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        if (SecureConfig.isEnabled(this)) startMqttService();
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(MqttBackgroundService.ACTION_DATA);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, filter);
    }

    @Override protected void onStop() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        super.onStop();
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
        }
        super.onDestroy();
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setDomStorageEnabled(false);
        s.setDatabaseEnabled(false);
        s.setGeolocationEnabled(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !url.startsWith("file:///android_asset/");
            }
        });
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
    }

    private void startMqttService() {
        Intent i = new Intent(this, MqttBackgroundService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void pushToWeb(String topic, String payload, String status) {
        if (webView == null) return;
        String js = "window.onNativeUpdate(" + quote(topic) + "," + quote(payload) + "," + quote(status) + ")";
        webView.post(() -> webView.evaluateJavascript(js, null));
    }

    private static String quote(String value) {
        return JSONObject.quote(value == null ? "" : value);
    }

    public final class Bridge {
        @JavascriptInterface public boolean saveConfig(String user, String password, String topic) {
            try {
                SecureConfig.save(MainActivity.this, user, password, topic);
                lastStatus = "Connexion automatique active";
                startMqttService();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface public void stopConnection() {
            SecureConfig.disable(MainActivity.this);
            stopService(new Intent(MainActivity.this, MqttBackgroundService.class));
            lastStatus = "Déconnecté";
        }

        @JavascriptInterface public String getSnapshot() {
            try {
                JSONObject j = new JSONObject();
                j.put("user", SecureConfig.user(MainActivity.this));
                j.put("topic", SecureConfig.topic(MainActivity.this));
                j.put("status", lastStatus);
                j.put("topicReceived", lastTopic);
                j.put("payload", lastPayload);
                return j.toString();
            } catch (Exception e) {
                return "{}";
            }
        }
    }
}
