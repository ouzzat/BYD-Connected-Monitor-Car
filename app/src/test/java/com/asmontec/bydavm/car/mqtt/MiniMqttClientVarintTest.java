package com.asmontec.bydavm.car.mqtt;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;

/**
 * Vérifie l'encodage/décodage de la longueur restante MQTT (algorithme
 * "variable byte integer" de la spécification 3.1.1), sans dépendance
 * Android : ce test s'exécute en JVM pure.
 */
public class MiniMqttClientVarintTest {

    @Test
    public void roundTripsKnownBoundaryValues() throws IOException {
        int[] values = {0, 1, 127, 128, 16383, 16384, 2097151, 2097152, 268435455};
        for (int value : values) {
            byte[] encoded = MiniMqttClient.encodeRemainingLength(value);
            int decoded = MiniMqttClient.readRemainingLength(new ByteArrayInputStream(encoded));
            assertEquals("valeur=" + value, value, decoded);
        }
    }

    @Test
    public void singleByteEncodingStaysUnderOneByteForSmallValues() {
        assertEquals(1, MiniMqttClient.encodeRemainingLength(127).length);
        assertEquals(2, MiniMqttClient.encodeRemainingLength(128).length);
    }
}
