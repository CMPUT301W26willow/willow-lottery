package com.example.willow_lotto_app;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RegistrationStatusTest {




    @Test
    public void fromString_waitlisted_returnsWaitlisted() {
        assertEquals(RegistrationStatus.WAITLISTED,
                RegistrationStatus.fromString("waitlisted"));
    }

    @Test
    public void fromString_invitedUppercase_returnsInvited() {
        assertEquals(RegistrationStatus.INVITED,
                RegistrationStatus.fromString("INVITED"));
    }

    @Test
    public void fromString_null_defaultsToWaitlisted() {
        assertEquals(RegistrationStatus.WAITLISTED,
                RegistrationStatus.fromString(null));
    }

    @Test
    public void fromString_unknown_defaultsToWaitlisted() {
        assertEquals(RegistrationStatus.WAITLISTED,
                RegistrationStatus.fromString("unknown_status"));
    }

    @Test
    public void getValue_returnsExpectedFirestoreString() {
        assertEquals("waitlisted", RegistrationStatus.WAITLISTED.getValue());
        assertEquals("invited", RegistrationStatus.INVITED.getValue());
        assertEquals("accepted", RegistrationStatus.ACCEPTED.getValue());
        assertEquals("declined", RegistrationStatus.DECLINED.getValue());
        assertEquals("cancelled", RegistrationStatus.CANCELLED.getValue());
    }
}
