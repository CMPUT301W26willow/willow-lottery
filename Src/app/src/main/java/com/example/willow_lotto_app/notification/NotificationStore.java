package com.example.willow_lotto_app.notification;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * NotificationStore handles writing notification documents to Firestore.
 *<p>
 * - Checks the user's notificationsEnabled preference before sending.
 * - If the user has opted out, the notification is silently skipped.
 * - If the user has opted in (or the field is absent), the notification
 *   is written to users/{userId}/notifications.
 *.
 */
public class NotificationStore {

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    private final FirebaseFirestore db;

    public NotificationStore() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Sends a notification to a user only if they have notifications enabled.
     * First checks users/{userId}/notificationsEnabled in Firestore.
     * If the field is absent, defaults to enabled so existing users
     * who have not set a preference still receive notifications.
     * if Notification successfully sent, will increment Statistics on the AdminDashboard
     */
    public void sendNotificationToUser(String userId, UserNotification notification, final SimpleCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");

                        // Send if field is absent (default on) or explicitly true
                        if (notificationsEnabled == null || notificationsEnabled) {
                            sendToFirestore(userId, notification, callback);

                            //incrementing Notification amount in adminDashboard
                            db.collection("statStore").document("appStats").update("notificationsSent", +1);
                        } else {
                            // User has opted out, skip silently and report success
                            callback.onSuccess();
                        }
                    } else {
                        // User document not found, skip silently
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Writes the notification document to users/{userId}/notifications.
     * Only called after confirming the user has notifications enabled.
     */
    private void sendToFirestore(String userId, UserNotification notification, final SimpleCallback callback) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(notification.toMap())
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}