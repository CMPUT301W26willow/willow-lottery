package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.willow_lotto_app.organizer.OrganizerLotteryManager;
import com.example.willow_lotto_app.organizer.ui.OrganizerDashboardActivity;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CancelledEntrantNotificationTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Mock
    OrganizerLotteryManager mockOrganizerLotteryManager;
    @Mock
    Context mockContext;
    @Mock
    OrganizerDashboardActivity mockOrganizerDashboardActivity;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    /**
     * Tests that a cancelled notification is sent to an entrant
     * when their registration is cancelled by the organizer
     */
    @Test
    public void notificationSentToCancelledEntrant() {
        mockOrganizerLotteryManager.replaceDeclinedOrCancelledEntrant(
                "event1",
                "registration1",
                RegistrationStatus.CANCELLED,
                new OrganizerLotteryManager.LotteryCallback() {
                    @Override
                    public void onSuccess(String message, List<Registration> affectedRegistrations) {
                        Toast.makeText(mockContext, message, Toast.LENGTH_SHORT).show();
                        mockOrganizerDashboardActivity.loadWaitingList();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("CancelledEntrantNotificationTest", "CancelledNotifEx1");
                    }
                });

        String expectedOut = "Notification Sent cancelled";
        String output = outContent.toString();
        assertEquals(expectedOut, output);
    }

    /**
     * Tests that a cancelled notification is NOT sent when status is DECLINED
     * DECLINED should trigger replacement draw not a cancelled notification
     */
    @Test
    public void notificationNotSentForDeclinedEntrant() {
        mockOrganizerLotteryManager.replaceDeclinedOrCancelledEntrant(
                "event1",
                "registration1",
                RegistrationStatus.DECLINED,
                new OrganizerLotteryManager.LotteryCallback() {
                    @Override
                    public void onSuccess(String message, List<Registration> affectedRegistrations) {
                        Toast.makeText(mockContext, message, Toast.LENGTH_SHORT).show();
                        mockOrganizerDashboardActivity.loadWaitingList();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("CancelledEntrantNotificationTest", "CancelledNotifEx2");
                    }
                });

        // declined goes straight to replacement draw, not a cancelled notification
        String expectedOut = "Notification Sent replacement";
        String output = outContent.toString();
        assertEquals(expectedOut, output);
    }

    /**
     * Tests that passing an invalid status does not send any notification
     * UNIMPLEMENTED IN ACTUAL CLASS
     */
    @Test
    public void notificationNotSentForInvalidStatus() {

    }
}