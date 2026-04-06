package com.example.willow_lotto_app.events.comments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Merges new comment snapshots with existing threads (keeps replies and expand state). */
public final class EventCommentThreadMerge {

    private EventCommentThreadMerge() {
    }

    public static List<EventCommentThread> merge(
            List<EventComment> topLevelFromSnap,
            List<EventCommentThread> previousThreads) {
        Map<String, EventCommentThread> prev = new LinkedHashMap<>();
        if (previousThreads != null) {
            for (EventCommentThread t : previousThreads) {
                prev.put(t.getTop().getDocumentId(), t);
            }
        }
        List<EventCommentThread> out = new ArrayList<>();
        if (topLevelFromSnap == null) {
            return out;
        }
        for (EventComment top : topLevelFromSnap) {
            EventCommentThread thread = prev.get(top.getDocumentId());
            if (thread == null) {
                thread = new EventCommentThread(top);
            } else {
                thread.setTop(top);
            }
            out.add(thread);
        }
        return out;
    }
}
