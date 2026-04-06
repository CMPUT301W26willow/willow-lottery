package com.example.willow_lotto_app.events;

import android.content.Intent;
import android.net.Uri;

/** Reads event id from intent extras or willow-lottery deep links. */
public final class EventDetailIntentHelper {

    public static final String DEEP_LINK_SCHEME = "willow-lottery";
    public static final String DEEP_LINK_HOST_EVENT = "event";

    private EventDetailIntentHelper() {
    }

    /**
     * @return trimmed event id, or {@code null} if missing / invalid
     */
    public static String resolveEventId(Intent intent) {
        if (intent == null) {
            return null;
        }
        String eventId = intent.getStringExtra(EventDetailActivity.EXTRA_EVENT_ID);
        if (eventId != null && !eventId.trim().isEmpty()) {
            return eventId.trim();
        }
        Uri data = intent.getData();
        if (data != null
                && DEEP_LINK_SCHEME.equals(data.getScheme())
                && DEEP_LINK_HOST_EVENT.equals(data.getHost())
                && data.getPathSegments() != null
                && !data.getPathSegments().isEmpty()) {
            String fromLink = data.getPathSegments().get(0);
            return fromLink != null && !fromLink.isEmpty() ? fromLink : null;
        }
        return null;
    }
}
