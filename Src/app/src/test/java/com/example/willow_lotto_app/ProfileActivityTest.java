package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.widget.Button;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Ignore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

/**
 * Basic UI-level tests for {@link ProfileActivity}.
 *
 * 
 * 
 * need to look into why the tests are not running on my machine and how to fix it
 * and build valid test cases tonight ....
 * 
 * These tests focus on wiring and validation that do not require real Firebase instances.
 */
@Ignore("Robolectric theme/resources not fully configured on this machine")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)

public class ProfileActivityTest {

    @Test
    public void onCreate_bottomNavProfileSelected() {
        ActivityController<ProfileActivity> controller =
                Robolectric.buildActivity(ProfileActivity.class);
        ProfileActivity activity = controller.get();
        // Use a built‑in AppCompat theme so Robolectric always has resources.
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        controller.create().start().resume();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        assertNotNull(bottomNav);
        assertEquals(R.id.nav_profile, bottomNav.getSelectedItemId());
    }

    @Test
    public void bottomNav_clickEvents_startsEventsActivity() {
        ActivityController<ProfileActivity> controller =
                Robolectric.buildActivity(ProfileActivity.class);
        ProfileActivity activity = controller.get();
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        controller.create().start().resume();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.getMenu().performIdentifierAction(R.id.nav_events, 0);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent started = shadowActivity.getNextStartedActivity();
        assertNotNull("EventsActivity should be started", started);
        assertEquals(EventsActivity.class.getName(),
                started.getComponent().getClassName());
    }

    @Test
    public void organizerDashboardButton_startsOrganizerDashboardActivity() {
        ActivityController<ProfileActivity> controller =
                Robolectric.buildActivity(ProfileActivity.class);
        ProfileActivity activity = controller.get();
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        controller.create().start().resume();

        Button organizerDashboardButton = activity.findViewById(R.id.organizerDashboardButton);
        assertNotNull(organizerDashboardButton);

        organizerDashboardButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent started = shadowActivity.getNextStartedActivity();
        assertNotNull("OrganizerDashboardActivity should be started", started);
        assertEquals(OrganizerDashboardActivity.class.getName(),
                started.getComponent().getClassName());
    }

    @Test
    public void saveProfile_emptyNameOrEmail_showsValidationToast() {
        ActivityController<ProfileActivity> controller =
                Robolectric.buildActivity(ProfileActivity.class);
        ProfileActivity activity = controller.get();
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        controller.create().start().resume();

        Button saveButton = activity.findViewById(R.id.saveButton);
        assertNotNull(saveButton);

        // Leave fields empty and click save
        saveButton.performClick();

        String latestToast = ShadowToast.getTextOfLatestToast();
        assertEquals("Name and Email required", latestToast);
    }

    @Test
    public void deleteButton_showsConfirmationDialog() {
        ActivityController<ProfileActivity> controller =
                Robolectric.buildActivity(ProfileActivity.class);
        ProfileActivity activity = controller.get();
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        controller.create().start().resume();

        Button deleteButton = activity.findViewById(R.id.deleteProfileButton);
        assertNotNull(deleteButton);

        deleteButton.performClick();

        android.app.AlertDialog dialog =
                (android.app.AlertDialog) ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("Delete confirmation dialog should be shown", dialog);
    }
}

