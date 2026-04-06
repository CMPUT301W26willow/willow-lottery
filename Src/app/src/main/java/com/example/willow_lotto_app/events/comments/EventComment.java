package com.example.willow_lotto_app.events.comments;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single top-level comment associated with a specific event.
 * <p>
 * These documents are stored in the Firestore hierarchy at:
 * {@code events/{eventId}/comments/{commentId}}.
 * </p>
 * @author Dev Tiwari
 */
public final class EventComment {

    /** The unique Firestore document ID for this comment. */
    private String documentId;

    /** The unique ID of the user who authored the comment. */
    private String authorId;

    /** The display name of the user who authored the comment. */
    private String authorName;

    /** The text content of the comment. */
    private String body;

    /** The timestamp indicating when the comment was created. */
    private Timestamp createdAt;

    /**
     * Empty string = top-level comment; non-empty = document id of parent comment (Option A).
     */
    private String parentCommentId;

    /** UID → emoji; one reaction per user. */
    private Map<String, String> reactionByUser;

    /** Stored on new top-level comments; legacy docs may omit this field. */
    public static final String TOP_LEVEL_PARENT_ID = "";

    /**
     * Sort order for reply lists: oldest first. Null timestamps sort before non-null.
     */
    public static Comparator<EventComment> createdTimeAscending() {
        return (a, b) -> {
            Timestamp ta = a.getCreatedAt();
            Timestamp tb = b.getCreatedAt();
            if (ta == null && tb == null) {
                return 0;
            }
            if (ta == null) {
                return -1;
            }
            if (tb == null) {
                return 1;
            }
            int cmp = Long.compare(ta.getSeconds(), tb.getSeconds());
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(ta.getNanoseconds(), tb.getNanoseconds());
        };
    }

    /**
     * Converts a Firestore {@link QueryDocumentSnapshot} into an {@code EventComment} object.
     *
     * @param doc The Firestore document snapshot to parse.
     * @return A populated {@link EventComment} instance.
     */
    public static EventComment fromSnapshot(QueryDocumentSnapshot doc) {
        EventComment c = new EventComment();
        c.documentId = doc.getId();
        c.authorId = doc.getString("authorId");
        c.authorName = doc.getString("authorName");
        c.body = doc.getString("body");
        c.createdAt = doc.getTimestamp("createdAt");
        c.parentCommentId = doc.getString("parentCommentId");
        c.reactionByUser = readReactionMap(doc.get("reactionByUser"));
        return c;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readReactionMap(Object raw) {
        if (!(raw instanceof Map)) {
            return Collections.emptyMap();
        }
        Map<?, ?> in = (Map<?, ?>) raw;
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<?, ?> e : in.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            out.put(e.getKey().toString(), e.getValue().toString());
        }
        return out.isEmpty() ? Collections.emptyMap() : out;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    /** True if this comment is shown in the main list (not a reply row). */
    public boolean isTopLevel() {
        return parentCommentId == null || parentCommentId.isEmpty();
    }

    /**
     * Gets the Firestore document ID.
     * * @return The unique ID of the comment document.
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Gets the author's unique user ID.
     * * @return The ID of the user who posted the comment.
     */
    public String getAuthorId() {
        return authorId;
    }

    /**
     * Gets the raw author name as stored in the database.
     * * @return The name of the author, which may be null or empty.
     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     * Gets the content of the comment.
     * * @return The message body.
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the time the comment was posted.
     * * @return A {@link Timestamp} representing the creation date.
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Map<String, String> getReactionByUser() {
        return reactionByUser != null ? reactionByUser : Collections.emptyMap();
    }

    /**
     * Resolves the name to display for the author.
     * <p>
     * If the {@link #authorName} is null or whitespace, it defaults to "User".
     * </p>
     * * @return A non-empty string suitable for UI display.
     */
    public String resolveDisplayName() {
        if (authorName != null && !authorName.trim().isEmpty()) {
            return authorName.trim();
        }
        return "User";
    }
}