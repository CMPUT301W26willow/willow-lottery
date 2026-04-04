/*
 * CommentsUnitTest.java
 *
 * Author: Dev Tiwari
 *
 * Unit tests for event comments/replies: Firestore drafts, model, thread merge, adapter,
 * fixture JSON, reply ordering, composer validation, intent resolution.
 */
package com.example.willow_lotto_app.commentsTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.net.Uri;

import com.example.willow_lotto_app.events.EventComment;
import com.example.willow_lotto_app.events.EventCommentDocument;
import com.example.willow_lotto_app.events.EventCommentPostValidator;
import com.example.willow_lotto_app.events.EventCommentThread;
import com.example.willow_lotto_app.events.EventCommentThreadMerge;
import com.example.willow_lotto_app.events.EventCommentsAdapter;
import com.example.willow_lotto_app.events.EventDetailActivity;
import com.example.willow_lotto_app.events.EventDetailIntentHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CommentsUnitTest {

    private static final EventCommentsAdapter.ThreadListener NO_OP_LISTENER =
            new EventCommentsAdapter.ThreadListener() {
                @Override
                public void onReply(EventComment topLevel) {
                }

                @Override
                public void onExpandReplies(EventCommentThread thread, int adapterPosition) {
                }

                @Override
                public void onCollapseReplies(int adapterPosition) {
                }
            };

    private static EventComment fromSnap(String id, String body, String parentId,
            String authorName, Timestamp createdAt) {
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);
        when(doc.getId()).thenReturn(id);
        when(doc.getString("authorId")).thenReturn("u");
        when(doc.getString("authorName")).thenReturn(authorName);
        when(doc.getString("body")).thenReturn(body);
        when(doc.getTimestamp("createdAt")).thenReturn(createdAt);
        when(doc.getString("parentCommentId")).thenReturn(parentId);
        return EventComment.fromSnapshot(doc);
    }

    @Test
    public void newDraft_topLevelReplyAndDefaultAuthor() {
        Map<String, Object> top = EventCommentDocument.newDraft("u1", "Alex", "Hello", null);
        assertEquals(EventComment.TOP_LEVEL_PARENT_ID, top.get("parentCommentId"));
        assertTrue(top.get("createdAt") instanceof FieldValue);

        Map<String, Object> reply = EventCommentDocument.newDraft("u1", "Alex", "Re", "pid");
        assertEquals("pid", reply.get("parentCommentId"));

        assertEquals("User", EventCommentDocument.newDraft("u1", null, "Hi", null).get("authorName"));
        assertEquals("User", EventCommentDocument.newDraft("u1", "  ", "Hi", null).get("authorName"));
    }

    @Test
    public void model_topLevelReplyAndDisplayName() {
        assertTrue(fromSnap("1", "t", null, "Sam", null).isTopLevel());
        assertTrue(fromSnap("1", "t", "", "Sam", null).isTopLevel());
        EventComment r = fromSnap("r", "t", "topId", "Sam", null);
        assertFalse(r.isTopLevel());
        assertEquals("topId", r.getParentCommentId());
        assertEquals("User", fromSnap("1", "b", "", null, null).resolveDisplayName());
    }

    @Test
    public void merge_preservesRepliesAndHandlesNulls() {
        Timestamp now = Timestamp.now();
        EventComment top1 = fromSnap("c1", "First", "", "A", now);
        EventCommentThread thread = new EventCommentThread(top1);
        thread.getReplies().add(fromSnap("r1", "reply", "c1", "B", now));
        thread.setExpanded(true);

        EventComment top2 = fromSnap("c1", "Edited", "", "A", now);
        List<EventCommentThread> merged = EventCommentThreadMerge.merge(
                Collections.singletonList(top2), Collections.singletonList(thread));
        assertSame(thread, merged.get(0));
        assertEquals("Edited", merged.get(0).getTop().getBody());
        assertEquals("reply", merged.get(0).getReplies().get(0).getBody());

        List<EventCommentThread> fresh = EventCommentThreadMerge.merge(
                Collections.singletonList(fromSnap("n", "New", "", "A", now)), Collections.emptyList());
        assertEquals(1, fresh.size());
        assertTrue(fresh.get(0).getReplies().isEmpty());

        assertEquals(0, EventCommentThreadMerge.merge(null, new ArrayList<>()).size());
        assertEquals(1, EventCommentThreadMerge.merge(Collections.singletonList(top2), null).size());
    }

    @Test
    public void adapter_fixtureAndReplySort() throws Exception {
        EventCommentsAdapter adapter = new EventCommentsAdapter(NO_OP_LISTENER);
        adapter.setThreads(Arrays.asList(
                new EventCommentThread(fromSnap("c1", "a", "", "x", Timestamp.now())),
                new EventCommentThread(fromSnap("c2", "b", "", "x", Timestamp.now()))));
        assertEquals(2, adapter.getItemCount());
        adapter.setThreads(Collections.emptyList());
        assertEquals(0, adapter.getItemCount());

        java.io.InputStream stream = CommentsUnitTest.class.getClassLoader()
                .getResourceAsStream("comment_fixtures/sample_event_comments.json");
        assertNotNull(stream);
        String json;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            json = br.lines().collect(Collectors.joining("\n"));
        }
        JSONObject root = new JSONObject(json);
        assertEquals("evt_willow_spring_gala_2026", root.getString("eventId"));
        assertEquals(2, root.getJSONArray("replies").length());

        List<EventComment> replies = new ArrayList<>();
        replies.add(fromSnap("a", "b", "p", "x", new Timestamp(300, 0)));
        replies.add(fromSnap("b", "b", "p", "x", new Timestamp(100, 0)));
        replies.add(fromSnap("c", "b", "p", "x", new Timestamp(200, 0)));
        Collections.sort(replies, EventComment.createdTimeAscending());
        assertEquals(100, replies.get(0).getCreatedAt().getSeconds());
    }

    @Test
    public void validator_nonEmptyBody() {
        assertFalse(EventCommentPostValidator.hasNonEmptyBody(null));
        assertFalse(EventCommentPostValidator.hasNonEmptyBody("  \t"));
        assertTrue(EventCommentPostValidator.hasNonEmptyBody("ok"));
    }

    @Test
    public void intent_resolveEventId() {
        Intent extra = new Intent();
        extra.putExtra(EventDetailActivity.EXTRA_EVENT_ID, "  id1  ");
        assertEquals("id1", EventDetailIntentHelper.resolveEventId(extra));

        Intent deep = new Intent();
        deep.setData(Uri.parse("willow-lottery://event/id2"));
        assertEquals("id2", EventDetailIntentHelper.resolveEventId(deep));

        Intent both = new Intent();
        both.putExtra(EventDetailActivity.EXTRA_EVENT_ID, "win");
        both.setData(Uri.parse("willow-lottery://event/lose"));
        assertEquals("win", EventDetailIntentHelper.resolveEventId(both));

        assertNull(EventDetailIntentHelper.resolveEventId(new Intent()));
        assertNull(EventDetailIntentHelper.resolveEventId(null));

        Intent bad = new Intent();
        bad.setData(Uri.parse("https://x.com/e/1"));
        assertNull(EventDetailIntentHelper.resolveEventId(bad));
    }
}
