package com.example.willow_lotto_app.registration;

/** Registration status values stored in Firestore. */

public enum RegistrationStatus {
    WAITLISTED("waitlisted"),
    INVITED("invited"),
    PRIVATE_INVITED("private_invited"),
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