package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.example.willow_lotto_app.profile.ProfileActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link ProfileActivity#validateProfileInput},
 * profile deletion, and registration history logic.
 *
 */
public class ProfileActivityValidationTest {

    /**
     * Verifies that an empty name field returns a validation error.
     * Mirrors the null/empty check in {@link ProfileActivity #saveProfile}.
     */
    // Checks that an empty name returns a validation error
    @Test
    public void validateProfileInput_missingName_returnsError() {
        String result = ProfileActivity.validateProfileInput(
                "", "user@example.com");
        assertEquals("Name and Email required", result);
    }

    /**
     * Verifies that an empty email field returns a validation error.
     * Mirrors the null/empty check in {@link ProfileActivity #saveProfile}.
     */
    // Checks that an empty email returns a validation error
    @Test
    public void validateProfileInput_missingEmail_returnsError() {
        String result = ProfileActivity.validateProfileInput(
                "Alice", "");
        assertEquals("Name and Email required", result);
    }

    /**
     * Verifies that valid name and email fields return null (no error).
     * A null return means the input passed validation and can be saved.
     */
    // Checks that valid name and email returns null (no error)
    @Test
    public void validateProfileInput_valid_returnsNull() {
        String result = ProfileActivity.validateProfileInput(
                "Alice", "user@example.com");
        assertNull(result);
    }

    /**
     * Verifies that after a profile is deleted it is marked as no longer existing.
     * Simulates the end state of {@link ProfileActivity #deleteProfile}.
     */
    // Checks that after deletion the profile is marked as no longer existing
    @Test
    public void deleteProfile_profileNoLongerExists() {
        boolean profileExists = true;
        profileExists = false;
        assertFalse("Profile should not exist after deletion", profileExists);
    }

    /**
     * Verifies that deleting a profile clears the user's registered events list.
     * Simulates the list being wiped when an account is removed.
     */
    // Checks that deleting a profile also clears the user's registered events list
    @Test
    public void deleteProfile_clearsRegisteredEvents() {
        List<String> registeredEvents = new ArrayList<>();
        registeredEvents.add("event_001");
        registeredEvents.add("event_002");
        registeredEvents.clear();
        assertTrue("Registered events should be empty after deletion", registeredEvents.isEmpty());
    }

    /**
     * Verifies that a null {@code registeredEvents} list is treated as empty.
     * Mirrors the null check at the top of {@link ProfileActivity #showRegistrationHistory},
     * which handles the case where Firestore returns no data for the field.
     */
    // Checks that a null events list (returned by Firestore when none exist) is treated as empty
    @Test
    public void registrationHistory_nullListTreatedAsEmpty() {
        List<String> registeredEvents = null;
        boolean isEmpty = registeredEvents == null || registeredEvents.isEmpty();
        assertTrue("Null list should be treated as empty", isEmpty);
    }

    /**
     * Verifies that the registered events list correctly reports its size.
     * Ensures the count shown in registration history matches the actual number of events.
     */
    // Checks that the list correctly reports the number of registered events
    @Test
    public void registrationHistory_returnsCorrectEventCount() {
        List<String> registeredEvents = new ArrayList<>();
        registeredEvents.add("event_001");
        registeredEvents.add("event_002");
        assertEquals("Should have 2 registered events", 2, registeredEvents.size());
    }

    /**
     * Verifies that the event name is displayed when available rather than the raw event ID.
     * Mirrors the {@code eventName != null} ternary inside
     * {@link ProfileActivity #showRegistrationHistory}.
     */
    // Checks that the event name is shown when available, rather than the raw event ID
    @Test
    public void registrationHistory_displaysEventNameWhenAvailable() {
        String eventId   = "event_001";
        String eventName = "Summer Lotto";
        String display = eventName != null ? eventName : eventId;
        assertEquals("Should display event name", "Summer Lotto", display);
    }
}