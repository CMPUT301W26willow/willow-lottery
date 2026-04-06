package com.example.willow_lotto_app.organizer.ui;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Stable contract for launching the organizer dashboard from profile / my events.
 * Full screen still depends on Firestore + auth; logic is covered in {@link OrganizerWaitlistAdapterTest}
 * and {@link com.example.willow_lotto_app.organizer.EventOrganizerAccessTest}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class OrganizerDashboardContractTest {

    @Test
    public void extraEventId_keyMatchesCallSites() {
        assertEquals("event_id", OrganizerDashboardActivity.EXTRA_EVENT_ID);
    }

    @Test
    public void launchIntent_carriesEventIdExtra() {
        Intent i = new Intent(RuntimeEnvironment.getApplication(), OrganizerDashboardActivity.class);
        i.putExtra(OrganizerDashboardActivity.EXTRA_EVENT_ID, "evt_42");
        assertEquals("evt_42", i.getStringExtra(OrganizerDashboardActivity.EXTRA_EVENT_ID));
    }
}
