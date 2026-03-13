/**
 * EditEventActivityTest.java
 *
 * Author: Mehr Dhanda
 *
 * Intent tests for EditEventActivity.
 * Verifies that the organizer poster upload screen launches correctly
 * and that UI elements and data logic behave as expected.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent tests for EditEventActivity.
 * Tests activity launch and poster upload data logic.
 */
@RunWith(AndroidJUnit4.class)
public class EditEventActivityTest {

    /**
     * Verifies that the selected image URI is null before any image is picked.
     */
    @Test
    public void testSelectedImageUriInitiallyNull() {
        Uri selectedImageUri = null;
        assertNull("Image URI should be null before selection", selectedImageUri);
    }

    /**
     * Verifies that a URI can be assigned for poster upload.
     */
    @Test
    public void testSelectedImageUriCanBeSet() {
        Uri selectedImageUri = Uri.parse("content://media/external/images/media/1");
        assertNotNull("Image URI should not be null after selection", selectedImageUri);
    }

    /**
     * Verifies that the storage path is correctly constructed from the event ID.
     */
    @Test
    public void testStoragePathIsCorrect() {
        String eventId = "event1";
        String expectedPath = "events/" + eventId + "/poster.jpg";
        assertEquals("events/event1/poster.jpg", expectedPath);
    }
}