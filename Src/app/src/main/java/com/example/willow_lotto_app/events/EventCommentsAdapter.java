package com.example.willow_lotto_app.events;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level comments with expand/collapse replies (Option A).
 */
public class EventCommentsAdapter extends RecyclerView.Adapter<EventCommentsAdapter.ThreadHolder> {

    public interface ThreadListener {
        void onReply(EventComment topLevel);

        /** Called when user expands thread; activity loads replies if needed. */
        void onExpandReplies(EventCommentThread thread, int adapterPosition);

        void onCollapseReplies(int adapterPosition);
    }

    private final List<EventCommentThread> threads = new ArrayList<>();
    private final ThreadListener listener;

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

    @NonNull
    @Override
    public ThreadHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_comment, parent, false);
        return new ThreadHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadHolder holder, int position) {
        holder.bind(threads.get(position));
    }

    @Override
    public int getItemCount() {
        return threads.size();
    }

    static final class ThreadHolder extends RecyclerView.ViewHolder {

        private final TextView authorView;
        private final TextView timeView;
        private final TextView bodyView;
        private final Button replyBtn;
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
            replyBtn = itemView.findViewById(R.id.event_comment_reply_btn);
            toggleReplies = itemView.findViewById(R.id.event_comment_toggle_replies);
            loading = itemView.findViewById(R.id.event_comment_replies_loading);
            repliesRv = itemView.findViewById(R.id.event_comment_replies_list);
            repliesRv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            repliesAdapter = new RepliesAdapter();
            repliesRv.setAdapter(repliesAdapter);
        }

        void bind(EventCommentThread thread) {
            EventComment c = thread.getTop();
            authorView.setText(c.resolveDisplayName());
            bodyView.setText(c.getBody() != null ? c.getBody() : "");
            bindTime(timeView, c.getCreatedAt());

            replyBtn.setOnClickListener(v -> listener.onReply(c));

            boolean expanded = thread.isExpanded();
            if (expanded) {
                toggleReplies.setText(R.string.event_detail_comment_hide_replies);
            } else {
                toggleReplies.setText(R.string.event_detail_comment_show_replies);
            }

            toggleReplies.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                if (thread.isExpanded()) {
                    thread.setExpanded(false);
                    listener.onCollapseReplies(pos);
                } else {
                    thread.setExpanded(true);
                    listener.onExpandReplies(thread, pos);
                }
            });

            loading.setVisibility(thread.isLoadingReplies() ? View.VISIBLE : View.GONE);

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
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {

            private final TextView authorView;
            private final TextView timeView;
            private final TextView bodyView;

            Holder(@NonNull View itemView) {
                super(itemView);
                authorView = itemView.findViewById(R.id.event_comment_reply_author);
                timeView = itemView.findViewById(R.id.event_comment_reply_time);
                bodyView = itemView.findViewById(R.id.event_comment_reply_body);
            }

            void bind(EventComment c) {
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
            }
        }
    }
}
