package com.example.willow_lotto_app;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class UserNotificationTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        UserNotification notification = new UserNotification(
                "event123",
                "You were selected!",
                "You have been chosen to sign up for this event.",
                "lottery_invited"
        );

        assertEquals("event123", notification.getEventId());
        assertEquals("You were selected!", notification.getTitle());
        assertEquals("You have been chosen to sign up for this event.", notification.getMessage());
        assertEquals("lottery_invited", notification.getType());
        assertFalse(notification.isRead());
        assertNotNull(notification.getCreatedAt());
    }

    @Test
    public void toMap_containsRequiredFields() {
        UserNotification notification = new UserNotification(
                "eventABC",
                "Replacement",
                "You were selected as a replacement.",
                "lottery_replacement"
        );

        Map<String, Object> map = notification.toMap();

        assertEquals("eventABC", map.get("eventId"));
        assertEquals("Replacement", map.get("title"));
        assertEquals("You were selected as a replacement.", map.get("message"));
        assertEquals("lottery_replacement", map.get("type"));
        assertEquals(false, map.get("read"));
        assertTrue(map.containsKey("createdAt"));
        assertTrue(map.get("createdAt") instanceof Timestamp);
    }

    @Test
    public void emptyConstructor_createsObject() {
        UserNotification notification = new UserNotification();
        assertNotNull(notification);
    }
}
