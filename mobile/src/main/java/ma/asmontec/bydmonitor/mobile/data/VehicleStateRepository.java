package ma.asmontec.bydmonitor.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import ma.asmontec.bydmonitor.mobile.db.AppDatabaseHelper;
import ma.asmontec.bydmonitor.mobile.mqtt.MqttConfig;
import ma.asmontec.bydmonitor.mobile.notify.NotificationHelper;
import ma.asmontec.bydmonitor.mobile.notify.NotificationPrefs;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Point central de vérité pour l'état véhicule côté mobile : reçoit les
 * messages MQTT, met à jour {@link VehicleState}, journalise les
 * événements, détecte le début/la fin de trajet et déclenche les
 * notifications. Conserve un dernier instantané hors-ligne dans
 * SharedPreferences pour un affichage immédiat au démarrage sans réseau.
 */
public final class VehicleStateRepository {

    public interface Listener {
        void onStateChanged(VehicleState state);
    }

    private static final String PREFS_NAME = "byd_mobile_last_state";
    private static final double TRIP_START_SPEED_KMH = 3.0;
    private static final double TRIP_STOP_SPEED_KMH = 1.0;
    private static final long TRIP_STOP_GRACE_MS = 60_000L;
    private static final int LOW_BATTERY_THRESHOLD = 15;

    private static volatile VehicleStateRepository instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final AppDatabaseHelper db;
    private final NotificationHelper notifications;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private final VehicleState state = new VehicleState();

    // Suivi du trajet en cours (aucune persistance de session : simple et robuste).
    private long activeTripId = -1L;
    private double tripDistanceKm;
    private double tripMaxSpeedKmh;
    private double tripSpeedSum;
    private int tripSpeedCount;
    private Double lastPointLat;
    private Double lastPointLon;
    private Long firstLowSpeedAt;
    private boolean lowBatteryAlerted;

