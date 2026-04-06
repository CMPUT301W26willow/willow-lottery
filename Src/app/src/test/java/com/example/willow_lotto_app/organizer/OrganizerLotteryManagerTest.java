package com.example.willow_lotto_app.organizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.willow_lotto_app.notification.NotificationStore;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.example.willow_lotto_app.testutil.ImmediateTasks;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class OrganizerLotteryManagerTest {

    @Test
    public void countFilledInvitationSlots_countsInvitedAndAcceptedOnly() {
        List<Registration> list = new ArrayList<>();
        Registration w = reg("w", RegistrationStatus.WAITLISTED);
        Registration i = reg("i", RegistrationStatus.INVITED);
        Registration a = reg("a", RegistrationStatus.ACCEPTED);
        Registration d = reg("d", RegistrationStatus.DECLINED);
        list.add(w);
        list.add(i);
        list.add(a);
        list.add(d);
        assertEquals(2, OrganizerLotteryManager.countFilledInvitationSlots(list));
    }

    @Test
    public void countFilledInvitationSlots_nullOrEmpty_isZero() {
        assertEquals(0, OrganizerLotteryManager.countFilledInvitationSlots(null));
        assertEquals(0, OrganizerLotteryManager.countFilledInvitationSlots(Collections.emptyList()));
    }

    @Test
    public void computeActualDrawCount_respectsAllCaps() {
        assertEquals(2, OrganizerLotteryManager.computeActualDrawCount(10, 2, 5));
        assertEquals(1, OrganizerLotteryManager.computeActualDrawCount(10, 5, 1));
        assertEquals(0, OrganizerLotteryManager.computeActualDrawCount(3, 0, 9));
    }

    @Test
    public void setDrawSize_nonPositive_failsWithoutFirestore() {
        RegistrationStore reg = mock(RegistrationStore.class);
        NotificationStore notif = mock(NotificationStore.class);
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        OrganizerLotteryManager mgr = new OrganizerLotteryManager(db, reg, notif);
        AtomicReference<Exception> err = new AtomicReference<>();
        mgr.setDrawSize("e1", 0, new OrganizerLotteryManager.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
            }

            @Override
            public void onFailure(Exception e) {
                err.set(e);
            }
        });
        assertTrue(err.get() instanceof IllegalArgumentException);
    }

    @Test
    public void drawLotteryForEvent_nonPositive_failsImmediately() {
        OrganizerLotteryManager mgr = new OrganizerLotteryManager(
                mock(FirebaseFirestore.class), mock(RegistrationStore.class), mock(NotificationStore.class));
        AtomicReference<Exception> err = new AtomicReference<>();
        mgr.drawLotteryForEvent("e1", 0, new OrganizerLotteryManager.LotteryCallback() {
            @Override
            public void onSuccess(String message, List<Registration> affectedRegistrations) {
            }

            @Override
            public void onFailure(Exception e) {
                err.set(e);
            }
        });
        assertTrue(err.get() instanceof IllegalArgumentException);
    }

    @Test
    public void replaceDeclinedOrCancelledEntrant_invalidStatus_fails() {
        OrganizerLotteryManager mgr = new OrganizerLotteryManager(
                mock(FirebaseFirestore.class), mock(RegistrationStore.class), mock(NotificationStore.class));
        AtomicReference<Exception> err = new AtomicReference<>();
        mgr.replaceDeclinedOrCancelledEntrant(
                "e1",
                "regX",
                RegistrationStatus.INVITED,
                new OrganizerLotteryManager.LotteryCallback() {
                    @Override
                    public void onSuccess(String message, List<Registration> affectedRegistrations) {
                    }

                    @Override
                    public void onFailure(Exception e) {
                        err.set(e);
                    }
                });
        assertTrue(err.get() instanceof IllegalArgumentException);
    }

    @Test
    public void promoteWaitlistedToInvited_nullRegistration_fails() {
        OrganizerLotteryManager mgr = new OrganizerLotteryManager(
                mock(FirebaseFirestore.class), mock(RegistrationStore.class), mock(NotificationStore.class));
        AtomicReference<Exception> err = new AtomicReference<>();
        mgr.promoteWaitlistedToInvited("e1", null, new OrganizerLotteryManager.LotteryCallback() {
            @Override
            public void onSuccess(String message, List<Registration> affectedRegistrations) {
            }

            @Override
            public void onFailure(Exception e) {
                err.set(e);
            }
        });
        assertTrue(err.get() instanceof IllegalArgumentException);
    }

    @Test
    public void drawLotteryForEvent_invitesUpToRemainingSpots() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        CollectionReference eventsCol = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(eventsCol.document(anyString())).thenReturn(eventRef);

        DocumentSnapshot eventSnap = mock(DocumentSnapshot.class);
        when(eventSnap.exists()).thenReturn(true);
        when(eventSnap.getLong("drawSize")).thenReturn(2L);
        when(eventSnap.getString("name")).thenReturn("Test Event");
        Task<DocumentSnapshot> eventGetTask = ImmediateTasks.forResult(eventSnap);
        when(eventRef.get()).thenReturn(eventGetTask);

        RegistrationStore regStore = mock(RegistrationStore.class);
        Registration invited = reg("r0", RegistrationStatus.INVITED);
        invited.setId("r0");

        Registration wait = new Registration("e1", "u9", RegistrationStatus.WAITLISTED.getValue());
        wait.setId("wait1");

        doAnswer(invocation -> {
            RegistrationStore.RegistrationListCallback cb = invocation.getArgument(1);
            cb.onSuccess(Collections.singletonList(invited));
            return null;
        }).when(regStore).getRegistrationsForEvent(eq("e1"), any());

        doAnswer(invocation -> {
            RegistrationStore.RegistrationListCallback cb = invocation.getArgument(2);
            cb.onSuccess(Collections.singletonList(wait));
            return null;
        }).when(regStore).getRegistrationsForEventByStatus(eq("e1"), eq("waitlisted"), any());

        doAnswer(invocation -> {
            RegistrationStore.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(regStore).updateManyStatuses(anyList(), eq("invited"), any());

        NotificationStore notif = mock(NotificationStore.class);
        doAnswer(invocation -> {
            NotificationStore.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(notif).sendNotificationToUser(anyString(), any(), any());

        OrganizerLotteryManager mgr = new OrganizerLotteryManager(db, regStore, notif);
        AtomicReference<String> msg = new AtomicReference<>();
        AtomicReference<List<Registration>> affected = new AtomicReference<>();
        mgr.drawLotteryForEvent("e1", 5, new OrganizerLotteryManager.LotteryCallback() {
            @Override
            public void onSuccess(String message, List<Registration> affectedRegistrations) {
                msg.set(message);
                affected.set(affectedRegistrations);
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError(e);
            }
        });

        verify(regStore).updateManyStatuses(
                argThat(ids -> ids != null && ids.size() == 1 && "wait1".equals(ids.get(0))),
                eq("invited"),
                any());
        assertNotNull(msg.get());
        assertEquals(1, affected.get().size());
        assertEquals("wait1", affected.get().get(0).getId());
    }

    private static Registration reg(String id, RegistrationStatus status) {
        Registration r = new Registration("e", "u", status.getValue());
        r.setId(id);
        return r;
    }
}
