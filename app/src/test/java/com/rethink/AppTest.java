package com.rethink;

import com.rethink.drop.tools.Utilities;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppTest {
    @Test
    public void checkDistanceCalcCorrect() {
        double KM = Utilities.distanceInKilometers(
                39.952584,
                38.449569,
                -75.165222,
                -78.868916
        );
        double MI = Utilities.distanceInMiles(
                39.952584,
                38.449569,
                -75.165222,
                -78.868916
        );
        assertTrue(360.1 < KM && KM < 360.4);
        assertTrue(223.8 < MI && MI < 224.0);
    }

    @Test
    public void checkUseMetric() {
        Locale US = Locale.US;
        Locale CHINA = Locale.CHINA;

        assertFalse(Utilities.useMetric(US));
        assertTrue(Utilities.useMetric(CHINA));
    }
}
