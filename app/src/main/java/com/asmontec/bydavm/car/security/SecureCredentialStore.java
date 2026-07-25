package com.asmontec.bydavm.car.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stocke les identifiants MQTT (broker, port, utilisateur, mot de passe, préfixe)
 * chiffrés avec une clé matérielle Android Keystore (AES-256-GCM). Le texte en
 * clair ne quitte jamais la mémoire du processus ; SharedPreferences ne contient
 * que du texte chiffré encodé en base64.
 */
public final class SecureCredentialStore {

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "byd_car_agent_mqtt_key_v1";
    private static final String PREFS_NAME = "byd_car_agent_secure_prefs";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final SharedPreferences prefs;

    public SecureCredentialStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasCredentials() {
        return prefs.contains("username_enc") && prefs.contains("password_enc");
    }

    /**
     * Enregistre le compte MQTT (limité à la publication côté agent voiture).
     * Le serveur, le port et le préfixe de topics sont verrouillés dans
     * {@link com.asmontec.bydavm.car.mqtt.MqttConfig} et ne sont pas stockés ici.
     */
    public void save(String username, char[] password) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username_enc", encrypt(username));
        editor.putString("password_enc", encrypt(new String(password)));
        editor.apply();
    }

    public String getUsername() {
        return decrypt(prefs.getString("username_enc", null));
    }

    public char[] getPassword() {
        String value = decrypt(prefs.getString("password_enc", null));
        return value == null ? new char[0] : value.toCharArray();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    // --- Chiffrement AES-GCM avec clé conservée exclusivement dans Android Keystore ---

    private String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[GCM_IV_BYTES];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Échec du chiffrement des identifiants", e);
        }
    }

    private String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);
            if (combined.length < GCM_IV_BYTES) {
                return null;
            }
            byte[] iv = new byte[GCM_IV_BYTES];
            byte[] cipherText = new byte[combined.length - GCM_IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_BYTES);
            System.arraycopy(combined, GCM_IV_BYTES, cipherText, 0, cipherText.length);

            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            return entry.getSecretKey();
        }

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }
}
