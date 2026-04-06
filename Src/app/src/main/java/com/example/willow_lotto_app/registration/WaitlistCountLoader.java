package com.example.willow_lotto_app.registration;

import com.example.willow_lotto_app.events.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads waitlist counts and limits onto Event models from Firestore. */
public final class WaitlistCountLoader {

    private WaitlistCountLoader() {
    }

    /**
     * Sets {@link Event#setLimit(Integer)} from {@code waitlistLimit} or legacy {@code limit}.
     */
    public static void applyWaitlistLimitFromDoc(DocumentSnapshot doc, Event event) {
        if (doc == null || event == null) {
            return;
        }
        Long w = doc.getLong("waitlistLimit");
        if (w == null) {
            w = doc.getLong("limit");
        }
        if (w != null) {
            event.setLimit(w.intValue());
        }
    }

    /**
     * Queries {@code registrations} and sets {@link Event#setWaitlistedRegistrationCount(int)} on each event.
     */
    public static void loadWaitlistedCounts(FirebaseFirestore db, List<Event> events, Runnable onComplete) {
        if (events == null || events.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        Set<String> idSet = new HashSet<>();
        for (Event e : events) {
            if (e.getId() != null && !e.getId().isEmpty()) {
                idSet.add(e.getId());
            }
        }
        if (idSet.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        List<String> ids = new ArrayList<>(idSet);
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        String status = RegistrationStatus.WAITLISTED.getValue();
        for (int i = 0; i < ids.size(); i += 30) {
            int end = Math.min(i + 30, ids.size());
            List<String> chunk = new ArrayList<>(ids.subList(i, end));
            tasks.add(db.collection("registrations")
                    .whereEqualTo("status", status)
                    .whereIn("eventId", chunk)
                    .get());
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(finished -> {
            Map<String, Integer> counts = new HashMap<>();
            for (Task<QuerySnapshot> t : tasks) {
                if (!t.isSuccessful() || t.getResult() == null) {
                    continue;
                }
                for (DocumentSnapshot d : t.getResult().getDocuments()) {
                    String eid = d.getString("eventId");
                    if (eid != null) {
                        counts.merge(eid, 1, Integer::sum);
                    }
                }
            }
            for (Event e : events) {
                String id = e.getId();
                int c = id != null ? counts.getOrDefault(id, 0) : 0;
                e.setWaitlistedRegistrationCount(c);
            }
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}
