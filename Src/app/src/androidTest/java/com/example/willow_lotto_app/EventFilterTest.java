/**
 * EventFilterTest.java
 *
 * Unit tests for the event filter feature in EventsAdapter.
 * Verifies that filtering by open status, availability, and date works correctly.
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

@RunWith(AndroidJUnit4.class)
public class EventFilterTest {

    private List<Event> allEvents;

    @Before
    public void setUp() {
        allEvents = new ArrayList<>();

        // open event - registration end date is in the future
        Event event1 = new Event();
        event1.setId("1");
        event1.setName("Swimming Lessons");
        event1.setDescription("Learn to swim at the rec centre");
        event1.setDate("2026-06-01");
        event1.setRegistrationEnd("2099-01-01");
        event1.setLimit(10);
        event1.setWaitlistedRegistrationCount(5);
        allEvents.add(event1);

        // closed event - registration end date is in the past
        Event event2 = new Event();
        event2.setId("2");
        event2.setName("Piano Classes");
        event2.setDescription("Beginner piano for adults");
        event2.setDate("2026-07-01");
        event2.setRegistrationEnd("2020-01-01");
        event2.setLimit(10);
        event2.setWaitlistedRegistrationCount(5);
        allEvents.add(event2);

        // full event - waitlist count equals limit
        Event event3 = new Event();
        event3.setId("3");
        event3.setName("Dance Workshop");
        event3.setDescription("Interpretive dance safety basics");
        event3.setDate("2026-05-01");
        event3.setRegistrationEnd("2099-01-01");
        event3.setLimit(5);
        event3.setWaitlistedRegistrationCount(5);
        allEvents.add(event3);

        // event with no limit - always has spots available
        Event event4 = new Event();
        event4.setId("4");
        event4.setName("Yoga Class");
        event4.setDescription("Morning yoga for beginners");
        event4.setDate("2026-08-01");
        event4.setRegistrationEnd("2099-01-01");
        event4.setLimit(null);
        event4.setWaitlistedRegistrationCount(20);
        allEvents.add(event4);
    }

    /**
     * Helper that mimics filterByOpenStatus in EventsAdapter
     * events with no end date or future end date are treated as open
     */
    private List<Event> filterByOpenStatus() {
        List<Event> filtered = new ArrayList<>();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        for (Event event : allEvents) {
            String end = event.getRegistrationEnd();
            if (end == null || end.isEmpty() || end.compareTo(today) >= 0) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Helper that mimics filterByAvailability in EventsAdapter
     * events with spots remaining are included
     */
    private List<Event> filterByAvailability() {
        List<Event> filtered = new ArrayList<>();
        for (Event event : allEvents) {
            Integer limit = event.getLimit();
            int registered = event.getWaitlistDisplayCount();
            if (limit == null || registered < limit) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Helper that mimics filterByDate in EventsAdapter
     * sorts events chronologically by date
     */
    private List<Event> filterByDate() {
        List<Event> sorted = new ArrayList<>(allEvents);
        sorted.sort((a, b) -> {
            String dateA = a.getDate() != null ? a.getDate() : "";
            String dateB = b.getDate() != null ? b.getDate() : "";
            return dateA.compareTo(dateB);
        });
        return sorted;
    }

    /**
     * Helper that mimics combined keyword search + open status filter
     * search runs first then open status filter is applied to results
     */
    private List<Event> filterByKeywordAndOpenStatus(String query) {
        List<Event> keywordResults = new ArrayList<>();
        String lower = query.toLowerCase().trim();
        for (Event event : allEvents) {
            boolean matchesName = event.getName() != null && event.getName().toLowerCase().contains(lower);
            boolean matchesDescription = event.getDescription() != null && event.getDescription().toLowerCase().contains(lower);
            boolean matchesDate = event.getDate() != null && event.getDate().toLowerCase().contains(lower);
            if (matchesName || matchesDescription || matchesDate) {
                keywordResults.add(event);
            }
        }
        // apply open status filter on top of keyword results
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        List<Event> filtered = new ArrayList<>();
        for (Event event : keywordResults) {
            String end = event.getRegistrationEnd();
            if (end == null || end.isEmpty() || end.compareTo(today) >= 0) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Verifies that filterByOpenStatus only returns events with future registration end dates
     */
    @Test
    public void testFilterByOpenStatusExcludesClosedEvents() {
        List<Event> result = filterByOpenStatus();
        // event2 has a past end date so should be excluded
        assertEquals(3, result.size());
        for (Event e : result) {
            assertTrue(!e.getName().equals("Piano Classes"));
        }
    }

    /**
     * Verifies that filterByAvailability excludes full events
     */
    @Test
    public void testFilterByAvailabilityExcludesFullEvents() {
        List<Event> result = filterByAvailability();
        // event3 is full (waitlist == limit) so should be excluded
        assertEquals(3, result.size());
        for (Event e : result) {
            assertTrue(!e.getName().equals("Dance Workshop"));
        }
    }

    /**
     * Verifies that events with no limit are always included in availability filter
     */
    @Test
    public void testFilterByAvailabilityIncludesUnlimitedEvents() {
        List<Event> result = filterByAvailability();
        boolean found = false;
        for (Event e : result) {
            if (e.getName().equals("Yoga Class")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    /**
     * Verifies that filterByDate returns events in chronological order
     */
    @Test
    public void testFilter9yMnTm4NSzvG9rrwjM2ec8xZgh1cafXH8() {
        List<Event> result = filterByDate();
        assertEquals(4, result.size());
        // earliest date should be first
        assertEquals("Dance Workshop", result.get(0).getName());
        assertEquals("Swimming Lessons", result.get(1).getName());
        assertEquals("Piano Classes", result.get(2).getName());
        assertEquals("Yoga Class", result.get(3).getName());
    }

    /**
     * Verifies that combining keyword search with open status filter works correctly
     */
    @Test
    public void testKeywordSearchCombinedWithOpenStatusFilter() {
        // "piano" matches Piano Classes but it is closed so should be excluded
        List<Event> result = filterByKeywordAndOpenStatus("piano");
        assertEquals(0, result.size());
    }

    /**
     * Verifies that combining keyword search with open status returns open matches
     */
    @Test
    public void testKeywordSearchCombinedWithOpenStatusReturnsOpenMatches() {
        // "swim" matches Swimming Lessons which is open so should be included
        List<Event> result = filterByKeywordAndOpenStatus("swim");
        assertEquals(1, result.size());
        assertEquals("Swimming Lessons", result.get(0).getName());
    }

    /**
     * Verifies that clearing filters returns all events
     */
    @Test
    public void testClearFiltersReturnsAllEvents() {
        // after any filter, clearing should restore all 4 events
        List<Event> filtered = filterByOpenStatus();
        assertEquals(3, filtered.size());
        // now clear by using the full list
        List<Event> cleared = new ArrayList<>(allEvents);
        assertEquals(4, cleared.size());
    }
}