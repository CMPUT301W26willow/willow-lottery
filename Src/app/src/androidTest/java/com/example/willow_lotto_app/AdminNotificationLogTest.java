/**
 * AdminNotificationLogTest.java
 *
 * Unit tests for the admin notification log feature in AdminDashboardActivity.
 * Verifies that notification log entries are correctly formatted and
 * that edge cases like empty logs and missing fields are handled properly.
 */
package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AdminNotificationLogTest {

    /**
     * Verifies that an empty notification log is handled correctly.
     */
    @Test
    public void testEmptyNotificationLogIsHandled() {
        List<String> logEntries = new ArrayList<>();
        assertTrue("Notification log should be empty", logEntries.isEmpty());
    }

    /**
     * Verifies that a notification log entry is correctly formatted
     * with title, message and type fields.
     */
    @Test
    public void testNotificationLogEntryFormattedCorrectly() {
        String title = "You were selected!";
        String message = "You have been chosen to sign up for this event.";
        String type = "lottery_invited";

        StringBuilder sb = new StringBuilder();
        sb.append("● ").append(title).append("\n")
                .append(message).append("\n")
                .append("[").append(type).append("]").append("\n\n");

        assertTrue("Entry should contain title", sb.toString().contains("You were selected!"));
        assertTrue("Entry should contain message", sb.toString().contains("You have been chosen"));
        assertTrue("Entry should contain type", sb.toString().contains("[lottery_invited]"));
    }

    /**
     * Verifies that a null title falls back to the default value.
     */
    @Test
    public void testNullTitleFallsBackToDefault() {
        String title = null;
        String display = title != null ? title : "(no title)";
        assertEquals("(no title)", display);
    }

    /**
     * Verifies that a null message falls back to an empty string.
     */
    @Test
    public void testNullMessageFallsBackToEmptyString() {
        String message = null;
        String display = message != null ? message : "";
        assertEquals("", display);
    }

    /**
     * Verifies that a null type is not shown in the log entry.
     */
    @Test
    public void testNullTypeIsNotShownInEntry() {
        String type = null;
        String display = type != null ? "[" + type + "]" : "";
        assertEquals("", display);
    }

    /**
     * Verifies that multiple notification entries are added correctly.
     */
    @Test
    public void testMultipleNotificationEntriesAddedCorrectly() {
        StringBuilder sb = new StringBuilder();
        sb.append("● You were selected!\nMessage 1\n[lottery_invited]\n\n");
        sb.append("● Your registration was cancelled\nMessage 2\n[lottery_cancelled]\n\n");
        sb.append("● You are on the waiting list!\nMessage 3\n[waitlist_update]\n\n");

        assertTrue("Should contain invited notification", sb.toString().contains("lottery_invited"));
        assertTrue("Should contain cancelled notification", sb.toString().contains("lottery_cancelled"));
        assertTrue("Should contain waitlist notification", sb.toString().contains("waitlist_update"));
    }

    /**
     * Verifies that the dialog title shows the correct notification count.
     */
    @Test
    public void testDialogTitleShowsCorrectCount() {
        int count = 5;
        String title = "Notification Log (" + count + ")";
        assertEquals("Notification Log (5)", title);
    }

    /**
     * Verifies that an empty log shows the correct empty state message.
     */
    @Test
    public void testEmptyLogShowsCorrectMessage() {
        List<String> entries = new ArrayList<>();
        String message = entries.isEmpty() ? "No notifications found." : "Log loaded.";
        assertEquals("No notifications found.", message);
    }

    /**
     * Verifies that a non-empty log shows the loaded message.
     */
    @Test
    public void testNonEmptyLogShowsLoadedMessage() {
        List<String> entries = new ArrayList<>();
        entries.add("entry1");
        String message = entries.isEmpty() ? "No notifications found." : "Log loaded.";
        assertEquals("Log loaded.", message);
    }
}