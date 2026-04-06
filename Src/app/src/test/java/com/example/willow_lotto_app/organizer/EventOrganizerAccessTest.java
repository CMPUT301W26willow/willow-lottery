package com.example.willow_lotto_app.organizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class EventOrganizerAccessTest {

    @Test
    public void canManageEvent_owner_returnsTrue() {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.getString("organizerId")).thenReturn("ownerUid");
        when(doc.get("coOrganizerIds")).thenReturn(Collections.emptyList());
        assertTrue(EventOrganizerAccess.canManageEvent(doc, "ownerUid"));
    }

    @Test
    public void canManageEvent_coOrganizer_returnsTrue() {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.getString("organizerId")).thenReturn("ownerUid");
        when(doc.get("coOrganizerIds")).thenReturn(Arrays.asList("coA", "coB"));
        assertTrue(EventOrganizerAccess.canManageEvent(doc, "coA"));
        assertFalse(EventOrganizerAccess.canManageEvent(doc, "coC"));
    }

    @Test
    public void canManageEvent_pendingCoOrganizer_returnsFalse() {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.getString("organizerId")).thenReturn("ownerUid");
        when(doc.get("coOrganizerIds")).thenReturn(Collections.emptyList());
        when(doc.get("pendingCoOrganizerIds")).thenReturn(Collections.singletonList("pendingUid"));
        assertFalse(EventOrganizerAccess.canManageEvent(doc, "pendingUid"));
    }

    @Test
    public void canManageEvent_stranger_returnsFalse() {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.getString("organizerId")).thenReturn("ownerUid");
        when(doc.get("coOrganizerIds")).thenReturn(Collections.singletonList("coA"));
        assertFalse(EventOrganizerAccess.canManageEvent(doc, "hacker"));
    }

    @Test
    public void canManageEvent_nullOrMissingDoc_returnsFalse() {
        assertFalse(EventOrganizerAccess.canManageEvent(null, "u"));
        DocumentSnapshot missing = mock(DocumentSnapshot.class);
        when(missing.exists()).thenReturn(false);
        assertFalse(EventOrganizerAccess.canManageEvent(missing, "u"));
    }

    @Test
    public void canManageEvent_emptyUid_returnsFalse() {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.getString("organizerId")).thenReturn("owner");
        assertFalse(EventOrganizerAccess.canManageEvent(doc, ""));
        assertFalse(EventOrganizerAccess.canManageEvent(doc, null));
    }

    @Test
    public void readCoOrganizerIds_nonList_returnsEmpty() {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.get("coOrganizerIds")).thenReturn("not-a-list");
        assertTrue(EventOrganizerAccess.readCoOrganizerIds(doc).isEmpty());
    }

    @Test
    public void readPendingCoOrganizerIds_parsesStrings() {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.exists()).thenReturn(true);
        when(doc.get("pendingCoOrganizerIds")).thenReturn(Arrays.asList("p1", "p2"));
        assertEquals(Arrays.asList("p1", "p2"), EventOrganizerAccess.readPendingCoOrganizerIds(doc));
    }
}
