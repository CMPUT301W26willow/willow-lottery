package com.example.willow_lotto_app.events.comments;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/** RecyclerView for event comment threads, replies, and reactions. */
public final class EventCommentsAdapter extends RecyclerView.Adapter<EventCommentsAdapter.ThreadHolder> {

    public interface ThreadListener {
        void onReply(EventComment topLevel);

        void onExpandReplies(EventCommentThread thread, int adapterPosition);

        void onCollapseReplies(int adapterPosition);

        void onDelete(EventComment comment);

        void onReact(EventComment comment, String emoji);
    }

    private final List<EventCommentThread> threads = new ArrayList<>();
    private final ThreadListener listener;
    private boolean isOrganizer = false;
    private String currentUserId;

    public EventCommentsAdapter(ThreadListener listener) {
        this.listener = listener;
    }

    public List<EventCommentThread> getThreads() {
        return new ArrayList<>(threads);
    }

    public void setThreads(List<EventCommentThread> next) {
        threads.clear();
        if (next != null) {
            threads.addAll(next);
        }
        notifyDataSetChanged();
    }

    public void setIsOrganizer(boolean isOrganizer) {
        this.isOrganizer = isOrganizer;
        notifyDataSetChanged();
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThreadHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_comment, parent, false);
        return new ThreadHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadHolder holder, int position) {
        holder.bind(threads.get(position), isOrganizer, currentUserId);
    }

    @Override
    public int getItemCount() {
        return threads.size();
    }

    private static void bindReactionChips(
            TextView thumbs,
            TextView heart,
            TextView laugh,
            EventComment comment,
            String uid,
            ThreadListener listener) {
        TextView[] views = {thumbs, heart, laugh};
        for (int i = 0; i < EventCommentReactions.EMOJIS.length; i++) {
            String emoji = EventCommentReactions.EMOJIS[i];
            TextView chip = views[i];
            chip.setText(EventCommentReactions.label(comment.getReactionByUser(), emoji));
            boolean mine = EventCommentReactions.userHasReacted(comment.getReactionByUser(), uid, emoji);
            chip.setTextColor(ContextCompat.getColor(chip.getContext(),
                    mine ? R.color.primaryPurple : R.color.textGray));
            chip.setOnClickListener(v -> listener.onReact(comment, emoji));
        }
    }

    static final class ThreadHolder extends RecyclerView.ViewHolder {

        private final TextView authorView;
        private final TextView timeView;
        private final TextView bodyView;
        private final TextView reactThumbs;
        private final TextView reactHeart;
        private final TextView reactLaugh;
        private final Button replyBtn;
        private final Button deleteBtn;
        private final TextView toggleReplies;
        private final ProgressBar loading;
        private final RecyclerView repliesRv;
        private final ThreadListener listener;
        private final RepliesAdapter repliesAdapter;

        ThreadHolder(@NonNull View itemView, ThreadListener listener) {
            super(itemView);
            this.listener = listener;
            authorView = itemView.findViewById(R.id.event_comment_author);
            timeView = itemView.findViewById(R.id.event_comment_time);
            bodyView = itemView.findViewById(R.id.event_comment_body);
            reactThumbs = itemView.findViewById(R.id.event_comment_react_thumbs);
            reactHeart = itemView.findViewById(R.id.event_comment_react_heart);
            reactLaugh = itemView.findViewById(R.id.event_comment_react_laugh);
            replyBtn = itemView.findViewById(R.id.event_comment_reply_btn);
            deleteBtn = itemView.findViewById(R.id.event_comment_delete_btn);
            toggleReplies = itemView.findViewById(R.id.event_comment_toggle_replies);
            loading = itemView.findViewById(R.id.event_comment_replies_loading);
            repliesRv = itemView.findViewById(R.id.event_comment_replies_list);
            repliesRv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            repliesAdapter = new RepliesAdapter(listener);
            repliesRv.setAdapter(repliesAdapter);
        }

        void bind(EventCommentThread thread, boolean isOrganizer, String currentUserId) {
            EventComment c = thread.getTop();
            authorView.setText(c.resolveDisplayName());
            bodyView.setText(c.getBody() != null ? c.getBody() : "");
            bindTime(timeView, c.getCreatedAt());

            EventCommentsAdapter.bindReactionChips(
                    reactThumbs, reactHeart, reactLaugh, c, currentUserId, listener);

            replyBtn.setOnClickListener(v -> listener.onReply(c));

            if (isOrganizer) {
                deleteBtn.setVisibility(View.VISIBLE);
                deleteBtn.setOnClickListener(v -> listener.onDelete(c));
            } else {
                deleteBtn.setVisibility(View.GONE);
            }

            boolean expanded = thread.isExpanded();
            toggleReplies.setText(expanded
                    ? R.string.event_detail_comment_hide_replies
                    : R.string.event_detail_comment_show_replies);

            toggleReplies.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    return;
                }
                if (thread.isExpanded()) {
                    thread.setExpanded(false);
                    listener.onCollapseReplies(pos);
                } else {
                    thread.setExpanded(true);
                    listener.onExpandReplies(thread, pos);
                }
            });

            loading.setVisibility(thread.isLoadingReplies() ? View.VISIBLE : View.GONE);

            repliesAdapter.setCurrentUserId(currentUserId);

            if (expanded && !thread.isLoadingReplies() && !thread.getReplies().isEmpty()) {
                repliesRv.setVisibility(View.VISIBLE);
                repliesAdapter.setReplies(thread.getReplies());
            } else {
                repliesRv.setVisibility(View.GONE);
                repliesAdapter.setReplies(null);
            }
        }

        private static void bindTime(TextView timeView, Timestamp ts) {
            if (ts != null) {
                long ms = ts.toDate().getTime();
                CharSequence rel = DateUtils.getRelativeTimeSpanString(
                        ms,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);
                timeView.setText(rel);
                timeView.setVisibility(View.VISIBLE);
            } else {
                timeView.setText("");
                timeView.setVisibility(View.GONE);
            }
        }
    }

    static final class RepliesAdapter extends RecyclerView.Adapter<RepliesAdapter.Holder> {

        private final List<EventComment> items = new ArrayList<>();
        private final ThreadListener listener;
        private String currentUserId;

        RepliesAdapter(ThreadListener listener) {
            this.listener = listener;
        }

        void setCurrentUserId(String uid) {
            this.currentUserId = uid;
        }

        void setReplies(List<EventComment> next) {
            items.clear();
            if (next != null) {
                items.addAll(next);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event_comment_reply, parent, false);
            return new Holder(v, listener);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position), currentUserId);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {

            private final TextView authorView;
            private final TextView timeView;
            private final TextView bodyView;
            private final TextView reactThumbs;
            private final TextView reactHeart;
            private final TextView reactLaugh;
            private final ThreadListener listener;

            Holder(@NonNull View itemView, ThreadListener listener) {
                super(itemView);
                this.listener = listener;
                authorView = itemView.findViewById(R.id.event_comment_reply_author);
                timeView = itemView.findViewById(R.id.event_comment_reply_time);
                bodyView = itemView.findViewById(R.id.event_comment_reply_body);
                reactThumbs = itemView.findViewById(R.id.event_comment_reply_react_thumbs);
                reactHeart = itemView.findViewById(R.id.event_comment_reply_react_heart);
                reactLaugh = itemView.findViewById(R.id.event_comment_reply_react_laugh);
            }

            void bind(EventComment c, String currentUserId) {
                authorView.setText(c.resolveDisplayName());
                bodyView.setText(c.getBody() != null ? c.getBody() : "");
                Timestamp ts = c.getCreatedAt();
                if (ts != null) {
                    long ms = ts.toDate().getTime();
                    CharSequence rel = DateUtils.getRelativeTimeSpanString(
                            ms,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS);
                    timeView.setText(rel);
                    timeView.setVisibility(View.VISIBLE);
                } else {
                    timeView.setText("");
                    timeView.setVisibility(View.GONE);
                }
                EventCommentsAdapter.bindReactionChips(
                        reactThumbs, reactHeart, reactLaugh, c, currentUserId, listener);
            }
        }
    }
}
