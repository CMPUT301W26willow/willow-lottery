package com.example.willow_lotto_app.entrant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.example.willow_lotto_app.testutil.ImmediateTasks;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class EntrantResponseManagerTest {

    @Test
    public void declineInvitation_updatesStatusToDeclined() {
        RegistrationStore reg = mock(RegistrationStore.class);
        doAnswer(invocation -> {
            RegistrationStore.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(reg).updateRegistrationStatus(eq("reg1"), eq(RegistrationStatus.DECLINED.getValue()), any());

        EntrantResponseManager mgr = new EntrantResponseManager(mock(FirebaseFirestore.class), reg);
        AtomicReference<String> msg = new AtomicReference<>();
        mgr.declineInvitation("reg1", new EntrantResponseManager.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                msg.set(message);
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError(e);
            }
        });
        verify(reg).updateRegistrationStatus(eq("reg1"), eq(RegistrationStatus.DECLINED.getValue()), any());
        assertTrue(msg.get() != null && msg.get().toLowerCase().contains("declined"));
    }

    @Test
    public void acceptInvitation_updatesStatusThenRegisteredUsers() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        DocumentReference eventDoc = mock(DocumentReference.class);
        when(db.collection("events")).thenReturn(events);
        when(events.document("ev1")).thenReturn(eventDoc);
        Task<Void> updateDone = ImmediateTasks.forResult(null);
        when(eventDoc.update(anyString(), any())).thenReturn(updateDone);

        RegistrationStore reg = mock(RegistrationStore.class);
        doAnswer(invocation -> {
            RegistrationStore.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(reg).updateRegistrationStatus(eq("reg1"), eq(RegistrationStatus.ACCEPTED.getValue()), any());

        EntrantResponseManager mgr = new EntrantResponseManager(db, reg);
        AtomicReference<String> msg = new AtomicReference<>();
        mgr.acceptInvitation("reg1", "ev1", "userZ", new EntrantResponseManager.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                msg.set(message);
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError(e);
            }
        });
        verify(reg).updateRegistrationStatus(eq("reg1"), eq(RegistrationStatus.ACCEPTED.getValue()), any());
        verify(eventDoc).update(eq("registeredUsers"), any());
        assertEquals("Invitation accepted successfully.", msg.get());
    }

    @Test
    public void cancelAcceptedEntrant_updatesStatusAndRemovesRegisteredUser() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        DocumentReference eventDoc = mock(DocumentReference.class);
        when(db.collection("events")).thenReturn(events);
        when(events.document("ev1")).thenReturn(eventDoc);
        Task<Void> updateDone = ImmediateTasks.forResult(null);
        when(eventDoc.update(anyString(), any())).thenReturn(updateDone);

        RegistrationStore reg = mock(RegistrationStore.class);
        doAnswer(invocation -> {
            RegistrationStore.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(reg).updateRegistrationStatus(eq("reg1"), eq(RegistrationStatus.CANCELLED.getValue()), any());

        EntrantResponseManager mgr = new EntrantResponseManager(db, reg);
        AtomicReference<String> msg = new AtomicReference<>();
        mgr.cancelAcceptedEntrant("reg1", "ev1", "userZ", new EntrantResponseManager.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                msg.set(message);
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError(e);
            }
        });
        verify(eventDoc).update(eq("registeredUsers"), any());
        assertEquals("Entrant cancelled successfully.", msg.get());
    }
}
