package com.asmontec.bydavm.car.telemetry;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Vérifie que les champs véhicule BYD non accessibles sont sérialisés
 * comme {@code null} et jamais comme une valeur simulée, conformément
 * au schéma JSON du projet.
 */
public class VehicleTelemetryTest {

    @Test
    public void unavailableVehicleFieldsAreSerializedAsNull() {
        VehicleTelemetry telemetry = new VehicleTelemetry();
        telemetry.vehicleId = "byd-seal-u-01";
        telemetry.timestamp = 1784929090922L;
        telemetry.online = true;
        telemetry.androidBattery = 78;
        telemetry.networkType = "4G";

        String json = telemetry.toJson();

        assertTrue(json.contains("\"soc\":null"));
        assertTrue(json.contains("\"rangeKm\":null"));
        assertTrue(json.contains("\"powerKw\":null"));
        assertTrue(json.contains("\"torqueNm\":null"));
        assertTrue(json.contains("\"batteryTemp\":null"));
        assertTrue(json.contains("\"androidBattery\":78"));
        assertTrue(json.contains("\"networkType\":\"4G\""));
    }

    @Test
    public void missingLocationIsNullRatherThanZero() {
        VehicleTelemetry telemetry = new VehicleTelemetry();
        telemetry.vehicleId = "byd-seal-u-01";

        String json = telemetry.toJson();

        assertTrue(json.contains("\"latitude\":null"));
        assertTrue(json.contains("\"longitude\":null"));
        assertTrue(json.contains("\"speedKmh\":null"));
    }
}
