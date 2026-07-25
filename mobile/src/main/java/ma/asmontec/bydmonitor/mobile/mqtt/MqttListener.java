package ma.asmontec.bydmonitor.mobile.mqtt;

public interface MqttListener {
    void onConnected();

    void onMessage(String topic, byte[] payload);

    void onDisconnected(Throwable reason);
}
