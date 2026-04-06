package com.example.willow_lotto_app.events.comments;

import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

/** Builds Firestore field maps for new comment documents. */
public final class EventCommentDocument {

    private EventCommentDocument() {
    }

    /**
     * @param replyingToTopLevelCommentId if non-null, document is a reply under that top-level comment id
     */
    public static Map<String, Object> newDraft(
            String authorId,
            String authorName,
            String body,
            String replyingToTopLevelCommentId) {
        Map<String, Object> comment = new HashMap<>();
        comment.put("authorId", authorId);
        String name = authorName;
        if (name == null || name.trim().isEmpty()) {
            name = "User";
        }
        comment.put("authorName", name.trim());
        comment.put("body", body);
        comment.put("createdAt", FieldValue.serverTimestamp());
        if (replyingToTopLevelCommentId != null) {
            comment.put("parentCommentId", replyingToTopLevelCommentId);
        } else {
            comment.put("parentCommentId", EventComment.TOP_LEVEL_PARENT_ID);
        }
        comment.put("reactionByUser", new HashMap<String, Object>());
        return comment;
    }
}