    public static VehicleStateRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (VehicleStateRepository.class) {
                if (instance == null) {
                    instance = new VehicleStateRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private VehicleStateRepository(Context context) {
        this.appContext = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.db = new AppDatabaseHelper(context);
        this.notifications = new NotificationHelper(context);
        restorePersistedSnapshot();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.onStateChanged(state.copy());
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public VehicleState currentState() {
        return state.copy();
    }

    public AppDatabaseHelper database() {
        return db;
    }

    /** Appelé par le service d'abonnement pour chaque message reçu, quel que soit le thread. */
    public void onMqttMessage(String topic, byte[] payload) {
        try {
            String json = new String(payload, StandardCharsets.UTF_8);
            if (MqttConfig.TOPIC_STATUS.equals(topic)) {
                handleStatus(new JSONObject(json));
            } else if (MqttConfig.TOPIC_TELEMETRY_LIVE.equals(topic)) {
                handleTelemetry(new JSONObject(json));
            } else if (MqttConfig.TOPIC_LOCATION_LIVE.equals(topic)) {
                handleLocation(new JSONObject(json));
            } else if (MqttConfig.TOPIC_EVENTS.equals(topic)) {
                handleEvent(new JSONObject(json));
            } else if (MqttConfig.TOPIC_ANDROID_HEALTH.equals(topic)) {
                handleAndroidHealth(new JSONObject(json));
            } else if (MqttConfig.TOPIC_NETWORK_STATUS.equals(topic)) {
                handleNetworkStatus(new JSONObject(json));
            }
            // trips/current, trips/history et camera/status : réservés, non émis
            // par la version actuelle de l'agent voiture.
        } catch (JSONException e) {
            db.insertEvent("malformed_message", "Message MQTT illisible sur " + topic);
        }
        persistSnapshot();
        publish();
    }

    public void onMqttDisconnected() {
        state.online = false;
        db.insertEvent("mqtt_disconnected", "Connexion MQTT perdue côté application mobile");
        notifications.notify(NotificationPrefs.Kind.VEHICLE_OFFLINE, "MQTT déconnecté",
                "La connexion au broker a été interrompue.");
        persistSnapshot();
        publish();
    }

    private void handleStatus(JSONObject json) {
        boolean online = json.optBoolean("online", false);
        Boolean previous = state.online;
        state.online = online;
        state.lastUpdateAt = System.currentTimeMillis();

        if (previous == null || previous != online) {
            if (online) {
                db.insertEvent("vehicle_connected", "Véhicule connecté");
            } else {
                db.insertEvent("vehicle_disconnected", "Véhicule déconnecté");
                notifications.notify(NotificationPrefs.Kind.VEHICLE_OFFLINE, "Véhicule hors ligne",
                        "Le véhicule ne publie plus de données.");
            }
        }
    }

    private void handleTelemetry(JSONObject json) {
        state.lastUpdateAt = json.optLong("timestamp", System.currentTimeMillis());
        state.speedKmh = optDouble(json, "speedKmh");
        state.latitude = optDouble(json, "latitude");
        state.longitude = optDouble(json, "longitude");
        state.altitude = optDouble(json, "altitude");
        state.accuracy = optDouble(json, "accuracy");
        state.androidBattery = optInt(json, "androidBattery");
        state.networkType = json.isNull("networkType") ? null : json.optString("networkType", null);
        state.signalLevel = optInt(json, "signalLevel");
        state.soc = optDouble(json, "soc");
        state.rangeKm = optDouble(json, "rangeKm");
        state.powerKw = optDouble(json, "powerKw");
        state.torqueNm = optDouble(json, "torqueNm");
        state.batteryTemp = optDouble(json, "batteryTemp");

        updateTrip();
        checkLowBattery();
    }

    private void handleLocation(JSONObject json) {
        Double lat = optDouble(json, "latitude");
        Double lon = optDouble(json, "longitude");
        if (lat != null) state.latitude = lat;
        if (lon != null) state.longitude = lon;
        Double alt = optDouble(json, "altitude");
        if (alt != null) state.altitude = alt;
        Double acc = optDouble(json, "accuracy");
        if (acc != null) state.accuracy = acc;
        Double speed = optDouble(json, "speedKmh");
        if (speed != null) state.speedKmh = speed;
    }

    private void handleAndroidHealth(JSONObject json) {
        Integer battery = optInt(json, "androidBattery");
        if (battery != null) {
            state.androidBattery = battery;
        }
    }

    private void handleNetworkStatus(JSONObject json) {
        if (!json.isNull("networkType")) {
            state.networkType = json.optString("networkType", state.networkType);
        }
        Integer signal = optInt(json, "signalLevel");
        if (signal != null) {
            state.signalLevel = signal;
        }
    }

    private void handleEvent(JSONObject json) {
        String type = json.optString("type", "event");
        String message = json.optString("message", "");
        db.insertEvent(type, message);

        NotificationPrefs.Kind kind = mapEventToNotificationKind(type);
        if (kind != null) {
            notifications.notify(kind, labelForEvent(type), message);
        }
    }

    private NotificationPrefs.Kind mapEventToNotificationKind(String type) {
        switch (type) {
            case "door_open":
            case "door_closed":
            case "trunk_open":
                return NotificationPrefs.Kind.DOOR;
            case "low_battery":
                return NotificationPrefs.Kind.LOW_BATTERY;
            case "charge_started":
            case "charge_complete":
                return NotificationPrefs.Kind.CHARGE_COMPLETE;
            case "gps_lost":
                return NotificationPrefs.Kind.GPS_LOST;
            case "service_error":
            case "android_restarted":
                return NotificationPrefs.Kind.SERVICE_ERROR;
            default:
                return null;
        }
    }

    private String labelForEvent(String type) {
        switch (type) {
            case "door_open": return "Porte ouverte";
            case "door_closed": return "Porte fermée";
            case "trunk_open": return "Coffre ouvert";
            case "low_battery": return "Batterie faible";
            case "charge_started": return "Charge démarrée";
            case "charge_complete": return "Charge terminée";
            case "gps_lost": return "Signal GPS perdu";
            case "service_error": return "Erreur du service voiture";
            case "android_restarted": return "Redémarrage du système Android";
            default: return type;
        }
    }

    private void checkLowBattery() {
        boolean low = (state.androidBattery != null && state.androidBattery <= LOW_BATTERY_THRESHOLD)
                || (state.soc != null && state.soc <= LOW_BATTERY_THRESHOLD);
        if (low && !lowBatteryAlerted) {
            lowBatteryAlerted = true;
            db.insertEvent("low_battery", "Batterie faible détectée");
            notifications.notify(NotificationPrefs.Kind.LOW_BATTERY, "Batterie faible",
                    "Le niveau de batterie est descendu sous " + LOW_BATTERY_THRESHOLD + "%.");
        } else if (!low) {
            lowBatteryAlerted = false;
        }
    }

    private void updateTrip() {
        Double speed = state.speedKmh;
        long now = System.currentTimeMillis();

        if (activeTripId < 0) {
            if (speed != null && speed > TRIP_START_SPEED_KMH) {
                activeTripId = db.startTrip(now, state.latitude, state.longitude);
                tripDistanceKm = 0;
                tripMaxSpeedKmh = speed;
                tripSpeedSum = speed;
                tripSpeedCount = 1;
                lastPointLat = state.latitude;
                lastPointLon = state.longitude;
                firstLowSpeedAt = null;
                db.insertEvent("trip_start", "Début du trajet");
                notifications.notify(NotificationPrefs.Kind.TRIP_START_END, "Trajet démarré",
                        "Le véhicule a commencé à rouler.");
                notifications.notify(NotificationPrefs.Kind.VEHICLE_MOVING, "Déplacement détecté",
                        "Le véhicule est en mouvement.");
            }
            return;
        }

        if (state.latitude != null && state.longitude != null) {
            if (lastPointLat != null && lastPointLon != null) {
                tripDistanceKm += GeoUtils.distanceKm(lastPointLat, lastPointLon, state.latitude, state.longitude);
            }
            lastPointLat = state.latitude;
            lastPointLon = state.longitude;
            db.addTripPoint(activeTripId, now, state.latitude, state.longitude, state.altitude, speed, state.powerKw);
        }
        if (speed != null) {
            tripMaxSpeedKmh = Math.max(tripMaxSpeedKmh, speed);
            tripSpeedSum += speed;
            tripSpeedCount++;
        }

        boolean stopped = speed == null || speed <= TRIP_STOP_SPEED_KMH;
        if (stopped) {
            if (firstLowSpeedAt == null) {
                firstLowSpeedAt = now;
            } else if (now - firstLowSpeedAt >= TRIP_STOP_GRACE_MS) {
                double avgSpeed = tripSpeedCount == 0 ? 0 : tripSpeedSum / tripSpeedCount;
                db.finishTrip(activeTripId, now, state.latitude, state.longitude, tripDistanceKm, avgSpeed, tripMaxSpeedKmh);
                db.insertEvent("trip_end", "Fin du trajet — " + String.format(java.util.Locale.FRANCE, "%.1f km", tripDistanceKm));
                notifications.notify(NotificationPrefs.Kind.TRIP_START_END, "Trajet terminé",
                        String.format(java.util.Locale.FRANCE, "%.1f km parcourus", tripDistanceKm));
                activeTripId = -1L;
                firstLowSpeedAt = null;
            }
        } else {
            firstLowSpeedAt = null;
        }
    }

    private void publish() {
        VehicleState snapshot = state.copy();
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                listener.onStateChanged(snapshot);
            }
        });
    }

