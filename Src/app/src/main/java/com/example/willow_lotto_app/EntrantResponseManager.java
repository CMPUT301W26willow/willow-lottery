package com.example.willow_lotto_app;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class EntrantResponseManager {

    public interface SimpleCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    private final FirebaseFirestore db;
    private final RegistrationStore registrationRepository;

    public EntrantResponseManager() {
        this.db = FirebaseFirestore.getInstance();
        this.registrationRepository = new RegistrationStore();
    }

    /**
     * EntrantMapActivity.java
     *
     * Displays a Google Map with markers showing where entrants joined the waiting list for a specific event.
     *
     * Role in application:
     * - Controller/View layer for organizer geolocation viewing.
     * - Reads entrant location data from Firestore and renders markers on a Google Map.
     *
     * Outstanding issues:
     * - The event ID is currently hardcoded as "event1" and should be passed by Intent.
     * - This file still reads from the older events/{eventId}/waitingList structure instead
     *   of the newer top-level registrations-based flow.
     * - Marker clustering is not implemented, so dense areas may become visually crowded.
     */

    public void acceptInvitation(String registrationId, String eventId, String userId, final SimpleCallback callback) {
        registrationRepository.updateRegistrationStatus(
                registrationId,
                RegistrationStatus.ACCEPTED.getValue(),
                new RegistrationStore.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        db.collection("events")
                                .document(eventId)
                                .update("registeredUsers", FieldValue.arrayUnion(userId))
                                .addOnSuccessListener(unused -> callback.onSuccess("Invitation accepted successfully."))
                                .addOnFailureListener(callback::onFailure);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                }
        );
    }

    /**
     * Entrant declines invitation.
     * Status changes INVITED -> DECLINED
     */
    public void declineInvitation(String registrationId, final SimpleCallback callback) {
        registrationRepository.updateRegistrationStatus(
                registrationId,
                RegistrationStatus.DECLINED.getValue(),
                new RegistrationStore.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess("Invitation declined successfully.");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                }
        );
    }

    /**
     * Organizer cancels a previously accepted entrant later.
     * Removes them from registeredUsers too.
     */
    public void cancelAcceptedEntrant(String registrationId, String eventId, String userId, final SimpleCallback callback) {
        registrationRepository.updateRegistrationStatus(
                registrationId,
                RegistrationStatus.CANCELLED.getValue(),
                new RegistrationStore.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        db.collection("events")
                                .document(eventId)
                                .update("registeredUsers", FieldValue.arrayRemove(userId))
                                .addOnSuccessListener(unused -> callback.onSuccess("Entrant cancelled successfully."))
                                .addOnFailureListener(callback::onFailure);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                }
        );
    }
}