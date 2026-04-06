package com.example.willow_lotto_app.registration;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RegistrationStore.java
 *<p>
 * Helper for reading and updating event registration documents
 * in the top-level Firestore "registrations" collection.
 *<p>
 * Role in application:
 * - Repository/data-access layer for registration records.
 * - Creates waitlist registrations.
 * - Queries registrations by event and status.
 * - Updates registration statuses individually or in batches.
 *<p>
 * Outstanding issues:
 * - This store does not yet enforce uniqueness for one user per event unless the
 *   calling code checks for existing registrations first.
 * - There is no delete method yet for removing registration documents entirely.
 * - Firestore query assumptions depend on all join flows writing status consistently.
 */

public class RegistrationStore {

    public interface RegistrationListCallback {
        void onSuccess(List<Registration> registrations);
        void onFailure(Exception e);
    }

    public interface RegistrationCallback {
        void onSuccess(Registration registration);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    private final FirebaseFirestore db;

    public RegistrationStore() {
        this.db = FirebaseFirestore.getInstance();
    }


    // CHANGED: use a stable registration document id so the app can
    // always find the same registration later.
    public void createWaitlistRegistration(String eventId, String userId, final SimpleCallback callback) {
        String docId = eventId + "_" + userId;

        Registration registration = new Registration(
                eventId,
                userId,
                RegistrationStatus.WAITLISTED.getValue()
        );

        db.collection("registrations")
                .document(docId)
                .set(registration.toMap())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public void getRegistrationsForEvent(String eventId, final RegistrationListCallback callback) {
        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Registration> results = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        results.add(Registration.fromDocument(doc));
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getRegistrationsForEventByStatus(String eventId, String status, final RegistrationListCallback callback) {
        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Registration> results = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        results.add(Registration.fromDocument(doc));
                    }
                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void findRegistration(String eventId, String userId, final RegistrationCallback callback) {
        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onFailure(new Exception("Registration not found."));
                        return;
                    }

                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                    callback.onSuccess(Registration.fromDocument(doc));
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateRegistrationStatus(String registrationId, String newStatus, final SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("updatedAt", Timestamp.now());

        db.collection("registrations")
                .document(registrationId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public void updateManyStatuses(List<String> registrationIds, String newStatus, final SimpleCallback callback) {
        WriteBatch batch = db.batch();

        for (String id : registrationIds) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", newStatus);
            updates.put("updatedAt", Timestamp.now());

            batch.update(db.collection("registrations").document(id), updates);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Removes a registration document (e.g. organizer rejects someone on the waitlist).
     */
    public void deleteRegistration(String registrationId, final SimpleCallback callback) {
        if (registrationId == null || registrationId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Missing registration id."));
            return;
        }
        db.collection("registrations")
                .document(registrationId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // US 2:01:03
    // CHANGED: creates a private invitation registration instead of a normal waitlist join
    /**
     * Creates a private-event invitation registration for a specific user.
     *
     * @param eventId the private event ID
     * @param userId the invited user ID
     * @param callback callback used to report success or failure
     * @since 31/03/2026
     */
    public void createPrivateInvitationRegistration(String eventId, String userId, final SimpleCallback callback) {
        String docId = eventId + "_" + userId;

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("userId", userId);
        data.put("status", RegistrationStatus.PRIVATE_INVITED.getValue());
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());

        db.collection("registrations")
                .document(docId)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}