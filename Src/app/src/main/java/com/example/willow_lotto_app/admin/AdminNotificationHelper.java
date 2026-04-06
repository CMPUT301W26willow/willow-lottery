package com.example.willow_lotto_app.admin;

import com.example.willow_lotto_app.notification.NotificationStore;
import com.example.willow_lotto_app.notification.UserNotification;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/** Sends in-app notifications and writes admin_logs audit rows. */
public class AdminNotificationHelper {

    private AdminNotificationHelper() {
    }

    /**
     * Sends an in-app notification to the user’s notifications subcollection.
     *
     * @param targetUserId Firebase Auth uid of the recipient; no-op if null or blank
     * @param eventId      related event id, or null if not tied to an event
     * @param title        notification title
     * @param message      notification body
     * @param type         app-specific type string for filtering or display
     */
    public static void sendAdminNotification(
            String targetUserId,
            String eventId,
            String title,
            String message,
            String type
    ) {
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            return;
        }

        NotificationStore store = new NotificationStore();
        UserNotification notification = new UserNotification(eventId, title, message, type);

        store.sendNotificationToUser(targetUserId, notification, new NotificationStore.SimpleCallback() {
            @Override
            public void onSuccess() {
                // No UI callback needed here.
            }

            @Override
            public void onFailure(Exception e) {
                // Silent failure for now to avoid crashing moderation flows.
            }
        });
    }

    /**
     * Appends a moderation audit row to the {@code admin_logs} collection.
     *
     * @param adminEmail email of the signed-in admin (may be null)
     * @param targetType moderated entity kind, e.g. {@code "event"} or {@code "profile"}
     * @param targetId   Firestore document id of the moderated entity
     * @param action     short action label, e.g. {@code "delete_event"}
     * @param details    human-readable detail for support and debugging
     */
    public static void logAdminAction(
            String adminEmail,
            String targetType,
            String targetId,
            String action,
            String details
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> log = new HashMap<>();
        log.put("adminEmail", adminEmail);
        log.put("targetType", targetType);
        log.put("targetId", targetId);
        log.put("action", action);
        log.put("details", details);
        log.put("createdAt", FieldValue.serverTimestamp());

        db.collection("admin_logs").add(log);
    }
}
