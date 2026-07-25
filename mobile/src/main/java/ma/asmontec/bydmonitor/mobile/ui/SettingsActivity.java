package ma.asmontec.bydmonitor.mobile.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import ma.asmontec.bydmonitor.mobile.mqtt.MqttConfig;
import ma.asmontec.bydmonitor.mobile.security.SecureCredentialStore;
import ma.asmontec.bydmonitor.mobile.service.MqttSubscriberService;

/**
 * Paramètres de connexion. Le serveur, le port et le préfixe sont
 * verrouillés (affichage seul). Le compte (utilisateur/mot de passe,
 * limité à l'abonnement) peut être saisi manuellement une fois ou importé
 * via un code d'activation encodé en base64. Un scan par QR code n'est
 * pas implémenté dans cette version (aucune bibliothèque de lecture de
 * code-barres n'a été ajoutée pour limiter la surface de dépendances).
 */
public final class SettingsActivity extends Activity {

    private SecureCredentialStore credentialStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        credentialStore = new SecureCredentialStore(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiKit.BACKGROUND);
        LinearLayout root = UiKit.screen(this);

        root.addView(UiKit.sectionTitle(this, "Paramètres de connexion"));
        TextView locked = new TextView(this);
        locked.setText("Serveur : " + MqttConfig.BROKER_HOST + ":" + MqttConfig.BROKER_PORT
                + "\nPréfixe : " + MqttConfig.TOPIC_PREFIX + "\n(paramètres verrouillés)");
        locked.setTextColor(UiKit.TEXT_MUTED);
        locked.setTextSize(13f);
        locked.setPadding(0, 0, 0, UiKit.dp(this, 20));
        root.addView(locked);

        LinearLayout manualCard = UiKit.card(this);
        manualCard.addView(UiKit.sectionTitle(this, "Saisie manuelle"));

        EditText username = new EditText(this);
        username.setHint("Utilisateur MQTT (abonnement)");
        String existing = credentialStore.getUsername();
        if (existing != null) {
            username.setText(existing);
        }
        manualCard.addView(username);

        EditText password = new EditText(this);
        password.setHint("Mot de passe MQTT");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        manualCard.addView(password);

        Button save = new Button(this);
        save.setText("Enregistrer");
        save.setOnClickListener(v -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString();
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Utilisateur et mot de passe requis", Toast.LENGTH_SHORT).show();
                return;
            }
            credentialStore.save(user, pass.toCharArray());
            MqttSubscriberService.start(this);
            Toast.makeText(this, "Identifiants enregistrés et chiffrés", Toast.LENGTH_SHORT).show();
            finish();
        });
        manualCard.addView(save);
        root.addView(manualCard);

        LinearLayout activationCard = UiKit.card(this);
        activationCard.addView(UiKit.sectionTitle(this, "Code d'activation"));
        activationCard.addView(UiKit.subtle(this,
                "Collez le code fourni par l'installateur (identifiants encodés). La lecture par QR code n'est pas encore implémentée."));

        EditText code = new EditText(this);
        code.setHint("Code d'activation");
        activationCard.addView(code);

        Button apply = new Button(this);
        apply.setText("Appliquer le code");
        apply.setOnClickListener(v -> {
            try {
                credentialStore.importFromActivationCode(code.getText().toString());
                MqttSubscriberService.start(this);
                Toast.makeText(this, "Compte importé et chiffré", Toast.LENGTH_SHORT).show();
                finish();
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Code d'activation invalide", Toast.LENGTH_SHORT).show();
            }
        });
        activationCard.addView(apply);
        root.addView(activationCard);

        scroll.addView(root);
        setContentView(scroll);
    }
}
