package com.example.willow_lotto_app.events;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single UI thread for event-related comments.
 * <p>
 * This class follows a "Lazy-Loaded" pattern, where a single
 * top-level comment is initially displayed, and its associated replies
 * are fetched and populated only when the user expands the thread.
 */
public final class EventCommentThread {

    /** The primary parent comment that anchors this thread. */
    private EventComment top;

    /** * Indicates if the UI should currently display the reply list.
     * Managed by user interaction (ex. clicking 'View Replies').
     */
    private boolean expanded;

    /** * State flag to trigger or track an active background fetch
     * for the {@link #replies} list.
     */
    private boolean loadingReplies;

    /** * The collection of child comments. This list is typically empty
     * until {@code loadingReplies} completes successfully.
     */
    private final List<EventComment> replies = new ArrayList<>();

    /**
     * Initializes a new thread anchored by a top-level comment.
     * @param top The root {@link EventComment} for this thread; must not be null.
     */
    public EventCommentThread(EventComment top) {
        this.top = top;
    }

    /**
     * @return The root comment of this specific thread.
     */
    public EventComment getTop() {
        return top;
    }

    /**
     * Updates the root comment. Useful if the comment data is refreshed
     * from the server (ex. updated like counts or text edits).
     * @param top The updated comment object.
     */
    public void setTop(EventComment top) {
        this.top = top;
    }

    /**
     * @return true if the reply section is currently toggled open in the UI.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Toggles the visibility of the replies.
     *
     * @param expanded Set to true to show replies, false to collapse them.
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * @return true if a network request is currently fetching the reply data.
     */
    public boolean isLoadingReplies() {
        return loadingReplies;
    }

    /**
     * Updates the loading state. Should be set to {@code true} before starting
     * a fetch and {@code false} once the {@link #replies} list is populated.
     *
     * @param loadingReplies The new loading state.
     */
    public void setLoadingReplies(boolean loadingReplies) {
        this.loadingReplies = loadingReplies;
    }

    /**
     * Returns the list of child comments.
     *
     * Note: This list may be empty if {@link #expanded} has never been true
     * or if the top-level comment has no replies.
     *
     * @return A mutable list of {@link EventComment} replies.
     */
    public List<EventComment> getReplies() {
        return replies;
    }
}