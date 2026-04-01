package com.example.willow_lotto_app.events;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RecyclerView.Adapter} implementation that binds a list of {@link EventComment}
 * objects to the {@code item_event_comment.xml} layout.
 * <p>
 * This adapter handles the display of the author's name, the comment body, and
 * a relative timestamp (e.g., "5 minutes ago").
 * </p>
 */
public class EventCommentsAdapter extends RecyclerView.Adapter<EventCommentsAdapter.Holder> {

    /** The local dataset containing the comments to be displayed. */
    private final List<EventComment> items = new ArrayList<>();

    /**
     * Updates the adapter's dataset and refreshes the UI.
     *
     * @param next The new list of comments to display. If null, the list is cleared.
     */
    public void setComments(List<EventComment> next) {
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
                .inflate(R.layout.item_event_comment, parent, false);
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

    /**
     * ViewHolder class that holds references to the views within a single comment item row.
     */
    static final class Holder extends RecyclerView.ViewHolder {

        private final TextView authorView;
        private final TextView timeView;
        private final TextView bodyView;

        /**
         * Initializes the view holder by finding the necessary sub-views.
         *
         * @param itemView The root view of the individual list item.
         */
        Holder(@NonNull View itemView) {
            super(itemView);
            authorView = itemView.findViewById(R.id.event_comment_author);
            timeView = itemView.findViewById(R.id.event_comment_time);
            bodyView = itemView.findViewById(R.id.event_comment_body);
        }

        /**
         * Binds the data from an {@link EventComment} to the UI components.
         * <p>
         * It calculates the relative time span (e.g., "Just now", "2 hours ago")
         * based on the current system time.
         * </p>
         *
         * @param c The comment data to display.
         */
        void bind(EventComment c) {
            authorView.setText(c.resolveDisplayName());
            bodyView.setText(c.getBody() != null ? c.getBody() : "");

            Timestamp ts = c.getCreatedAt();
            if (ts != null) {
                long ms = ts.toDate().getTime();
                // Generates a human-readable relative time string
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