package ma.asmontec.bydmonitor.mobile.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeoUtilsTest {

    @Test
    public void samePointHasZeroDistance() {
        assertEquals(0.0, GeoUtils.distanceKm(31.6245, -8.0123, 31.6245, -8.0123), 0.0001);
    }

    @Test
    public void knownDistanceIsWithinTolerance() {
        // Marrakech (31.6295, -7.9811) -> Casablanca (33.5731, -7.5898) ≈ 220 km à vol d'oiseau.
        double distance = GeoUtils.distanceKm(31.6295, -7.9811, 33.5731, -7.5898);
        assertTrue("distance=" + distance, distance > 200 && distance < 240);
    }
}
