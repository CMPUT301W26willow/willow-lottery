package com.example.willow_lotto_app;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void createWaitlistRegistration(String eventId, String userId, final SimpleCallback callback) {
        Registration registration = new Registration(
                eventId,
                userId,
                RegistrationStatus.WAITLISTED.getValue()
        );

        db.collection("registrations")
                .add(registration.toMap())
                .addOnSuccessListener(documentReference -> callback.onSuccess())
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
}