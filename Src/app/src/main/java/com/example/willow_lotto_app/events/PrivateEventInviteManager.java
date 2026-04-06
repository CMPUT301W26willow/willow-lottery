package com.example.willow_lotto_app.events;

import com.example.willow_lotto_app.notification.NotificationStore;
import com.example.willow_lotto_app.notification.UserNotification;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/** Search users and send private waitlist invites for an event. */
public class PrivateEventInviteManager {

    /**
     * Callback used when searching users for private event invitations.
     *
     * @author Jasdeep Cheema
     * @version 1.0
     * @since 31/03/2026
     */
    public interface UserSearchCallback {
        /**
         * Called when matching users are found successfully.
         *
         * @param users list of matching user documents
         */
        void onSuccess(List<DocumentSnapshot> users);

        /**
         * Called when the user search fails.
         *
         * @param e exception returned from the failed search
         */
        void onFailure(Exception e);
    }

    /**
     * Simple callback used for private invitation actions.
     *
     * @author Jasdeep Cheema
     * @version 1.0
     * @since 31/03/2026
     */
    public interface SimpleCallback {
        /**
         * Called when the action completes successfully.
         *
         * @param message success message for the caller
         */
        void onSuccess(String message);

        /**
         * Called when the action fails.
         *
         * @param e exception returned from the failed action
         */
        void onFailure(Exception e);
    }

    private final FirebaseFirestore db;
    private final RegistrationStore registrationStore;
    private final NotificationStore notificationStore;

    /**
     * Creates a new private event invitation manager and initializes
     * the Firebase and repository helpers used by this class.
     *
     * @since 31/03/2026
     */
    public PrivateEventInviteManager() {
        this(FirebaseFirestore.getInstance(), new RegistrationStore(), new NotificationStore());
    }

    /**
     * For tests: {@code searchUsers} still uses {@code db}; invite path uses injected stores only.
     */
    public PrivateEventInviteManager(
            FirebaseFirestore db,
            RegistrationStore registrationStore,
            NotificationStore notificationStore) {
        this.db = db;
        this.registrationStore = registrationStore;
        this.notificationStore = notificationStore;
    }

    /**
     * Same matching rules as {@link #searchUsers}; extracted for unit tests.
     */
    public static boolean matchesPrivateUserSearch(String name, String email, String phone, String query) {
        String lower = query == null ? "" : query.trim().toLowerCase();
        if (lower.isEmpty()) {
            return true;
        }
        String n = safeLowerField(name);
        String e = safeLowerField(email);
        String p = safeLowerField(phone);
        return n.contains(lower) || e.contains(lower) || p.contains(lower);
    }

    private static String safeLowerField(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    // US 2:01:03
    /**
     * Searches users by name, email, or phone number for private event invites.
     *
     * @param query search text entered by the organizer
     * @param callback callback used to return results or failure
     */
    public void searchUsers(String query, final UserSearchCallback callback) {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> matches = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (matchesPrivateUserSearch(
                                doc.getString("name"),
                                doc.getString("email"),
                                doc.getString("phone"),
                                query)) {
                            matches.add(doc);
                        }
                    }

                    callback.onSuccess(matches);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // US 01:05:06
    /**
     * Invites a specific user to a private event waiting list and sends
     * an in-app notification to that user.
     *
     * @param eventId the ID of the private event
     * @param userId the ID of the invited user
     * @param callback callback used to report success or failure
     */
    public void inviteUserToPrivateEvent(String eventId, String userId, final SimpleCallback callback) {
        registrationStore.createPrivateInvitationRegistration(
                eventId,
                userId,
                new RegistrationStore.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        UserNotification notification = new UserNotification(
                                eventId,
                                "Private Event Invitation",
                                "You have been invited to join the waiting list for a private event.",
                                "private_event_invite"
                        );

                        notificationStore.sendNotificationToUser(
                                userId,
                                notification,
                                new NotificationStore.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess("Private event invitation sent.");
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        callback.onFailure(e);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                }
        );
    }

}