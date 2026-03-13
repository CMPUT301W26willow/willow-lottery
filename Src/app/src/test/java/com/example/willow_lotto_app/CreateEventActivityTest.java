package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link CreateEventActivity#validateEventForm}.
 */
public class CreateEventActivityTest {

    @Test
    public void validateEventForm_emptyName_returnsEventNameRequired() {
        String result = CreateEventActivity.validateEventForm("", "A description", "2025-06-01");
        assertEquals("Event name is required", result);
    }

    @Test
    public void validateEventForm_nullName_returnsEventNameRequired() {
        String result = CreateEventActivity.validateEventForm(null, "A description", "2025-06-01");
        assertEquals("Event name is required", result);
    }

    @Test
    public void validateEventForm_whitespaceOnlyName_returnsEventNameRequired() {
        String result = CreateEventActivity.validateEventForm("   ", "A description", "2025-06-01");
        assertEquals("Event name is required", result);
    }

    @Test
    public void validateEventForm_emptyDescription_returnsDescriptionRequired() {
        String result = CreateEventActivity.validateEventForm("Event Name", "", "2025-06-01");
        assertEquals("Description is required", result);
    }

    @Test
    public void validateEventForm_nullDescription_returnsDescriptionRequired() {
        String result = CreateEventActivity.validateEventForm("Event Name", null, "2025-06-01");
        assertEquals("Description is required", result);
    }

    @Test
    public void validateEventForm_emptyEventDate_returnsEventDateRequired() {
        String result = CreateEventActivity.validateEventForm("Event Name", "A description", "");
        assertEquals("Event date is required", result);
    }

    @Test
    public void validateEventForm_nullEventDate_returnsEventDateRequired() {
        String result = CreateEventActivity.validateEventForm("Event Name", "A description", null);
        assertEquals("Event date is required", result);
    }

    @Test
    public void validateEventForm_allValid_returnsNull() {
        String result = CreateEventActivity.validateEventForm("My Event", "Some description", "2025-06-15");
        assertNull(result);
    }

    @Test
    public void validateEventForm_trimmedWhitespace_valid() {
        String result = CreateEventActivity.validateEventForm("  Name  ", "  Desc  ", "  2025-01-01  ");
        assertNull(result);
    }
}
