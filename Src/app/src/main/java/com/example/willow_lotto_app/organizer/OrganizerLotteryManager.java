package com.example.willow_lotto_app.organizer;

import com.example.willow_lotto_app.notification.NotificationStore;
import com.example.willow_lotto_app.notification.NotificationTypes;
import com.example.willow_lotto_app.notification.UserNotification;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * OrganizerLotteryManager.java
 *<p>
 * Implements the core organizer-side lottery logic for selecting entrants, drawing replacements, and sending notifications.
 *<p>
 * Role in application:
 * - Service/manager layer for organizer lottery operations.
 * - Reads event draw size and registration state from Firestore.
 * - Enforces capacity before inviting additional entrants.
 * - Sends invitation and replacement notifications through NotificationStore.
 *<p>
 * Outstanding issues:
 * - Random selection currently relies on in-memory shuffling and does not yet include audit logging or deterministic reproducibility.
 * - The manager depends on the registrations collection being populated with consistent status values.
 * - Selected/cancelled/final-enrolled list screens are still separate UI concerns and are not managed directly here.
 */

public class OrganizerLotteryManager {


    public interface LotteryCallback {
        void onSuccess(String message, List<Registration> affectedRegistrations);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    private final FirebaseFirestore db;
    private final RegistrationStore registrationRepository;
    private final NotificationStore notificationRepository;
    private static final String TAG = "OrganizerLotteryManager";

    public OrganizerLotteryManager() {
        this.db = FirebaseFirestore.getInstance();
        this.registrationRepository = new RegistrationStore();
        this.notificationRepository = new NotificationStore();
    }

    private interface EventNameCallback {
        void onSuccess(String eventName);

        void onFailure(Exception e);
    }

