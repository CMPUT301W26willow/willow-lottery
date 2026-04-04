package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link SignUpActivity#validateSignUpInput}.
 */
public class SignUpActivityTest {

    @Test
    public void validateSignUpInput_emptyName_returnsNameError() {
        String result = SignUpActivity.validateSignUpInput(
                "", "user@example.com", "secret");
        assertEquals("Name required", result);
    }

    @Test
    public void validateSignUpInput_emptyEmail_returnsEmailError() {
        String result = SignUpActivity.validateSignUpInput(
                "Alice", "", "secret");
        assertEquals("Email required", result);
    }

    @Test
    public void validateSignUpInput_emptyPassword_returnsPasswordError() {
        String result = SignUpActivity.validateSignUpInput(
                "Alice", "user@example.com", "");
        assertEquals("Password required", result);
    }

    @Test
    public void validateSignUpInput_shortPassword_returnsLengthError() {
        String result = SignUpActivity.validateSignUpInput(
                "Alice", "user@example.com", "12345");
        assertEquals("Password must be at least 6 characters", result);
    }

    @Test
    public void validateSignUpInput_allValid_returnsNull() {
        String result = SignUpActivity.validateSignUpInput(
                "Alice", "user@example.com", "secret");
        assertNull(result);
    }
}

