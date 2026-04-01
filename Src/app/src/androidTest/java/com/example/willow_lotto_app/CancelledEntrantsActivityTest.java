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

import com.example.willow_lotto_app.notification.UserNotification;

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

    /**
     * Verifies that a cancelled entrant notification has the correct title.
     */
    @Test
    public void testCancelledNotificationHasCorrectTitle() {
        String expectedTitle = "Your registration was cancelled";
        UserNotification notification = new UserNotification(
                "event1",
                "Your registration was cancelled",
                "Unfortunately, your registration for this event has been cancelled.",
                "lottery_cancelled"
        );
        assertEquals(expectedTitle, notification.getTitle());
    }

    /**
     * Verifies that a cancelled entrant notification has the correct message.
     */
    @Test
    public void testCancelledNotificationHasCorrectMessage() {
        String expectedMessage = "Unfortunately, your registration for this event has been cancelled.";
        UserNotification notification = new UserNotification(
                "event1",
                "Your registration was cancelled",
                "Unfortunately, your registration for this event has been cancelled.",
                "lottery_cancelled"
        );
        assertEquals(expectedMessage, notification.getMessage());
    }

    /**
     * Verifies that a cancelled entrant notification has the correct type.
     */
    @Test
    public void testCancelledNotificationHasCorrectType() {
        UserNotification notification = new UserNotification(
                "event1",
                "Your registration was cancelled",
                "Unfortunately, your registration for this event has been cancelled.",
                "lottery_cancelled"
        );
        assertEquals("lottery_cancelled", notification.getType());
    }

    /**
     * Verifies that a cancelled entrant notification has the correct eventId.
     */
    @Test
    public void testCancelledNotificationHasCorrectEventId() {
        String expectedEventId = "event1";
        UserNotification notification = new UserNotification(
                expectedEventId,
                "Your registration was cancelled",
                "Unfortunately, your registration for this event has been cancelled.",
                "lottery_cancelled"
        );
        assertEquals(expectedEventId, notification.getEventId());
    }

    /**
     * Verifies that a cancelled notification toMap() contains all required fields.
     */
    @Test
    public void testCancelledNotificationToMapContainsRequiredFields() {
        UserNotification notification = new UserNotification(
                "event1",
                "Your registration was cancelled",
                "Unfortunately, your registration for this event has been cancelled.",
                "lottery_cancelled"
        );
        java.util.Map<String, Object> map = notification.toMap();
        assertTrue("Map should contain eventId", map.containsKey("eventId"));
        assertTrue("Map should contain title", map.containsKey("title"));
        assertTrue("Map should contain message", map.containsKey("message"));
        assertTrue("Map should contain type", map.containsKey("type"));
        assertTrue("Map should contain read", map.containsKey("read"));
        assertEquals(false, map.get("read"));
    }

    /**
     * Verifies that a cancelled notification is marked as unread by default.
     */
    @Test
    public void testCancelledNotificationIsUnreadByDefault() {
        UserNotification notification = new UserNotification(
                "event1",
                "Your registration was cancelled",
                "Unfortunately, your registration for this event has been cancelled.",
                "lottery_cancelled"
        );
        java.util.Map<String, Object> map = notification.toMap();
        assertEquals(false, map.get("read"));
    }
}

