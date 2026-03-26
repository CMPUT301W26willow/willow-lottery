package com.example.willow_lotto_app.registration;

/**
 * Enum representing the supported registration states used throughout the lottery workflow.
 *
 * Role in application:
 * - Standardizes status values stored in Firestore registration documents.
 * - Provides safe conversion between enum values and Firestore string values.
 *
 * Outstanding issues:
 * - Unknown or null values currently default to WAITLISTED, which is convenient
 *   for robustness but may hide malformed data during debugging.
 */

public enum RegistrationStatus {
    WAITLISTED("waitlisted"),
    INVITED("invited"),
    ACCEPTED("accepted"),
    DECLINED("declined"),
    CANCELLED("cancelled");

    private final String value;

    RegistrationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RegistrationStatus fromString(String value) {
        if (value == null) {
            return WAITLISTED;
        }

        for (RegistrationStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }

        return WAITLISTED;
    }
}