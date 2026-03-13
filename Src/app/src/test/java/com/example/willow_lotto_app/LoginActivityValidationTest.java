package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link LoginActivity#validateLoginInput(String, String)}.
 * Covers basic email/password presence checks for the sign-in form.
 */
public class LoginActivityValidationTest {

    @Test
    public void validateLoginInput_missingEmail_returnsEmailError() {
        String result = LoginActivity.validateLoginInput(
                "", "secret");
        assertEquals("Email required", result);
    }

    @Test
    public void validateLoginInput_missingPassword_returnsPasswordError() {
        String result = LoginActivity.validateLoginInput(
                "user@example.com", "");
        assertEquals("Password required", result);
    }

    @Test
    public void validateLoginInput_allValid_returnsNull() {
        String result = LoginActivity.validateLoginInput(
                "user@example.com", "secret");
        assertNull(result);
    }
}

