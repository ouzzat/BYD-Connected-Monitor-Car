package ma.asmontec.bydmonitor.mobile.notify;

import android.content.Context;
import android.content.SharedPreferences;

/** Préférences d'activation par type de notification (toutes activées par défaut). */
public final class NotificationPrefs {

    public enum Kind {
        DOOR,
        TRIP_START_END,
        LOW_BATTERY,
        CHARGE_COMPLETE,
        VEHICLE_OFFLINE,
        VEHICLE_MOVING,
        GPS_LOST,
        SERVICE_ERROR
    }

    private static final String PREFS_NAME = "byd_mobile_notification_prefs";
    private final SharedPreferences prefs;

    public NotificationPrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled(Kind kind) {
        return prefs.getBoolean(kind.name(), true);
    }

    public void setEnabled(Kind kind, boolean enabled) {
        prefs.edit().putBoolean(kind.name(), enabled).apply();
    }
}
