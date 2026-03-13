package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link CreateEventActivity#validateEventForm}.
 */
public class CreateEventActivityTest {

    @Test
    public void validateEventForm_emptyName_returnsNameError() {
        String result = CreateEventActivity.validateEventForm(
                "", "Desc", "2025-06-01");
        assertEquals("Event name is required", result);
    }

    @Test
    public void validateEventForm_emptyDescription_returnsDescriptionError() {
        String result = CreateEventActivity.validateEventForm(
                "Name", "", "2025-06-01");
        assertEquals("Description is required", result);
    }

    @Test
    public void validateEventForm_emptyDate_returnsDateError() {
        String result = CreateEventActivity.validateEventForm(
                "Name", "Desc", "");
        assertEquals("Event date is required", result);
    }

    @Test
    public void validateEventForm_allValid_returnsNull() {
        String result = CreateEventActivity.validateEventForm(
                "Name", "Desc", "2025-06-01");
        assertNull(result);
    }
}

