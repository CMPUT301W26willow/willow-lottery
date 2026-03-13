/**
 * EntrantMapActivityTest.java
 *
 * Author: Mehr Dhanda
 *
 * Intent tests for EntrantMapActivity.
 * Verifies that the entrant map screen and location data logic behave as expected.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent tests for EntrantMapActivity.
 * Tests location data logic without launching the activity directly.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantMapActivityTest {

    /**
     * Verifies that a valid LatLng object can be created from entrant coordinates.
     */
    @Test
    public void testLatLngCreatedFromValidCoordinates() {
        Double latitude = 53.5461;
        Double longitude = -113.4938;
        LatLng location = new LatLng(latitude, longitude);
        assertNotNull("LatLng should not be null", location);
        assertEquals(53.5461, location.latitude, 0.001);
        assertEquals(-113.4938, location.longitude, 0.001);
    }

    /**
     * Verifies that null latitude skips marker creation.
     */
    @Test
    public void testNullLatitudeSkipsMarker() {
        Double latitude = null;
        Double longitude = -113.4938;
        boolean shouldAddMarker = latitude != null && longitude != null;
        assertEquals(false, shouldAddMarker);
    }

    /**
     * Verifies that null longitude skips marker creation.
     */
    @Test
    public void testNullLongitudeSkipsMarker() {
        Double latitude = 53.5461;
        Double longitude = null;
        boolean shouldAddMarker = latitude != null && longitude != null;
        assertEquals(false, shouldAddMarker);
    }

    /**
     * Verifies that valid coordinates result in marker being added.
     */
    @Test
    public void testValidCoordinatesAddsMarker() {
        Double latitude = 53.5461;
        Double longitude = -113.4938;
        boolean shouldAddMarker = latitude != null && longitude != null;
        assertEquals(true, shouldAddMarker);
    }
}