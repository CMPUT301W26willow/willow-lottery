package com.example.willow_lotto_app.events;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.willow_lotto_app.notification.NotificationStore;
import com.example.willow_lotto_app.notification.UserNotification;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class PrivateEventInviteManagerTest {

    @Test
    public void matchesPrivateUserSearch_findsNameEmailPhone() {
        assertTrue(PrivateEventInviteManager.matchesPrivateUserSearch(
                "Alice Smith", "a@x.com", "555", "alice"));
        assertTrue(PrivateEventInviteManager.matchesPrivateUserSearch(
                "Bob", "bob@example.org", "", "EXAMPLE.ORG"));
        assertTrue(PrivateEventInviteManager.matchesPrivateUserSearch(
                "C", "", "+1 800 555 0199", "0199"));
    }

    @Test
    public void matchesPrivateUserSearch_noMatch() {
        assertFalse(PrivateEventInviteManager.matchesPrivateUserSearch(
                "Zed", "z@z.com", "111", "nomatch"));
    }

    @Test
    public void matchesPrivateUserSearch_emptyQuery_matchesAll() {
        assertTrue(PrivateEventInviteManager.matchesPrivateUserSearch("N", "e", "p", ""));
        assertTrue(PrivateEventInviteManager.matchesPrivateUserSearch("N", "e", "p", "   "));
        assertTrue(PrivateEventInviteManager.matchesPrivateUserSearch("N", "e", "p", null));
    }

    @Test
    public void inviteUserToPrivateEvent_createsRegistrationThenNotification() {
        RegistrationStore regStore = mock(RegistrationStore.class);
        NotificationStore notifStore = mock(NotificationStore.class);
        FirebaseFirestore db = mock(FirebaseFirestore.class);

        doAnswer(invocation -> {
            RegistrationStore.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(regStore).createPrivateInvitationRegistration(eq("ev1"), eq("user9"), any());

        doAnswer(invocation -> {
            NotificationStore.SimpleCallback cb = invocation.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(notifStore).sendNotificationToUser(eq("user9"), any(UserNotification.class), any());

        PrivateEventInviteManager mgr = new PrivateEventInviteManager(db, regStore, notifStore);
        AtomicReference<String> msg = new AtomicReference<>();
        mgr.inviteUserToPrivateEvent("ev1", "user9", new PrivateEventInviteManager.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                msg.set(message);
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError(e);
            }
        });

        verify(regStore).createPrivateInvitationRegistration(eq("ev1"), eq("user9"), any());
        verify(notifStore).sendNotificationToUser(eq("user9"), any(UserNotification.class), any());
        assertTrue(msg.get() != null && msg.get().toLowerCase().contains("invitation"));
    }
}
