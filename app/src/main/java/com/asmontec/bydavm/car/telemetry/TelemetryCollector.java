package com.asmontec.bydavm.car.telemetry;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;

/**
 * Collecte uniquement des données réellement accessibles via les API Android
 * publiques : position/vitesse GPS, batterie de l'unité Android, type de
 * réseau et niveau de signal. Aucune donnée constructeur BYD n'est lue ici.
 */
public final class TelemetryCollector implements LocationListener {

    private static final String VEHICLE_ID = "byd-seal-u-01";

    private final Context context;
    private volatile Location lastLocation;

    public TelemetryCollector(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Démarre l'écoute GPS si l'autorisation de localisation est accordée. */
    public void startLocationUpdates() {
        if (!hasLocationPermission()) {
            return;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return;
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 5f, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, this);
            }
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) {
                lastLocation = last;
            }
        } catch (SecurityException ignored) {
            // Autorisation révoquée entre-temps : la télémétrie GPS restera null.
        }
    }

    public void stopLocationUpdates() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException ignored) {
            }
        }
    }

    public boolean isGpsAvailable() {
        return lastLocation != null && (System.currentTimeMillis() - lastLocation.getTime()) < 60_000L;
    }

    public VehicleTelemetry collect() {
        VehicleTelemetry telemetry = new VehicleTelemetry();
        telemetry.vehicleId = VEHICLE_ID;
        telemetry.timestamp = System.currentTimeMillis();
        telemetry.online = true;

        Location location = lastLocation;
        if (location != null && (System.currentTimeMillis() - location.getTime()) < 60_000L) {
            telemetry.latitude = location.getLatitude();
            telemetry.longitude = location.getLongitude();
            telemetry.altitude = location.hasAltitude() ? location.getAltitude() : null;
            telemetry.accuracy = location.hasAccuracy() ? (double) location.getAccuracy() : null;
            telemetry.speedKmh = location.hasSpeed() ? location.getSpeed() * 3.6 : null;
        }

        telemetry.androidBattery = readBatteryPercent();
        telemetry.networkType = readNetworkType();
        telemetry.signalLevel = readSignalLevel();

        return telemetry;
    }

    /** Convertit l'indicateur de force de signal exposé par ConnectivityManager (API 29+) en échelle 0-4. */
    private Integer readSignalLevel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return null;
        }
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) {
            return null;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)) {
            return null;
        }
        int raw = capabilities.getSignalStrength();
        if (raw == Integer.MIN_VALUE) {
            return null;
        }
        // Barème approximatif dBm -> 0..4, cohérent avec l'affichage opérateur habituel.
        if (raw >= -65) return 4;
        if (raw >= -75) return 3;
        if (raw >= -85) return 2;
        if (raw >= -95) return 1;
        return 0;
    }

    private boolean hasLocationPermission() {
        return ActivityCompatCheck.granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompatCheck.granted(context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private Integer readBatteryPercent() {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager == null) {
            return null;
        }
        int capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return in0to100(capacity) ? capacity : null;
    }

    private static boolean in0to100(int value) {
        return value >= 0 && value <= 100;
    }

    private String readNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return null;
        }
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) {
            return null;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null) {
            return null;
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "WiFi";
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "4G";
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return "Ethernet";
        }
        return "Inconnu";
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /** Petit indirecteur pour éviter de dupliquer les vérifications de permission. */
    private static final class ActivityCompatCheck {
        static boolean granted(Context context, String permission) {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
    }
}
