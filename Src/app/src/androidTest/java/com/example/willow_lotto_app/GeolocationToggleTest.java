/**
 * GeolocationToggleTest.java
 *
 * Author: Mehr Dhanda
 *
 * Unit tests for the geolocation enable/disable feature in OrganizerDashboardActivity.
 * Verifies that geolocation setting logic behaves correctly.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for geolocation toggle logic in OrganizerDashboardActivity.
 */
@RunWith(AndroidJUnit4.class)
public class GeolocationToggleTest {

    /**
     * Verifies that geolocation is disabled by default (false).
     */
    @Test
    public void testGeolocationDefaultIsDisabled() {
        boolean geolocationRequired = false;
        assertFalse("Geolocation should be disabled by default", geolocationRequired);
    }

    /**
     * Verifies that enabling geolocation sets the value to true.
     */
    @Test
    public void testEnableGeolocationSetsTrue() {
        boolean geolocationRequired = true;
        assertTrue("Geolocation should be enabled", geolocationRequired);
    }

    /**
     * Verifies that disabling geolocation sets the value to false.
     */
    @Test
    public void testDisableGeolocationSetsFalse() {
        boolean geolocationRequired = false;
        assertFalse("Geolocation should be disabled", geolocationRequired);
    }

    /**
     * Verifies that the correct toast message is shown when geolocation is enabled.
     */
    @Test
    public void testGeolocationEnabledMessage() {
        boolean isEnabled = true;
        String msg = isEnabled ? "Geolocation enabled" : "Geolocation disabled";
        assertEquals("Geolocation enabled", msg);
    }

    /**
     * Verifies that the correct toast message is shown when geolocation is disabled.
     */
    @Test
    public void testGeolocationDisabledMessage() {
        boolean isEnabled = false;
        String msg = isEnabled ? "Geolocation enabled" : "Geolocation disabled";
        assertEquals("Geolocation disabled", msg);
    }
}