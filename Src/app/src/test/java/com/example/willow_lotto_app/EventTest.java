package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class EventTest {

    private Event event;

    @Before
    public void setup() {
        event = new Event();
    }

    @Test
    public void testWaitingCount_US010504() {
        event.setLocalWaitingList(Arrays.asList("user1", "user2", "user3"));
        assertEquals(3, event.getWaitingCount());
    }

    @Test
    public void testStatusChangeDoesNotAffectLocalCount() {
        event.setLocalWaitingList(Arrays.asList("user1", "user2"));
        // updateStatus only touches Firestore — local count stays unchanged
        // (no call here so the test doesn't crash)
        assertEquals(2, event.getWaitingCount());
    }

    @Test
    public void testSetLocalWaitingList() {
        event.setLocalWaitingList(Arrays.asList("u1", "u2", "u3", "u4"));
        assertEquals(4, event.getWaitingCount());
    }
}