    private void persistSnapshot() {
        try {
            JSONObject json = new JSONObject();
            json.put("online", state.online == null ? JSONObject.NULL : state.online);
            json.put("lastUpdateAt", state.lastUpdateAt);
            json.put("speedKmh", state.speedKmh == null ? JSONObject.NULL : state.speedKmh);
            json.put("latitude", state.latitude == null ? JSONObject.NULL : state.latitude);
            json.put("longitude", state.longitude == null ? JSONObject.NULL : state.longitude);
            json.put("altitude", state.altitude == null ? JSONObject.NULL : state.altitude);
            json.put("accuracy", state.accuracy == null ? JSONObject.NULL : state.accuracy);
            json.put("androidBattery", state.androidBattery == null ? JSONObject.NULL : state.androidBattery);
            json.put("networkType", state.networkType == null ? JSONObject.NULL : state.networkType);
            json.put("signalLevel", state.signalLevel == null ? JSONObject.NULL : state.signalLevel);
            json.put("soc", state.soc == null ? JSONObject.NULL : state.soc);
            json.put("rangeKm", state.rangeKm == null ? JSONObject.NULL : state.rangeKm);
            json.put("powerKw", state.powerKw == null ? JSONObject.NULL : state.powerKw);
            json.put("torqueNm", state.torqueNm == null ? JSONObject.NULL : state.torqueNm);
            json.put("batteryTemp", state.batteryTemp == null ? JSONObject.NULL : state.batteryTemp);
            prefs.edit().putString("snapshot", json.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    private void restorePersistedSnapshot() {
        String raw = prefs.getString("snapshot", null);
        if (raw == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject(raw);
            state.online = json.isNull("online") ? null : json.getBoolean("online");
            state.lastUpdateAt = json.optLong("lastUpdateAt", 0L);
            state.speedKmh = optDouble(json, "speedKmh");
            state.latitude = optDouble(json, "latitude");
            state.longitude = optDouble(json, "longitude");
            state.altitude = optDouble(json, "altitude");
            state.accuracy = optDouble(json, "accuracy");
            state.androidBattery = optInt(json, "androidBattery");
            state.networkType = json.isNull("networkType") ? null : json.optString("networkType", null);
            state.signalLevel = optInt(json, "signalLevel");
            state.soc = optDouble(json, "soc");
            state.rangeKm = optDouble(json, "rangeKm");
            state.powerKw = optDouble(json, "powerKw");
            state.torqueNm = optDouble(json, "torqueNm");
            state.batteryTemp = optDouble(json, "batteryTemp");
        } catch (JSONException ignored) {
        }
    }

    private static Double optDouble(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        return json.optDouble(key);
    }

    private static Integer optInt(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        return json.optInt(key);
    }
}
