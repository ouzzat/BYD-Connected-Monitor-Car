package ma.asmontec.bydmonitor.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureConfig {
    static final String DEFAULT_HOST = "f110c3c7fb39470e865d51c9530c8e9c.s1.eu.hivemq.cloud";
    static final int DEFAULT_PORT = 8883;
    static final String DEFAULT_TOPIC = "asmontec/byd/seal-u";
    private static final String ALIAS = "byd_mobile_mqtt_key";
    private static final String PREF = "mqtt_secure_config";

    private SecureConfig() {}

    static void save(Context c, String user, String password, String topic) throws Exception {
        SharedPreferences.Editor e = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        e.putString("user", user == null ? "" : user.trim());
        e.putString("topic", normalizeTopic(topic));
        e.putString("password", encrypt(password == null ? "" : password));
        e.putBoolean("enabled", true);
        e.apply();
    }

    static boolean isEnabled(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("enabled", false);
    }

    static String user(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("user", "");
    }

    static String topic(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("topic", DEFAULT_TOPIC);
    }

    static String password(Context c) throws Exception {
        String v = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("password", "");
        return v.isEmpty() ? "" : decrypt(v);
    }

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            kg.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
    }

    private static String encrypt(String value) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key());
        byte[] enc = c.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(c.getIV(), Base64.NO_WRAP) + "." + Base64.encodeToString(enc, Base64.NO_WRAP);
    }

    private static String decrypt(String value) throws Exception {
        String[] p = value.split("\\.", 2);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(p[0], Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(p[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }

    static String normalizeTopic(String value) {
        String t = value == null ? "" : value.trim();
        while (t.startsWith("/")) t = t.substring(1);
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t.isEmpty() ? DEFAULT_TOPIC : t;
    }
}
