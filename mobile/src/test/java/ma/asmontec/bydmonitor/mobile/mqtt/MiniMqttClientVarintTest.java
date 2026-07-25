package ma.asmontec.bydmonitor.mobile.mqtt;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;

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
}
