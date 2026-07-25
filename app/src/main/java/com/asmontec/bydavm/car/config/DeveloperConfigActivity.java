package com.asmontec.bydavm.car.config;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.asmontec.bydavm.car.mqtt.MqttConfig;
import com.asmontec.bydavm.car.security.PinGuard;
import com.asmontec.bydavm.car.security.SecureCredentialStore;
import java.util.concurrent.TimeUnit;

/**
 * Écran protégé par un code local. Permet de créer ce code au premier
 * lancement puis, une fois déverrouillé, de saisir le compte MQTT
 * (utilisateur / mot de passe) chiffré ensuite via Android Keystore.
 * Le serveur, le port et le préfixe de topics sont verrouillés et
 * affichés en lecture seule.
 */
public final class DeveloperConfigActivity extends Activity {

    private PinGuard pinGuard;
    private SecureCredentialStore credentialStore;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        pinGuard = new PinGuard(this);
        credentialStore = new SecureCredentialStore(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(11, 17, 24));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 64);
        scroll.addView(root);
        setContentView(scroll);

        render();
    }

    private void render() {
        root.removeAllViews();
        if (!pinGuard.isConfigured()) {
            renderCreatePin();
        } else if (pinGuard.isLocked()) {
            renderLocked();
        } else {
            renderPinEntry();
        }
    }

    private void renderCreatePin() {
        addTitle("Créer un code de configuration");
        addBody("Choisissez un code local de 6 à 10 chiffres. Aucun code par défaut n'est intégré à l'application.");

        EditText pin = newPinField("Nouveau code");
        EditText confirm = newPinField("Confirmer le code");
        root.addView(pin);
        root.addView(confirm);

        Button create = new Button(this);
        create.setText("Créer le code");
        create.setOnClickListener(v -> {
            String p1 = pin.getText().toString();
            String p2 = confirm.getText().toString();
            if (p1.length() < 6 || p1.length() > 10 || !p1.matches("\\d+")) {
                toast("Le code doit contenir de 6 à 10 chiffres");
                return;
            }
            if (!p1.equals(p2)) {
                toast("Les deux codes ne correspondent pas");
                return;
            }
            pinGuard.createPin(p1);
            toast("Code créé");
            render();
        });
        root.addView(create);
    }

    private void renderLocked() {
        addTitle("Configuration verrouillée");
        long remainingMs = pinGuard.lockedUntilMillis() - System.currentTimeMillis();
        long remainingMin = Math.max(1, TimeUnit.MILLISECONDS.toMinutes(remainingMs) + 1);
        addBody("Trop de tentatives incorrectes. Réessayez dans environ " + remainingMin + " minute(s).");
    }

    private void renderPinEntry() {
        addTitle("Configuration développeur");
        addBody("Saisissez votre code local pour accéder aux paramètres du compte MQTT.");

        EditText pin = newPinField("Code");
        root.addView(pin);

        Button unlock = new Button(this);
        unlock.setText("Déverrouiller");
        unlock.setOnClickListener(v -> {
            if (pinGuard.verify(pin.getText().toString())) {
                renderSettingsForm();
            } else {
                toast(pinGuard.isLocked() ? "Verrouillé après trop d'essais" : "Code incorrect");
                render();
            }
        });
        root.addView(unlock);
    }

    private void renderSettingsForm() {
        root.removeAllViews();
        addTitle("Compte MQTT (publication uniquement)");
        addBody("Serveur : " + MqttConfig.BROKER_HOST + ":" + MqttConfig.BROKER_PORT
                + "\nPréfixe : " + MqttConfig.TOPIC_PREFIX
                + "\n(paramètres verrouillés, non modifiables)");

        EditText username = new EditText(this);
        username.setHint("Utilisateur MQTT");
        username.setTextColor(Color.WHITE);
        username.setHintTextColor(Color.GRAY);
        String existingUser = credentialStore.getUsername();
        if (existingUser != null) {
            username.setText(existingUser);
        }
        root.addView(username);

        EditText password = new EditText(this);
        password.setHint("Mot de passe MQTT");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setTextColor(Color.WHITE);
        password.setHintTextColor(Color.GRAY);
        root.addView(password);

        TextView note = new TextView(this);
        note.setText(credentialStore.hasCredentials()
                ? "Un compte est déjà enregistré. Laissez le mot de passe vide pour le conserver."
                : "Aucun compte enregistré pour le moment.");
        note.setTextColor(Color.rgb(150, 150, 150));
        note.setPadding(0, 8, 0, 24);
        root.addView(note);

        Button save = new Button(this);
        save.setText("Enregistrer");
        save.setOnClickListener(v -> {
            String userValue = username.getText().toString().trim();
            String passValue = password.getText().toString();
            if (userValue.isEmpty()) {
                toast("L'utilisateur MQTT est requis");
                return;
            }
            char[] toStore;
            if (passValue.isEmpty() && credentialStore.hasCredentials()) {
                toStore = credentialStore.getPassword();
            } else if (!passValue.isEmpty()) {
                toStore = passValue.toCharArray();
            } else {
                toast("Le mot de passe MQTT est requis");
                return;
            }
            credentialStore.save(userValue, toStore);
            toast("Identifiants enregistrés et chiffrés");
            finish();
        });
        root.addView(save);
    }

    private EditText newPinField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.GRAY);
        return field;
    }

    private void addTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(Color.WHITE);
        title.setTextSize(22f);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);
    }

    private void addBody(String text) {
        TextView body = new TextView(this);
        body.setText(text);
        body.setTextColor(Color.rgb(180, 180, 180));
        body.setTextSize(15f);
        body.setPadding(0, 0, 0, 24);
        root.addView(body);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
