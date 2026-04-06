package com.example.willow_lotto_app.organizer;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/** Tells if a user is the organizer or a co-organizer for an event. */
public final class EventOrganizerAccess {

    private EventOrganizerAccess() {
    }

    @SuppressWarnings("unchecked")
    public static List<String> readCoOrganizerIds(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return new ArrayList<>();
        }
        Object raw = doc.get("coOrganizerIds");
        if (!(raw instanceof List)) {
            return new ArrayList<>();
        }
        List<?> list = (List<?>) raw;
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o != null) {
                out.add(o.toString());
            }
        }
        return out;
    }

    /**
     * UIDs invited to co-organize who have not accepted yet ({@code pendingCoOrganizerIds} on the event).
     */
    @SuppressWarnings("unchecked")
    public static List<String> readPendingCoOrganizerIds(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return new ArrayList<>();
        }
        Object raw = doc.get("pendingCoOrganizerIds");
        if (!(raw instanceof List)) {
            return new ArrayList<>();
        }
        List<?> list = (List<?>) raw;
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o != null) {
                out.add(o.toString());
            }
        }
        return out;
    }

    public static boolean canManageEvent(DocumentSnapshot eventDoc, String uid) {
        if (eventDoc == null || !eventDoc.exists() || uid == null || uid.isEmpty()) {
            return false;
        }
        String organizerId = eventDoc.getString("organizerId");
        if (uid.equals(organizerId)) {
            return true;
        }
        return readCoOrganizerIds(eventDoc).contains(uid);
    }
}
