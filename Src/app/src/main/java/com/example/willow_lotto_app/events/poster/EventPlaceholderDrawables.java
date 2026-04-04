package com.example.willow_lotto_app.events.poster;

import androidx.annotation.DrawableRes;

import com.example.willow_lotto_app.R;

/**
 * Picks one of four Willow-themed default poster images when an event has no uploaded poster.
 * Choice is stable per {@code eventId} (hash-based) so list and detail match and rows don’t flicker on scroll.
 */
public final class EventPlaceholderDrawables {

    private static final int[] PLACEHOLDERS = {
            R.drawable.event_placeholder_1,
            R.drawable.event_placeholder_2,
            R.drawable.event_placeholder_3,
            R.drawable.event_placeholder_4,
    };

    private EventPlaceholderDrawables() {
    }

    @DrawableRes
    public static int forEventId(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return PLACEHOLDERS[0];
        }
        int idx = Math.floorMod(eventId.hashCode(), PLACEHOLDERS.length);
        return PLACEHOLDERS[idx];
    }
}
