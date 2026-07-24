package com.asmontec.bydavm.car.telemetry;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Un instantané de télémétrie tel que défini par le schéma JSON du projet.
 * Toute valeur non accessible reste à {@code null} et est sérialisée comme
 * telle : aucune donnée simulée n'est jamais envoyée à sa place.
 */
public final class VehicleTelemetry {

    public String vehicleId;
    public long timestamp;
    public boolean online;
    public Double speedKmh;
    public Double latitude;
    public Double longitude;
    public Double altitude;
    public Double accuracy;
    public Integer androidBattery;
    public String networkType;
    public Integer signalLevel;

    // Données véhicule BYD/DiLink : non accessibles tant qu'aucune API officielle
    // n'est validée. Restent null par construction.
    public final Double soc = null;
    public final Double rangeKm = null;
    public final Double powerKw = null;
    public final Double torqueNm = null;
    public final Double batteryTemp = null;

    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("vehicleId", vehicleId);
            json.put("timestamp", timestamp);
            json.put("online", online);
            putNullable(json, "speedKmh", speedKmh);
            putNullable(json, "latitude", latitude);
            putNullable(json, "longitude", longitude);
            putNullable(json, "altitude", altitude);
            putNullable(json, "accuracy", accuracy);
            putNullable(json, "androidBattery", androidBattery);
            json.put("networkType", networkType == null ? JSONObject.NULL : networkType);
            putNullable(json, "signalLevel", signalLevel);
            putNullable(json, "soc", soc);
            putNullable(json, "rangeKm", rangeKm);
            putNullable(json, "powerKw", powerKw);
            putNullable(json, "torqueNm", torqueNm);
            putNullable(json, "batteryTemp", batteryTemp);
            return json.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Échec de sérialisation de la télémétrie", e);
        }
    }

    private static void putNullable(JSONObject json, String key, Object value) throws JSONException {
        json.put(key, value == null ? JSONObject.NULL : value);
    }
}
