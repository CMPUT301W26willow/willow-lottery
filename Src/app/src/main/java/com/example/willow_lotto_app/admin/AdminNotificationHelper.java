package com.example.willow_lotto_app.admin;

import com.example.willow_lotto_app.notification.NotificationStore;
import com.example.willow_lotto_app.notification.UserNotification;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for admin-generated notifications and moderation logs.
 *
 * Responsibilities:
 * - Sends in-app notifications to a user's notifications subcollection.
 * - Writes admin audit logs to a top-level admin_logs collection.
 */

public class AdminNotificationHelper {
    /**
     * Private constructor because this is a static helper class.
     */
    private AdminNotificationHelper() {
    }

    /**
     * Sends an in-app notification to a target user using the same notification
     * structure already used elsewhere in the project.
     *
     * @param targetUserId UID of the user receiving the notification
     * @param eventId related event ID, or null if not applicable
     * @param title short notification title
     * @param message full notification message
     * @param type notification type string
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
     * Writes an admin moderation log entry for auditing and debugging.
     *
     * @param adminEmail email of the admin performing the action
     * @param targetType type of entity being moderated, for example "event" or "profile"
     * @param targetId document ID of the entity being moderated
     * @param action action that was performed
     * @param details extra detail string for debugging
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
