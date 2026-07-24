package com.asmontec.bydavm.car.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Protège l'écran "Configuration développeur" par un code local (6 à 10 chiffres)
 * créé au premier lancement. Le code n'est jamais stocké en clair : seul un hachage
 * PBKDF2 salé est conservé. Verrouillage temporaire après cinq tentatives erronées.
 */
public final class PinGuard {

    private static final String PREFS_NAME = "byd_car_agent_pin_prefs";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 5 * 60 * 1000L;

    private final SharedPreferences prefs;

    public PinGuard(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isConfigured() {
        return prefs.contains("pin_hash");
    }

    public void createPin(String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String hash = hash(pin, salt);
        prefs.edit()
                .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("pin_hash", hash)
                .putInt("failed_attempts", 0)
                .putLong("locked_until", 0L)
                .apply();
    }

    public long lockedUntilMillis() {
        return prefs.getLong("locked_until", 0L);
    }

    public boolean isLocked() {
        return System.currentTimeMillis() < lockedUntilMillis();
    }

    /** @return true si le code est correct. */
    public boolean verify(String pin) {
        if (isLocked()) {
            return false;
        }
        String salt = prefs.getString("pin_salt", null);
        String expectedHash = prefs.getString("pin_hash", null);
        if (salt == null || expectedHash == null) {
            return false;
        }
        String actualHash = hash(pin, Base64.decode(salt, Base64.NO_WRAP));
        boolean ok = constantTimeEquals(actualHash, expectedHash);
        if (ok) {
            prefs.edit().putInt("failed_attempts", 0).putLong("locked_until", 0L).apply();
        } else {
            int attempts = prefs.getInt("failed_attempts", 0) + 1;
            SharedPreferences.Editor editor = prefs.edit().putInt("failed_attempts", attempts);
            if (attempts >= MAX_ATTEMPTS) {
                editor.putLong("locked_until", System.currentTimeMillis() + LOCKOUT_MS);
                editor.putInt("failed_attempts", 0);
            }
            editor.apply();
        }
        return ok;
    }

    private static String hash(String pin, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 indisponible", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
