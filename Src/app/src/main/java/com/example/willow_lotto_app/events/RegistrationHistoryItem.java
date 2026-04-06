package com.example.willow_lotto_app.events;

import androidx.annotation.NonNull;

/** One row in the Events tab registration history list. */
public class RegistrationHistoryItem {

    public enum Outcome {
        NONE,
        POSITIVE,
        NEGATIVE
    }

    private final String registrationId;
    private final String eventId;
    private final String eventTitle;
    private final String eventDateLine;
    private final String registeredDateLine;
    private final int badgeBackgroundRes;
    private final int badgeTextColorRes;
    @NonNull
    private final String badgeLabel;
    @NonNull
    private final Outcome outcome;
    private final int outcomeMessageRes;

    public RegistrationHistoryItem(
            String registrationId,
            String eventId,
            String eventTitle,
            String eventDateLine,
            String registeredDateLine,
            int badgeBackgroundRes,
            int badgeTextColorRes,
            @NonNull String badgeLabel,
            @NonNull Outcome outcome,
            int outcomeMessageRes) {
        this.registrationId = registrationId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventDateLine = eventDateLine;
        this.registeredDateLine = registeredDateLine;
        this.badgeBackgroundRes = badgeBackgroundRes;
        this.badgeTextColorRes = badgeTextColorRes;
        this.badgeLabel = badgeLabel;
        this.outcome = outcome;
        this.outcomeMessageRes = outcomeMessageRes;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getEventId() {
        return eventId;
    }

    @NonNull
    public String getEventTitle() {
        return eventTitle;
    }

    @NonNull
    public String getEventDateLine() {
        return eventDateLine;
    }

    @NonNull
    public String getRegisteredDateLine() {
        return registeredDateLine;
    }

    public int getBadgeBackgroundRes() {
        return badgeBackgroundRes;
    }

    public int getBadgeTextColorRes() {
        return badgeTextColorRes;
    }

    @NonNull
    public String getBadgeLabel() {
        return badgeLabel;
    }

    @NonNull
    public Outcome getOutcome() {
        return outcome;
    }

    public int getOutcomeMessageRes() {
        return outcomeMessageRes;
    }
}
