/**
 * EventSearchTest.java
 *
 * Author: Mehr Dhanda
 *
 * Unit tests for the event search/filter feature in EventsAdapter.
 * Verifies that keyword filtering works correctly across event fields.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.willow_lotto_app.events.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for event keyword search filtering logic.
 */
@RunWith(AndroidJUnit4.class)
public class EventSearchTest {

    private List<Event> allEvents;

    @Before
    public void setUp() {
        allEvents = new ArrayList<>();

        Event event1 = new Event();
        event1.setId("1");
        event1.setName("Swimming Lessons");
        event1.setDescription("Learn to swim at the rec centre");
        event1.setDate("2025-01-01");

        Event event2 = new Event();
        event2.setId("2");
        event2.setName("Piano Classes");
        event2.setDescription("Beginner piano for adults");
        event2.setDate("2025-02-01");

        Event event3 = new Event();
        event3.setId("3");
        event3.setName("Dance Workshop");
        event3.setDescription("Interpretive dance safety basics");
        event3.setDate("2025-03-01");

        allEvents.add(event1);
        allEvents.add(event2);
        allEvents.add(event3);
    }

    /**
     * Helper method that mimics the filter logic in EventsAdapter.
     */
    private List<Event> filter(String query) {
        List<Event> filtered = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(allEvents);
        } else {
            String lower = query.toLowerCase().trim();
            for (Event event : allEvents) {
                boolean matchesName = event.getName() != null && event.getName().toLowerCase().contains(lower);
                boolean matchesDescription = event.getDescription() != null && event.getDescription().toLowerCase().contains(lower);
                boolean matchesDate = event.getDate() != null && event.getDate().toLowerCase().contains(lower);
                if (matchesName || matchesDescription || matchesDate) {
                    filtered.add(event);
                }
            }
        }
        return filtered;
    }

    /**
     * Verifies that an empty query returns all events.
     */
    @Test
    public void testEmptyQueryReturnsAllEvents() {
        List<Event> result = filter("");
        assertEquals(3, result.size());
    }

    /**
     * Verifies that a null query returns all events.
     */
    @Test
    public void testNullQueryReturnsAllEvents() {
        List<Event> result = filter(null);
        assertEquals(3, result.size());
    }

    /**
     * Verifies that searching by event name returns the correct event.
     */
    @Test
    public void testSearchByName() {
        List<Event> result = filter("Swimming");
        assertEquals(1, result.size());
        assertEquals("Swimming Lessons", result.get(0).getName());
    }

    /**
     * Verifies that searching by description returns the correct event.
     */
    @Test
    public void testSearchByDescription() {
        List<Event> result = filter("Beginner piano");
        assertEquals(1, result.size());
        assertEquals("Piano Classes", result.get(0).getName());
    }

    /**
     * Verifies that searching by date returns the correct event.
     */
    @Test
    public void testSearchByDate() {
        List<Event> result = filter("2025-03");
        assertEquals(1, result.size());
        assertEquals("Dance Workshop", result.get(0).getName());
    }

    /**
     * Verifies that a query with no matches returns an empty list.
     */
    @Test
    public void testNoMatchReturnsEmptyList() {
        List<Event> result = filter("xyz123");
        assertEquals(0, result.size());
    }

    /**
     * Verifies that search is case insensitive.
     */
    @Test
    public void testSearchIsCaseInsensitive() {
        List<Event> result = filter("swimming");
        assertEquals(1, result.size());
        assertEquals("Swimming Lessons", result.get(0).getName());
    }
}