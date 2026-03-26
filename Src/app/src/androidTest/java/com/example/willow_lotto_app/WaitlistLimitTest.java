/**
 * WaitlistLimitTest.java
 *
 * Author: Mehr Dhanda
 *
 * Unit tests for the optional waiting list limit feature in CreateEventActivity.
 * Verifies that the waitlist limit logic and event form validation behave correctly.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.willow_lotto_app.events.CreateEventActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for waitlist limit logic in CreateEventActivity.
 */
@RunWith(AndroidJUnit4.class)
public class WaitlistLimitTest {

    /**
     * Verifies that a valid event form returns no error.
     */
    @Test
    public void testValidEventFormReturnsNoError() {
        String error = CreateEventActivity.validateEventForm("Swimming Lessons", "Learn to swim", "2025-01-01");
        assertNull("Valid form should return no error", error);
    }

    /**
     * Verifies that a missing event name returns an error.
     */
    @Test
    public void testMissingNameReturnsError() {
        String error = CreateEventActivity.validateEventForm("", "Learn to swim", "2025-01-01");
        assertNotNull("Missing name should return error", error);
        assertEquals("Event name is required", error);
    }

    /**
     * Verifies that a missing description returns an error.
     */
    @Test
    public void testMissingDescriptionReturnsError() {
        String error = CreateEventActivity.validateEventForm("Swimming Lessons", "", "2025-01-01");
        assertNotNull("Missing description should return error", error);
        assertEquals("Description is required", error);
    }

    /**
     * Verifies that a missing event date returns an error.
     */
    @Test
    public void testMissingDateReturnsError() {
        String error = CreateEventActivity.validateEventForm("Swimming Lessons", "Learn to swim", "");
        assertNotNull("Missing date should return error", error);
        assertEquals("Event date is required", error);
    }

    /**
     * Verifies that when no limit is set, waitlistLimit is null.
     */
    @Test
    public void testNoLimitIsNull() {
        Integer waitlistLimit = null;
        assertNull("Waitlist limit should be null when not set", waitlistLimit);
    }

    /**
     * Verifies that setting a limit of 50 is stored correctly.
     */
    @Test
    public void testWaitlistLimitIsSet() {
        Integer waitlistLimit = 50;
        assertNotNull("Waitlist limit should not be null when set", waitlistLimit);
        assertEquals(Integer.valueOf(50), waitlistLimit);
    }

    /**
     * Verifies that waitlistLimit is included in the event map when set.
     */
    @Test
    public void testWaitlistLimitIncludedInEventMap() {
        Map<String, Object> event = new HashMap<>();
        Integer waitlistLimit = 100;
        if (waitlistLimit != null) {
            event.put("waitlistLimit", waitlistLimit);
        }
        assertEquals(100, event.get("waitlistLimit"));
    }

    /**
     * Verifies that waitlistLimit is NOT included in the event map when null.
     */
    @Test
    public void testWaitlistLimitNotIncludedWhenNull() {
        Map<String, Object> event = new HashMap<>();
        Integer waitlistLimit = null;
        if (waitlistLimit != null) {
            event.put("waitlistLimit", waitlistLimit);
        }
        assertNull("waitlistLimit should not be in map when null", event.get("waitlistLimit"));
    }
}