package ma.asmontec.bydmonitor.mobile.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Base locale SQLite (historique des trajets et journal des événements).
 * Aucune donnée sensible n'y est stockée ; les identifiants MQTT restent
 * exclusivement dans {@link ma.asmontec.bydmonitor.mobile.security.SecureCredentialStore}.
 */
public final class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "byd_monitor.db";
    private static final int DB_VERSION = 1;

    public AppDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE trips (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "start_time INTEGER NOT NULL," +
                "end_time INTEGER," +
                "start_lat REAL, start_lon REAL," +
                "end_lat REAL, end_lon REAL," +
                "distance_km REAL DEFAULT 0," +
                "avg_speed_kmh REAL," +
                "max_speed_kmh REAL," +
                "avg_consumption REAL," +
                "energy_used_kwh REAL)");

        db.execSQL("CREATE TABLE trip_points (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "trip_id INTEGER NOT NULL," +
                "timestamp INTEGER NOT NULL," +
                "lat REAL, lon REAL, altitude REAL," +
                "speed_kmh REAL, power_kw REAL," +
                "FOREIGN KEY(trip_id) REFERENCES trips(id))");
        db.execSQL("CREATE INDEX idx_trip_points_trip ON trip_points(trip_id)");

        db.execSQL("CREATE TABLE events (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp INTEGER NOT NULL," +
                "type TEXT NOT NULL," +
                "message TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 uniquement pour le moment.
    }

    // --- Événements ---

    public void insertEvent(String type, String message) {
        ContentValues values = new ContentValues();
        values.put("timestamp", System.currentTimeMillis());
        values.put("type", type);
        values.put("message", message);
        getWritableDatabase().insert("events", null, values);
    }

    public static final class EventRow {
        public long timestamp;
        public String type;
        public String message;
    }

    public List<EventRow> listEvents(int limit) {
        List<EventRow> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT timestamp, type, message FROM events ORDER BY timestamp DESC LIMIT ?",
                new String[]{String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                EventRow row = new EventRow();
                row.timestamp = cursor.getLong(0);
                row.type = cursor.getString(1);
                row.message = cursor.getString(2);
                result.add(row);
            }
        }
        return result;
    }

    // --- Trajets ---

    public long startTrip(long startTime, Double lat, Double lon) {
        ContentValues values = new ContentValues();
        values.put("start_time", startTime);
        putNullableDouble(values, "start_lat", lat);
        putNullableDouble(values, "start_lon", lon);
        return getWritableDatabase().insert("trips", null, values);
    }

    public void addTripPoint(long tripId, long timestamp, Double lat, Double lon, Double altitude,
                              Double speedKmh, Double powerKw) {
        ContentValues values = new ContentValues();
        values.put("trip_id", tripId);
        values.put("timestamp", timestamp);
        putNullableDouble(values, "lat", lat);
        putNullableDouble(values, "lon", lon);
        putNullableDouble(values, "altitude", altitude);
        putNullableDouble(values, "speed_kmh", speedKmh);
        putNullableDouble(values, "power_kw", powerKw);
        getWritableDatabase().insert("trip_points", null, values);
    }

    public void finishTrip(long tripId, long endTime, Double endLat, Double endLon, double distanceKm,
                            double avgSpeedKmh, double maxSpeedKmh) {
        ContentValues values = new ContentValues();
        values.put("end_time", endTime);
        putNullableDouble(values, "end_lat", endLat);
        putNullableDouble(values, "end_lon", endLon);
        values.put("distance_km", distanceKm);
        values.put("avg_speed_kmh", avgSpeedKmh);
        values.put("max_speed_kmh", maxSpeedKmh);
        getWritableDatabase().update("trips", values, "id = ?", new String[]{String.valueOf(tripId)});
    }

    public static final class TripRow {
        public long id;
        public long startTime;
        public Long endTime;
        public double distanceKm;
        public Double avgSpeedKmh;
        public Double maxSpeedKmh;
    }

    public List<TripRow> listTrips(int limit) {
        List<TripRow> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, start_time, end_time, distance_km, avg_speed_kmh, max_speed_kmh " +
                        "FROM trips ORDER BY start_time DESC LIMIT ?",
                new String[]{String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                TripRow row = new TripRow();
                row.id = cursor.getLong(0);
                row.startTime = cursor.getLong(1);
                row.endTime = cursor.isNull(2) ? null : cursor.getLong(2);
                row.distanceKm = cursor.getDouble(3);
                row.avgSpeedKmh = cursor.isNull(4) ? null : cursor.getDouble(4);
                row.maxSpeedKmh = cursor.isNull(5) ? null : cursor.getDouble(5);
                result.add(row);
            }
        }
        return result;
    }

    public static final class TripPointRow {
        public long timestamp;
        public Double lat;
        public Double lon;
        public Double altitude;
        public Double speedKmh;
        public Double powerKw;
    }

    public List<TripPointRow> listTripPoints(long tripId) {
        List<TripPointRow> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT timestamp, lat, lon, altitude, speed_kmh, power_kw FROM trip_points " +
                        "WHERE trip_id = ? ORDER BY timestamp ASC",
                new String[]{String.valueOf(tripId)})) {
            while (cursor.moveToNext()) {
                TripPointRow row = new TripPointRow();
                row.timestamp = cursor.getLong(0);
                row.lat = cursor.isNull(1) ? null : cursor.getDouble(1);
                row.lon = cursor.isNull(2) ? null : cursor.getDouble(2);
                row.altitude = cursor.isNull(3) ? null : cursor.getDouble(3);
                row.speedKmh = cursor.isNull(4) ? null : cursor.getDouble(4);
                row.powerKw = cursor.isNull(5) ? null : cursor.getDouble(5);
                result.add(row);
            }
        }
        return result;
    }

    private static void putNullableDouble(ContentValues values, String key, Double value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }
}
