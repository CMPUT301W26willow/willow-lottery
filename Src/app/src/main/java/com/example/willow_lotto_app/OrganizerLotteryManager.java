package com.example.willow_lotto_app;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

    public OrganizerLotteryManager() {
        this.db = FirebaseFirestore.getInstance();
        this.registrationRepository = new RegistrationStore();
        this.notificationRepository = new NotificationStore();
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
                                                        sendInvitedNotifications(eventId, selected, callback);
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
                        drawReplacement(eventId, callback);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                }
        );
    }

    private void sendInvitedNotifications(String eventId, List<Registration> selected, final LotteryCallback callback) {
        if (selected.isEmpty()) {
            callback.onSuccess("No entrants were selected.", selected);
            return;
        }

        final int[] completed = {0};
        final int total = selected.size();

        for (Registration registration : selected) {
            UserNotification notification = new UserNotification(
                    eventId,
                    "You were selected!",
                    "You have been chosen to sign up for this event.",
                    "lottery_invited"
            );

            notificationRepository.sendNotificationToUser(
                    registration.getUserId(),
                    notification,
                    new NotificationStore.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            completed[0]++;
                            if (completed[0] == total) {
                                callback.onSuccess("Lottery draw completed successfully.", selected);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    }
            );
        }
    }

    private void sendReplacementNotification(String eventId, Registration replacement, final LotteryCallback callback) {
        UserNotification notification = new UserNotification(
                eventId,
                "You were selected as a replacement!",
                "A spot opened up and you have now been invited to sign up for this event.",
                "lottery_replacement"
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