    private void getEventDisplayName(String eventId, EventNameCallback callback) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onSuccess("Event");
                        return;
                    }
                    String name = documentSnapshot.getString("name");
                    callback.onSuccess(name != null && !name.trim().isEmpty() ? name.trim() : "Event");
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * US 02.05.02
     * Save draw size on the event document.
     */
    public void setDrawSize(String eventId, int drawSize, final SimpleCallback callback) {
        if (drawSize <= 0) {
            callback.onFailure(new IllegalArgumentException("Draw size must be greater than 0."));
            return;
        }

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("drawSize", drawSize);
        updates.put("updatedAt", Timestamp.now());

        db.collection("events")
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess("Draw size saved successfully."))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * US 02.05.01 + US 02.05.02
     * Draw a specified number of users from WAITLISTED and mark them INVITED.
     * Then send each selected user a notification.
     */
    public void drawLotteryForEvent(String eventId, int numberToDraw, final LotteryCallback callback) {
        if (numberToDraw <= 0) {
            callback.onFailure(new IllegalArgumentException("Number to draw must be greater than 0."));
            return;
        }

        getEventDrawSize(eventId, new DrawSizeCallback() {
            @Override
            public void onSuccess(int savedDrawSize) {
                if (savedDrawSize <= 0) {
                    callback.onFailure(new Exception("Draw size must be set before running the lottery."));
                    return;
                }

                getFilledSpotCount(eventId, new FilledCountCallback() {
                    @Override
                    public void onSuccess(int filledCount) {
                        int remainingSpots = savedDrawSize - filledCount;

                        if (remainingSpots <= 0) {
                            callback.onFailure(new Exception("No spots remaining."));
                            return;
                        }

                        registrationRepository.getRegistrationsForEventByStatus(
                                eventId,
                                RegistrationStatus.WAITLISTED.getValue(),
                                new RegistrationStore.RegistrationListCallback() {
                                    @Override
                                    public void onSuccess(List<Registration> waitlistedRegistrations) {
                                        if (waitlistedRegistrations.isEmpty()) {
                                            callback.onFailure(new Exception("No waitlisted entrants available."));
                                            return;
                                        }

                                        Collections.shuffle(waitlistedRegistrations);

                                        int actualDrawCount = Math.min(
                                                Math.min(numberToDraw, remainingSpots),
                                                waitlistedRegistrations.size()
                                        );

                                        if (actualDrawCount <= 0) {
                                            callback.onFailure(new Exception("No spots remaining."));
                                            return;
                                        }

                                        List<Registration> selected = new ArrayList<>(
                                                waitlistedRegistrations.subList(0, actualDrawCount)
                                        );

                                        List<String> selectedIds = new ArrayList<>();
                                        for (Registration registration : selected) {
                                            selectedIds.add(registration.getId());
                                        }

                                        registrationRepository.updateManyStatuses(
                                                selectedIds,
                                                RegistrationStatus.INVITED.getValue(),
                                                new RegistrationStore.SimpleCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        sendInvitedNotificationsResilient(eventId, selected, callback);
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

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * US 02.05.03
     * Draw exactly one replacement from the WAITLISTED pool.
     */
    public void drawReplacement(String eventId, final LotteryCallback callback) {
        getEventDrawSize(eventId, new DrawSizeCallback() {
            @Override
            public void onSuccess(int savedDrawSize) {
                if (savedDrawSize <= 0) {
                    callback.onFailure(new Exception("Draw size must be set before drawing a replacement."));
                    return;
                }

                getFilledSpotCount(eventId, new FilledCountCallback() {
                    @Override
                    public void onSuccess(int filledCount) {
                        if (filledCount >= savedDrawSize) {
                            callback.onFailure(new Exception("No replacement needed. Event is already full."));
                            return;
                        }

                        registrationRepository.getRegistrationsForEventByStatus(
                                eventId,
                                RegistrationStatus.WAITLISTED.getValue(),
                                new RegistrationStore.RegistrationListCallback() {
                                    @Override
                                    public void onSuccess(List<Registration> waitlistedRegistrations) {
                                        if (waitlistedRegistrations.isEmpty()) {
                                            callback.onFailure(new Exception("No replacement entrants available."));
                                            return;
                                        }

                                        Collections.shuffle(waitlistedRegistrations);
                                        Registration selectedReplacement = waitlistedRegistrations.get(0);

                                        registrationRepository.updateRegistrationStatus(
                                                selectedReplacement.getId(),
                                                RegistrationStatus.INVITED.getValue(),
                                                new RegistrationStore.SimpleCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        sendReplacementNotification(eventId, selectedReplacement, callback);
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

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }


    /**
     * US 02.05.03
     * First mark the old user as declined/cancelled, then draw replacement.
     */
    public void replaceDeclinedOrCancelledEntrant(
            String eventId,
            String oldRegistrationId,
            RegistrationStatus removalStatus,
            final LotteryCallback callback
    ) {
        if (removalStatus != RegistrationStatus.DECLINED && removalStatus != RegistrationStatus.CANCELLED) {
            callback.onFailure(new IllegalArgumentException("Removal status must be DECLINED or CANCELLED."));
            return;
        }

        registrationRepository.updateRegistrationStatus(
                oldRegistrationId,
                removalStatus.getValue(),
                new RegistrationStore.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        // CHANGED: once the status update succeeds, the decline/cancel
                        // itself has already worked. Replacement drawing is now treated
                        // as a follow-up step instead of deciding whether the whole
                        // decline action was a failure.
                        if (removalStatus == RegistrationStatus.CANCELLED) {
                            sendCancelledNotification(eventId, oldRegistrationId, callback);
                        } else {
                            drawReplacement(eventId, new LotteryCallback() {
                                @Override
                                public void onSuccess(String message, List<Registration> affectedRegistrations) {
                                    // CHANGED: if replacement succeeds too, report both results together.
                                    callback.onSuccess("Invitation declined. " + message, affectedRegistrations);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    // CHANGED: do not report decline as failed just because
                                    // no replacement could be drawn.
                                    callback.onSuccess("Invitation declined.", new ArrayList<>());
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                }
        );
    }

    /**
     * Invites are already saved as INVITED; notifications are best-effort so a push failure
     * does not undo the lottery result.
     */
    private void sendInvitedNotificationsResilient(String eventId, List<Registration> selected, final LotteryCallback callback) {
        if (selected.isEmpty()) {
            callback.onSuccess("No entrants were selected.", selected);
            return;
        }

        getEventDisplayName(eventId, new EventNameCallback() {
            @Override
            public void onSuccess(String eventName) {
                String body = "Congratulations! You have been selected for " + eventName
                        + ". Open this event to accept or decline your invitation.";
                final int[] completed = {0};
                final boolean[] hadFailure = {false};
                final int total = selected.size();

                for (Registration registration : selected) {
                    UserNotification notification = new UserNotification(
                            eventId,
                            eventName,
                            body,
                            NotificationTypes.LOTTERY_INVITED
                    );

                    notificationRepository.sendNotificationToUser(
                            registration.getUserId(),
                            notification,
                            new NotificationStore.SimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    bump();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    hadFailure[0] = true;
                                    bump();
                                }

                                private void bump() {
                                    completed[0]++;
                                    if (completed[0] == total) {
                                        String msg = hadFailure[0]
                                                ? "Selections saved. Some notifications could not be sent."
                                                : "Lottery draw completed successfully.";
                                        callback.onSuccess(msg, selected);
                                    }
                                }
                            }
                    );
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Promotes one waitlisted entrant to INVITED (organizer pick) if a draw slot is open.
     */
    public void promoteWaitlistedToInvited(String eventId, Registration registration, final LotteryCallback callback) {
        if (registration == null || registration.getId() == null) {
            callback.onFailure(new IllegalArgumentException("Invalid registration."));
            return;
        }

        getEventDrawSize(eventId, new DrawSizeCallback() {
            @Override
            public void onSuccess(int savedDrawSize) {
                if (savedDrawSize <= 0) {
                    callback.onFailure(new Exception("Draw size must be set before inviting."));
                    return;
                }

                getFilledSpotCount(eventId, new FilledCountCallback() {
                    @Override
                    public void onSuccess(int filledCount) {
                        if (filledCount >= savedDrawSize) {
                            callback.onFailure(new Exception("No spots remaining."));
                            return;
                        }

                        registrationRepository.updateRegistrationStatus(
                                registration.getId(),
                                RegistrationStatus.INVITED.getValue(),
                                new RegistrationStore.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        List<Registration> one = new ArrayList<>();
                                        one.add(registration);
                                        sendInvitedNotificationsResilient(eventId, one, callback);
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
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private void sendReplacementNotification(String eventId, Registration replacement, final LotteryCallback callback) {
        getEventDisplayName(eventId, new EventNameCallback() {
            @Override
            public void onSuccess(String eventName) {
                String body = "A spot opened up for " + eventName
                        + ". You have been invited—open the event to accept or decline.";
                UserNotification notification = new UserNotification(
                        eventId,
                        eventName,
                        body,
                        NotificationTypes.LOTTERY_REPLACEMENT
                );

                notificationRepository.sendNotificationToUser(
                        replacement.getUserId(),
                        notification,
                        new NotificationStore.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                List<Registration> result = new ArrayList<>();
                                result.add(replacement);
                                callback.onSuccess("Replacement entrant selected successfully.", result);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                List<Registration> result = new ArrayList<>();
                                result.add(replacement);
                                callback.onSuccess(
                                        "Replacement saved; notification may not have been delivered.",
                                        result);
                            }
                        }
                );
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private void sendCancelledNotification(String eventId, String oldRegistrationId, final LotteryCallback callback) {
        /**
         * Send the entrant a cancelled notification
         */
        registrationRepository.getRegistrationsForEvent(eventId, new RegistrationStore.RegistrationListCallback() {
            @Override
            public void onSuccess(List<Registration> registrations) {
                // Search through the registrations to find a cancelled one
                // get the userID
                String userId = null;
                for (Registration r : registrations) {
                    if (r.getId().equals(oldRegistrationId)) {
                        userId = r.getUserId();
                        break;
                    }
                }

                //if user registration is not found skip the notification
                // continue to draw

                if (userId == null) {
                    drawReplacement(eventId, callback);
                    return;
                }

                final String finalUserId = userId;
                getEventDisplayName(eventId, new EventNameCallback() {
                    @Override
                    public void onSuccess(String eventName) {
                        UserNotification notification = new UserNotification(
                                eventId,
                                eventName,
                                "Unfortunately, your registration for " + eventName + " has been cancelled.",
                                NotificationTypes.LOTTERY_CANCELLED
                        );

                        notificationRepository.sendNotificationToUser(
                                finalUserId,
                                notification,
                                new NotificationStore.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        drawReplacement(eventId, callback);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        drawReplacement(eventId, callback);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onFailure(Exception e) {
                        drawReplacement(eventId, callback);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                drawReplacement(eventId, callback);
            }
        });
    }

    /**
     * Sends a notification to all entrants with WAITLISTED status for the event.
     * Mirrors the pattern used by sendInvitedNotifications() but targets
     * the waiting list instead of selected entrants.
     */
    public void notifyWaitlistedEntrants(String eventId, final LotteryCallback callback) {
        registrationRepository.getRegistrationsForEventByStatus(
                eventId,
                RegistrationStatus.WAITLISTED.getValue(),
                new RegistrationStore.RegistrationListCallback() {
                    @Override
                    public void onSuccess(List<Registration> waitlistedRegistrations) {
                        if (waitlistedRegistrations.isEmpty()) {
                            callback.onFailure(new Exception("No waitlisted entrants to notify."));
                            return;
                        }

                        getEventDisplayName(eventId, new EventNameCallback() {
                            @Override
                            public void onSuccess(String eventName) {
                                String body = "Update regarding " + eventName
                                        + ": you are on the waiting list. The organizer may send more news here.";
                                final int[] completed = {0};
                                final int total = waitlistedRegistrations.size();

                                final boolean[] hadFailure = {false};
                                for (Registration registration : waitlistedRegistrations) {
                                    UserNotification notification = new UserNotification(
                                            eventId,
                                            eventName,
                                            body,
                                            NotificationTypes.WAITLIST_UPDATE
                                    );

                                    notificationRepository.sendNotificationToUser(
                                            registration.getUserId(),
                                            notification,
                                            new NotificationStore.SimpleCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    bump();
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    hadFailure[0] = true;
                                                    bump();
                                                }

                                                private void bump() {
                                                    completed[0]++;
                                                    if (completed[0] == total) {
                                                        String msg = hadFailure[0]
                                                                ? "Update sent to some entrants; others may not have been notified."
                                                                : "Notified " + total + " waitlisted entrant(s).";
                                                        callback.onSuccess(msg, waitlistedRegistrations);
                                                    }
                                                }
                                            }
                                    );
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(e);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                }
        );
    }

    private void getEventDrawSize(String eventId, final DrawSizeCallback callback) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onFailure(new Exception("Event not found."));
                        return;
                    }

                    Long drawSizeLong = documentSnapshot.getLong("drawSize");
                    int drawSize = drawSizeLong == null ? 0 : drawSizeLong.intValue();
                    callback.onSuccess(drawSize);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void getFilledSpotCount(String eventId, final FilledCountCallback callback) {
        registrationRepository.getRegistrationsForEvent(eventId, new RegistrationStore.RegistrationListCallback() {
            @Override
            public void onSuccess(List<Registration> registrations) {
                int filled = 0;

                for (Registration registration : registrations) {
                    RegistrationStatus status = registration.getStatusEnum();
                    if (status == RegistrationStatus.INVITED || status == RegistrationStatus.ACCEPTED) {
                        filled++;
                    }
                }

                callback.onSuccess(filled);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private interface DrawSizeCallback {
        void onSuccess(int drawSize);
        void onFailure(Exception e);
    }

    private interface FilledCountCallback {
        void onSuccess(int filledCount);
        void onFailure(Exception e);
    }
}