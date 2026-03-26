package com.example.willow_lotto_app;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.willow_lotto_app.registration.Registration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

//@RunWith(MockitoJUnitRunner.class)
@RunWith(AndroidJUnit4.class)
public class NotificationSentTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;


    @Mock
    OrganizerLotteryManager mockOrganizerLotteryManager;
    @Mock
    Context mockContext;
    @Mock
    OrganizerDashboardActivity mockOrganizerDashboardActivity;


    @Before
    public void Setup(){
        MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams(){
        System.setOut(originalOut);
    }

    /**
     * Testing if notification for invite is sent when prompted
     */
    @Test
    public void NotificationSentInvite(){
        mockOrganizerLotteryManager.drawLotteryForEvent("event1", 1, new OrganizerLotteryManager.LotteryCallback() {
            @Override
            public void onSuccess(String message, List<Registration> affectedRegistrations) {
                Toast.makeText(mockContext, message, Toast.LENGTH_SHORT).show();
                mockOrganizerDashboardActivity.loadWaitingList();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("NotificationSentTest","DrawLottoEx1");
            }
        });
        String expectedOut = "Notification Sent";
        String output = outContent.toString();
        assertEquals(expectedOut,output);
    }

    /**
     * Testing if reselect notification is sent when prompted
     */
    @Test
    public void NotificationSentReselect(){
        mockOrganizerLotteryManager.drawReplacement("event1", new OrganizerLotteryManager.LotteryCallback() {
            @Override
            public void onSuccess(String message, List<Registration> affectedRegistrations) {
                Toast.makeText(mockContext, message, Toast.LENGTH_SHORT).show();
                mockOrganizerDashboardActivity.loadWaitingList();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("NotificationSentTest","DrawLottoEx2");
            }
        });
        String expectedOut = "Notification Sent replacement";
        String output = outContent.toString();
        assertEquals(expectedOut,output);
    }

    /**
     * Testing if the lottery fail Notification is sent when prompted
     * UNIMPLEMENTED IN ACTUAL CLASS
     */
    @Test
    public void NotficationSentUnselected(){

    }
}
