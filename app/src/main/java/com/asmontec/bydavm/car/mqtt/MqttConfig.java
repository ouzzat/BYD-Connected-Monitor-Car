package com.asmontec.bydavm.car.mqtt;

/**
 * Paramètres MQTT publics, verrouillés dans l'application (serveur, port,
 * préfixe et topics). Ils ne sont pas modifiables depuis l'interface :
 * seuls les identifiants (utilisateur / mot de passe) sont saisis une fois
 * puis chiffrés via Android Keystore.
 */
public final class MqttConfig {

    public static final String BROKER_HOST = "f110c3c7fb39470e865d51c9530c8e9c.s1.eu.hivemq.cloud";
    public static final int BROKER_PORT = 8883;
    public static final String TOPIC_PREFIX = "asmontec/byd/seal-u";

    public static final String TOPIC_STATUS = TOPIC_PREFIX + "/status";
    public static final String TOPIC_TELEMETRY_LIVE = TOPIC_PREFIX + "/telemetry/live";
    public static final String TOPIC_LOCATION_LIVE = TOPIC_PREFIX + "/location/live";
    public static final String TOPIC_EVENTS = TOPIC_PREFIX + "/events";
    public static final String TOPIC_TRIPS_CURRENT = TOPIC_PREFIX + "/trips/current";
    public static final String TOPIC_TRIPS_HISTORY = TOPIC_PREFIX + "/trips/history";
    public static final String TOPIC_ANDROID_HEALTH = TOPIC_PREFIX + "/android/health";
    public static final String TOPIC_NETWORK_STATUS = TOPIC_PREFIX + "/network/status";
    public static final String TOPIC_CAMERA_STATUS = TOPIC_PREFIX + "/camera/status";

    /** Topic d'abonnement utilisé par l'application mobile uniquement. */
    public static final String SUBSCRIBE_ALL = TOPIC_PREFIX + "/#";

    private MqttConfig() {
    }
}
