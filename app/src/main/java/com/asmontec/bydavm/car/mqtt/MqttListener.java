package com.asmontec.bydavm.car.mqtt;

public interface MqttListener {
    void onConnected();
    void onDisconnected(Throwable reason);
}
