package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RegistrationHistoryTest.java
 *
 * Unit tests for the registration history display logic in ProfileActivity.
 * Tests the formatting logic that builds the history string from registration data
 * without requiring Firestore or Firebase Auth.
 */
@RunWith(AndroidJUnit4.class)
public class RegistrationHistoryTest {

    private List<Map<String, String>> registrations;

    @Before
    public void setUp() {
        registrations = new ArrayList<>();

        // registration 1 - waitlisted
        Map<String, String> reg1 = new HashMap<>();
        reg1.put("eventId", "event_001");
        reg1.put("eventName", "Swimming Lessons");
        reg1.put("status", "waitlisted");
        reg1.put("userId", "user_001");
        registrations.add(reg1);

        // registration 2 - invited
        Map<String, String> reg2 = new HashMap<>();
        reg2.put("eventId", "event_002");
        reg2.put("eventName", "Piano Classes");
        reg2.put("status", "invited");
        reg2.put("userId", "user_001");
        registrations.add(reg2);

        // registration 3 - accepted
        Map<String, String> reg3 = new HashMap<>();
        reg3.put("eventId", "event_003");
        reg3.put("eventName", "Dance Workshop");
        reg3.put("status", "accepted");
        reg3.put("userId", "user_001");
        registrations.add(reg3);
    }

    /**
     * Helper that mimics the history string building logic in showRegistrationHistory
     */
    private String buildHistoryString(List<Map<String, String>> regs) {
        StringBuilder history = new StringBuilder();
        for (Map<String, String> doc : regs) {
            String eventName = doc.get("eventName");
            String status = doc.get("status");
            history.append("• ")
                    .append(eventName != null ? eventName : "(Unknown Event)")
                    .append(status != null ? " — " + status : "")
                    .append("\n\n");
        }
        return history.toString().trim();
    }

    /**
     * Tests that history string contains all event names
     */
    @Test
    public void testHistoryContainsAllEventNames() {
        String result = buildHistoryString(registrations);
        assertTrue(result.contains("Swimming Lessons"));
        assertTrue(result.contains("Piano Classes"));
        assertTrue(result.contains("Dance Workshop"));
    }

    /**
     * Tests that history string contains all statuses
     */
    @Test
    public void testHistoryContainsAllStatuses() {
        String result = buildHistoryString(registrations);
        assertTrue(result.contains("waitlisted"));
        assertTrue(result.contains("invited"));
        assertTrue(result.contains("accepted"));
    }

    /**
     * Tests that each entry is formatted correctly with bullet, name, and status
     */
    @Test
    public void testHistoryEntryFormat() {
        String result = buildHistoryString(registrations);
        assertTrue(result.contains("• Swimming Lessons — waitlisted"));
        assertTrue(result.contains("• Piano Classes — invited"));
        assertTrue(result.contains("• Dance Workshop — accepted"));
    }

    /**
     * Tests that a null eventName falls back to Unknown Event
     */
    @Test
    public void testNullEventNameFallsBackToUnknown() {
        List<Map<String, String>> regs = new ArrayList<>();
        Map<String, String> reg = new HashMap<>();
        reg.put("eventId", "event_004");
        reg.put("eventName", null);
        reg.put("status", "waitlisted");
        regs.add(reg);

        String result = buildHistoryString(regs);
        assertTrue(result.contains("(Unknown Event)"));
    }

    /**
     * Tests that a null status does not crash and omits the status label
     */
    @Test
    public void testNullStatusDoesNotCrash() {
        List<Map<String, String>> regs = new ArrayList<>();
        Map<String, String> reg = new HashMap<>();
        reg.put("eventId", "event_005");
        reg.put("eventName", "Yoga Class");
        reg.put("status", null);
        regs.add(reg);

        String result = buildHistoryString(regs);
        assertTrue(result.contains("• Yoga Class"));
    }

    /**
     * Tests that an empty registration list produces an empty string
     */
    @Test
    public void testEmptyRegistr9yMnTm4NSzvG9rrwjM2ec8xZgh1cafXH8() {
        String result = buildHistoryString(new ArrayList<>());
        assertEquals("", result);
    }

    /**
     * Tests that showRegistrationHistory requires a signed in user
     * UNIMPLEMENTED - requires Firestore and Firebase Auth
     */
    @Test
    public void testShowRegistrationHistoryRequiresSignedInUser() {
        // left unimplemented matching the pattern of NotficationSentUnselected
    }
}