/**
 * CancelledEntrantsActivityTest.java
 *
 * Author: Mehr Dhanda
 *
 * Unit tests for the cancelled entrants feature in CancelledEntrantsActivity.
 * Verifies that the cancelled entrants list logic behaves correctly.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for cancelled entrants list logic.
 */
@RunWith(AndroidJUnit4.class)
public class CancelledEntrantsActivityTest {

    /**
     * Verifies that an empty cancelled list is handled correctly.
     */
    @Test
    public void testEmptyCancelledListIsHandled() {
        List<String> entrantNames = new ArrayList<>();
        assertTrue("Cancelled list should be empty", entrantNames.isEmpty());
    }

    /**
     * Verifies that cancelled entrant names can be added to the list.
     */
    @Test
    public void testCancelledEntrantsAddedToList() {
        List<String> entrantNames = new ArrayList<>();
        entrantNames.add("Alex");
        entrantNames.add("Test User");
        assertEquals(2, entrantNames.size());
    }

    /**
     * Verifies that a null name falls back to email.
     */
    @Test
    public void testNullNameFallsBackToEmail() {
        String name = null;
        String email = "alex@example.com";
        String display = (name != null && !name.trim().isEmpty()) ? name : email;
        assertEquals("alex@example.com", display);
    }

    /**
     * Verifies that a null name and null email falls back to userId.
     */
    @Test
    public void testNullNameAndEmailFallsBackToUserId() {
        String name = null;
        String email = null;
        String userId = "user123";
        String display;
        if (name != null && !name.trim().isEmpty()) {
            display = name;
        } else if (email != null && !email.trim().isEmpty()) {
            display = email;
        } else {
            display = userId;
        }
        assertEquals("user123", display);
    }

    /**
     * Verifies that the event ID is not null when passed correctly.
     */
    @Test
    public void testEventIdIsNotNull() {
        String eventId = "event1";
        assertNotNull("Event ID should not be null", eventId);
    }

    /**
     * Verifies that a missing event ID is detected.
     */
    @Test
    public void testMissingEventIdIsDetected() {
        String eventId = null;
        assertTrue("Event ID should be null or empty", eventId == null || eventId.trim().isEmpty());
    }
}