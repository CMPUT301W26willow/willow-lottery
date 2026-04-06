package com.example.willow_lotto_app.entrant;

import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

/** Updates registration status when entrants accept, decline, or are cancelled after invites. */
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