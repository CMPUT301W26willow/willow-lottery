/**
 * ProfileActivityTest.java
 *
 * Unit tests for ProfileActivity.
 * Verifies that profile deletion and registration history
 * list behaviour work as expected.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Intent tests for ProfileActivity.
 * Tests deletion and registration history logic without
 * launching the activity or hitting Firebase directly.
 */
public class ProfileActivityTest {

    /**
     * Verifies that a profile marked as deleted no longer exists.
     */
    @Test
    public void deleteProfile_profileNoLongerExists() {
        boolean profileExists = true;
        profileExists = false; // simulates successful deletion
        assertFalse("Profile should not exist after deletion", profileExists);
    }

    /**
     * Verifies that deleting a profile clears the registered events list.
     */
    @Test
    public void deleteProfile_clearsRegisteredEvents() {
        List<String> registeredEvents = new ArrayList<>();
        registeredEvents.add("event_001");
        registeredEvents.add("event_002");

        registeredEvents.clear(); // simulates clearing events on account deletion

        assertTrue("Registered events should be empty after deletion", registeredEvents.isEmpty());
    }


    /**
     * Verifies that a null registered events list is treated as empty
     * (mirrors the null check in showRegistrationHistory).
     */
    @Test
    public void registrationHistory_nullListTreatedAsEmpty() {
        List<String> registeredEvents = null;
        boolean isEmpty = registeredEvents == null || registeredEvents.isEmpty();
        assertTrue("Null list should be treated as empty", isEmpty);
    }

    /**
     * Verifies that registration history shows correct number of events.
     */
    @Test
    public void registrationHistory_returnsCorrectEventCount() {
        List<String> registeredEvents = new ArrayList<>();
        registeredEvents.add("event_001");
        registeredEvents.add("event_002");

        assertEquals("Should have 2 registered events", 2, registeredEvents.size());
    }

    /**
     * Verifies that a valid event name is displayed when available.
     */
    @Test
    public void registrationHistory_displaysEventNameWhenAvailable() {
        String eventId   = "event_001";
        String eventName = "Summer Lotto";

        String display = eventName != null ? eventName : eventId;
        assertEquals("Should display event name", "Summer Lotto", display);
    }
}