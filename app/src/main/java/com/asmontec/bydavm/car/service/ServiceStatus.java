package com.asmontec.bydavm.car.service;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Petit répartiteur d'état en mémoire (même processus) entre le service
 * d'agent et l'interface. Évite tout broadcast inter-processus superflu.
 */
public final class ServiceStatus {

    public enum State {
        IDLE,
        CONFIG_REQUIRED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        GPS_UNAVAILABLE
    }

    public interface Listener {
        void onStatusChanged(State state, String detail, long timestamp);
    }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static volatile State currentState = State.IDLE;
    private static volatile String currentDetail = "";
    private static volatile long currentTimestamp = 0L;

    private ServiceStatus() {
    }

    public static void register(Listener listener) {
        listeners.add(listener);
        listener.onStatusChanged(currentState, currentDetail, currentTimestamp);
    }

    public static void unregister(Listener listener) {
        listeners.remove(listener);
    }

    public static void publish(State state, String detail) {
        currentState = state;
        currentDetail = detail;
        currentTimestamp = System.currentTimeMillis();
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                listener.onStatusChanged(state, detail, currentTimestamp);
            }
        });
    }

    public static State currentState() {
        return currentState;
    }
}
