package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.willow_lotto_app.organizer.ui.OrganizerDashboardActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented sanity check; detailed behavior is covered by JVM tests:
 * {@link com.example.willow_lotto_app.organizer.ui.OrganizerDashboardContractTest},
 * {@link com.example.willow_lotto_app.organizer.ui.OrganizerWaitlistAdapterTest},
 * {@link com.example.willow_lotto_app.organizer.EventOrganizerAccessTest}.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerDashboardActivityTest {

    @Test
    public void dashboardExtraKey_isStableAcrossBuilds() {
        assertEquals("event_id", OrganizerDashboardActivity.EXTRA_EVENT_ID);
    }
}
