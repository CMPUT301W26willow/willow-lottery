package com.example.willow_lotto_app;

import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
/**
 * EntrantResponseManager.java
 *<p>
 * Handles entrant response actions after a lottery invitation has been issued.
 *<p>
 * Role in application:
 * - Service/manager layer for updating invitation outcomes.
 * - Changes registration status between invited, accepted, declined, and cancelled.
 * - Keeps the event's registeredUsers array synchronized for compatibility with the current event data model.
 *<p>
 * Outstanding issues:
 * - This manager assumes the caller already knows the correct registration ID.
 * - There is no built-in timeout or non-response handling yet.
 * - Cancellation and acceptance logic still depends on the event's registeredUsers array, which may later be replaced by fully registration-driven queries.
 */
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