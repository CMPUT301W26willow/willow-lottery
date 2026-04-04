package com.example.willow_lotto_app.events;

/**
 * Validation for posting event comments (used by {@link EventDetailActivity}).
 *
 * @author Dev Tiwari
 */
public final class EventCommentPostValidator {

    private EventCommentPostValidator() {
    }

    /** @return true if the user entered non-whitespace text */
    public static boolean hasNonEmptyBody(CharSequence text) {
        if (text == null) {
            return false;
        }
        return text.toString().trim().length() > 0;
    }
}
