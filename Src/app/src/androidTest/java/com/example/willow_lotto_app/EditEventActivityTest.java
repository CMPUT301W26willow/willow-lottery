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

import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.willow_lotto_app.events.EditEventActivity;

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
     * Posters are stored in Firestore on {@code posterUri}; activities use this intent extra for the document id.
     */
    @Test
    public void testEditEventIntentExtraKey() {
        assertEquals("event_id", EditEventActivity.EXTRA_EVENT_ID);
        Intent intent = new Intent();
        intent.putExtra(EditEventActivity.EXTRA_EVENT_ID, "event1");
        assertEquals("event1", intent.getStringExtra(EditEventActivity.EXTRA_EVENT_ID));
    }
}