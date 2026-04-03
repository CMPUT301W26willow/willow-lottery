package com.example.willow_lotto_app;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class RegistrationTest {


    @Test
    public void constructor_setsFieldsCorrectly() {
        Registration registration = new Registration(
                "event123",
                "user456",
                RegistrationStatus.WAITLISTED.getValue()
        );

        assertEquals("event123", registration.getEventId());
        assertEquals("user456", registration.getUserId());
        assertEquals("waitlisted", registration.getStatus());
        assertNotNull(registration.getCreatedAt());
        assertNotNull(registration.getUpdatedAt());
    }

    @Test
    public void getStatusEnum_mapsStatusCorrectly() {
        Registration registration = new Registration();
        registration.setStatus("invited");

        assertEquals(RegistrationStatus.INVITED, registration.getStatusEnum());
    }

    @Test
    public void getStatusEnum_unknownDefaultsToWaitlisted() {
        Registration registration = new Registration();
        registration.setStatus("something_weird");

        assertEquals(RegistrationStatus.WAITLISTED, registration.getStatusEnum());
    }

    @Test
    public void toMap_containsRequiredFields() {
        Registration registration = new Registration(
                "eventABC",
                "userXYZ",
                RegistrationStatus.ACCEPTED.getValue()
        );

        Map<String, Object> map = registration.toMap();

        assertEquals("eventABC", map.get("eventId"));
        assertEquals("userXYZ", map.get("userId"));
        assertEquals("accepted", map.get("status"));
        assertTrue(map.containsKey("createdAt"));
        assertTrue(map.containsKey("updatedAt"));
        assertTrue(map.get("createdAt") instanceof Timestamp);
        assertTrue(map.get("updatedAt") instanceof Timestamp);
    }

    @Test
    public void settersAndGetters_workCorrectly() {
        Registration registration = new Registration();

        registration.setId("reg1");
        registration.setEventId("event1");
        registration.setUserId("user1");
        registration.setStatus("declined");

        Timestamp created = Timestamp.now();
        Timestamp updated = Timestamp.now();

        registration.setCreatedAt(created);
        registration.setUpdatedAt(updated);

        assertEquals("reg1", registration.getId());
        assertEquals("event1", registration.getEventId());
        assertEquals("user1", registration.getUserId());
        assertEquals("declined", registration.getStatus());
        assertEquals(created, registration.getCreatedAt());
        assertEquals(updated, registration.getUpdatedAt());
    }
}
