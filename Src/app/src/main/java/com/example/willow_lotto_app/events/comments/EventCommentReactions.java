package com.example.willow_lotto_app.events.comments;

import java.util.Map;

/**
 * Emoji reactions on event comments. Stored as {@code reactionByUser}: map from user UID to emoji
 * (one reaction per user; tap the same emoji again to remove).
 */
public final class EventCommentReactions {

    public static final String THUMBS = "\uD83D\uDC4D";
    public static final String HEART = "\u2764\uFE0F";
    public static final String LAUGH = "\uD83D\uDE02";

    public static final String[] EMOJIS = {THUMBS, HEART, LAUGH};

    private EventCommentReactions() {
    }

    public static int countForEmoji(Map<String, String> reactionByUser, String emoji) {
        if (reactionByUser == null || reactionByUser.isEmpty() || emoji == null) {
            return 0;
        }
        int n = 0;
        for (String v : reactionByUser.values()) {
            if (emoji.equals(v)) {
                n++;
            }
        }
        return n;
    }

    /** Label like "👍 3" or plain "👍" when zero. */
    public static String label(Map<String, String> reactionByUser, String emoji) {
        int c = countForEmoji(reactionByUser, emoji);
        if (c <= 0) {
            return emoji;
        }
        return emoji + " " + c;
    }

    public static boolean userHasReacted(Map<String, String> reactionByUser, String uid, String emoji) {
        if (uid == null || emoji == null || reactionByUser == null) {
            return false;
        }
        return emoji.equals(reactionByUser.get(uid));
    }
}
