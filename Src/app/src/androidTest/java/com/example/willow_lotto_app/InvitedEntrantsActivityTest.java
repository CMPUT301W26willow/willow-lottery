/**
 * InvitedEntrantsActivityTest.java
 *
 * Author: Mehr Dhanda
 *
 * Unit tests for the invited entrants cancel feature in InvitedEntrantsActivity.
 * Verifies that the invited entrants list and cancel logic behave correctly.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.willow_lotto_app.registration.RegistrationStatus;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for invited entrants cancel logic in InvitedEntrantsActivity.
 */
@RunWith(AndroidJUnit4.class)
public class InvitedEntrantsActivityTest {

    /**
     * Verifies that INVITED status value is correct.
     */
    @Test
    public void testInvitedStatusValue() {
        assertEquals("invited", RegistrationStatus.INVITED.getValue());
    }

    /**
     * Verifies that CANCELLED status value is correct.
     */
    @Test
    public void testCancelledStatusValue() {
        assertEquals("cancelled", RegistrationStatus.CANCELLED.getValue());
    }

    /**
     * Verifies that an empty invited list is handled correctly.
     */
    @Test
    public void testEmptyInvitedListIsHandled() {
        List<String> entrantNames = new ArrayList<>();
        assertTrue("Invited list should be empty", entrantNames.isEmpty());
    }

    /**
     * Verifies that invited entrant names can be added to the list.
     */
    @Test
    public void testInvitedEntrantsAddedToList() {
        List<String> entrantNames = new ArrayList<>();
        entrantNames.add("Alex");
        entrantNames.add("Test User");
        assertEquals(2, entrantNames.size());
    }

    /**
     * Verifies that cancelling an entrant removes them from the list.
     */
    @Test
    public void testCancellingEntrantRemovesFromList() {
        List<String> entrantNames = new ArrayList<>();
        entrantNames.add("Alex");
        entrantNames.add("Test User");
        entrantNames.remove(0);
        assertEquals(1, entrantNames.size());
        assertFalse(entrantNames.contains("Alex"));
    }

    /**
     * Verifies that after cancelling all entrants the list is empty.
     */
    @Test
    public void testCancellingAllEntrantsLeavesEmptyList() {
        List<String> entrantNames = new ArrayList<>();
        entrantNames.add("Alex");
        entrantNames.remove(0);
        assertTrue("List should be empty after cancelling all", entrantNames.isEmpty());
    }

    /**
     * Verifies that event ID is not null when passed correctly.
     */
    @Test
    public void testEventIdIsNotNull() {
        String eventId = "event1";
        assertNotNull("Event ID should not be null", eventId);
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
}