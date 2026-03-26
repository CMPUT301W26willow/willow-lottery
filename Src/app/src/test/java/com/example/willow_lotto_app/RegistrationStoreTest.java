package com.example.willow_lotto_app;


import static org.junit.Assert.assertEquals;


import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;

import org.junit.Test;


import java.util.Map;


/**
 * Unit tests for {@link Registration} logic.
 * Tests data mapping and status strings (Pure Logic).
 */
public class RegistrationStoreTest {


    @Test
    public void registration_constructor_setsFieldsCorrectly() {
        Registration reg = new Registration("event123", "user456", "waiting");


        assertEquals("event123", reg.getEventId());
        assertEquals("user456", reg.getUserId());
        assertEquals("waiting", reg.getStatus());
    }


    @Test
    public void toMap_validInputs_returnsCorrectDataMap() {
        Registration reg = new Registration("event_abc", "user_xyz", "selected");
        Map<String, Object> map = reg.toMap();


        // Verifies the keys match what the Store sends to Firestore
        assertEquals("event_abc", map.get("eventId"));
        assertEquals("user_xyz", map.get("userId"));
        assertEquals("selected", map.get("status"));
    }




    @Test
    public void registrationStatus_enum_returnsCorrectStringValues() {
        // Change "waiting" to "waitlisted" to match your actual code
        assertEquals("waitlisted", RegistrationStatus.WAITLISTED.getValue());
    }
}

