package com.example.willow_lotto_app.notification;

/**
 * {@code type} values stored on notification documents in
 * {@code users/{uid}/notifications}. Keep in sync with {@link NotificationAdapter} styling.
 */
public final class NotificationTypes {

    public static final String LOTTERY_INVITED = "lottery_invited";
    public static final String LOTTERY_REPLACEMENT = "lottery_replacement";
    public static final String LOTTERY_CANCELLED = "lottery_cancelled";
    /** Waitlisted entrant was not selected in a lottery draw; written from {@link com.example.willow_lotto_app.organizer.OrganizerLotteryManager#drawLotteryForEvent}. */
    public static final String LOTTERY_NOT_CHOSEN = "lottery_not_chosen";
    /** Entrant joined (or accepted onto) the waiting list; written from {@code EventDetailActivity}. */
    public static final String WAITLIST_JOINED = "waitlist_joined";
    /** Organizer broadcast to everyone waitlisted (see {@link com.example.willow_lotto_app.organizer.OrganizerLotteryManager#notifyWaitlistedEntrants}). */
    public static final String WAITLIST_UPDATE = "waitlist_update";
    /** Primary organizer invited this user to co-organize; user must accept or decline in-app. */
    public static final String CO_ORGANIZER_INVITE = "co_organizer_invite";

    private NotificationTypes() {
    }
}
