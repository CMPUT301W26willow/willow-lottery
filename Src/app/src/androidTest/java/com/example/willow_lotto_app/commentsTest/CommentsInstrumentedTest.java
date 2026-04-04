/*
 * CommentsInstrumentedTest.java
 *
 * Author: Dev Tiwari
 *
 * Instrumented intent tests for event detail (comments / replies entry).
 */
package com.example.willow_lotto_app.commentsTest;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.willow_lotto_app.events.EventDetailActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CommentsInstrumentedTest {

    private static void launchWithEventId(String eventId) {
        Intent intent = new Intent(getApplicationContext(), EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        try (ActivityScenario<EventDetailActivity> scenario = ActivityScenario.launch(intent)) {
            assertNotNull(scenario);
        }
    }

    @Test
    public void launch_withEventExtra_variants() {
        launchWithEventId("evt_willow_spring_gala_2026");
        launchWithEventId("  evt_intent_smoke_01  ");
    }
}
