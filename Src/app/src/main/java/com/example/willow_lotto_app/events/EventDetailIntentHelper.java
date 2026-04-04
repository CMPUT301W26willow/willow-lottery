package com.example.willow_lotto_app.events;

import android.content.Intent;
import android.net.Uri;

/**
 * Resolves the event id for {@link EventDetailActivity} from an {@link Intent}
 * (in-app extra or {@code willow-lottery://event/{id}} deep link).
 *
 * @author Dev Tiwari
 */
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
