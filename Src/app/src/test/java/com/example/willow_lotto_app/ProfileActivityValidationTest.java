package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link ProfileActivity#validateProfileInput}.
 */
public class ProfileActivityValidationTest {

    @Test
    public void validateProfileInput_missingName_returnsError() {
        String result = ProfileActivity.validateProfileInput(
                "", "user@example.com");
        assertEquals("Name and Email required", result);
    }

    @Test
    public void validateProfileInput_missingEmail_returnsError() {
        String result = ProfileActivity.validateProfileInput(
                "Alice", "");
        assertEquals("Name and Email required", result);
    }

    @Test
    public void validateProfileInput_valid_returnsNull() {
        String result = ProfileActivity.validateProfileInput(
                "Alice", "user@example.com");
        assertNull(result);
    }
}

