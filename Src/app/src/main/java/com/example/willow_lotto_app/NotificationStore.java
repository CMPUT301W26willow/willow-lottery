package com.example.willow_lotto_app;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * NotificationAdapter binds UserNotificationItem data to the RecyclerView rows shown in NotificationActivity.
 *
 * Role in application:
 * - Acts as the adapter layer between the notification model and the RecyclerView UI.
 * - Inflates item_notification.xml and fills each card with notification data.
 *
 * Current limitations / outstanding issues:
 * - Notifications are display-only for now.
 * - There is no click handling yet for opening related events.
 * - Dates are displayed using the default Date.toString() format and may be reformatted later.
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

    public void sendNotificationToUser(String userId, UserNotification notification, final SimpleCallback callback) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(notification.toMap())
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}