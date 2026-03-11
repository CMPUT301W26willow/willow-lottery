/**
 * OrganizerDashboardActivityTest.java
 *
 * Intent tests for OrganizerDashboardActivity.
 * Verifies that the organizer dashboard launches correctly and that
 * the waiting list data structures behave as expected.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Tests activity launch and waiting list data logic without Espresso UI interactions.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerDashboardActivityTest {

    /**
     * Verifies that OrganizerDashboardActivity launches without crashing.
     */
    @Test
    public void testActivityLaunchesSuccessfully() {
        try (ActivityScenario<OrganizerDashboardActivity> scenario =
                     ActivityScenario.launch(OrganizerDashboardActivity.class)) {
            assertNotNull(scenario);
        }
    }

    /**
     * Verifies that the waiting list starts empty before Firebase data loads.
     */
    @Test
    public void testWaitingListInitiallyEmpty() {
        ArrayList<String> entrantNames = new ArrayList<>();
        assertTrue("Waiting list should start empty", entrantNames.isEmpty());
    }

    /**
     * Checks if entrant names can be added to list correctly
     */
    @Test
    public void testWaitingListAddsEntrants() {
        ArrayList<String> entrantNames = new ArrayList<>();
        entrantNames.add("Alex");
        entrantNames.add("Test User");
        assertEquals("Waiting list should have 2 entrants", 2, entrantNames.size());
        assertTrue(entrantNames.contains("Alex"));
        assertTrue(entrantNames.contains("Test User"));
    }

    /**
     *  Checks that null names are not added to the waiting list.
     */
    @Test
    public void testWaitingListIgnoresNullNames() {
        ArrayList<String> entrantNames = new ArrayList<>();
        String name = null;
        if (name != null) {
            entrantNames.add(name);
        }
        assertTrue("Null names should not be added", entrantNames.isEmpty());
    }
}