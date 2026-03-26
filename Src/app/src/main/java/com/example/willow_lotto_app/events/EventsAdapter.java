package com.example.willow_lotto_app.events;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.willow_lotto_app.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * EventsAdapter.java
 *
 * Author: Mehr Dhanda
 *
 * RecyclerView adapter for event cards on Home and Events screens.
 * Supports keyword filtering across event name, description, and date.
 *
 * Responsibilities:
 * - Binds Firestore-backed {@link Event} models to {@code item_event.xml}
 *   cards, including poster, title, date, and short description.
 * - Supports keyword search filtering via {@link #filter(String)}.
 * - Exposes callbacks for:
 *   - Joining/leaving events (01.01.01 / 01.01.02) via
 *     {@link OnJoinLeaveListener}.
 *   - Opening detailed event view (01.01.03) via
 *     {@link OnEventClickListener}.
 *
 * Outstanding issues:
 * - Search is client-side only; does not query Firestore directly.
 */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    public interface OnJoinLeaveListener {
        void onJoin(Event event);
        void onLeave(Event event);
    }

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final List<Event> allEvents = new ArrayList<>();
    private final Set<String> joinedEventIds = new HashSet<>();
    private String currentUserId;
    private OnJoinLeaveListener joinLeaveListener;
    private OnEventClickListener eventClickListener;

    /**
     * Sets the full list of events and resets the filter.
     *
     * @param events List of events to display.
     */
    public void setEvents(List<Event> events) {
        this.events.clear();
        this.allEvents.clear();
        if (events != null) {
            this.events.addAll(events);
            this.allEvents.addAll(events);
        }
        notifyDataSetChanged();
    }

    /**
     * Sets the IDs of events the current user has joined.
     *
     * @param joinedEventIds Set of joined event IDs.
     */
    public void setJoinedEventIds(Set<String> joinedEventIds) {
        this.joinedEventIds.clear();
        if (joinedEventIds != null) {
            this.joinedEventIds.addAll(joinedEventIds);
        }
        notifyDataSetChanged();
    }

    /**
     * Sets the current user's ID for join/leave logic.
     *
     * @param currentUserId The current user's Firebase UID.
     */
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    /**
     * Sets the listener for join/leave button interactions.
     *
     * @param listener The join/leave listener.
     */
    public void setOnJoinLeaveListener(OnJoinLeaveListener listener) {
        this.joinLeaveListener = listener;
    }

    /**
     * Sets the listener for event card click interactions.
     *
     * @param listener The event click listener.
     */
    public void setOnEventClickListener(OnEventClickListener listener) {
        this.eventClickListener = listener;
    }

    /**
     * Updates join state for one event and refreshes the list.
     *
     * @param eventId The event ID to update.
     * @param joined  Whether the user has joined the event.
     */
    public void setEventJoined(String eventId, boolean joined) {
        if (joined) {
            joinedEventIds.add(eventId);
        } else {
            joinedEventIds.remove(eventId);
        }
        notifyDataSetChanged();
    }

    /**
     * Filters events by keyword across name, description, and date fields.
     *
     * @param query The search keyword entered by the user.
     */
    public void filter(String query) {
        events.clear();
        if (query == null || query.trim().isEmpty()) {
            events.addAll(allEvents);
        } else {
            String lower = query.toLowerCase().trim();
            for (Event event : allEvents) {
                boolean matchesName = event.getName() != null && event.getName().toLowerCase().contains(lower);
                boolean matchesDescription = event.getDescription() != null && event.getDescription().toLowerCase().contains(lower);
                boolean matchesDate = event.getDate() != null && event.getDate().toLowerCase().contains(lower);
                if (matchesName || matchesDescription || matchesDate) {
                    events.add(event);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        String posterUrl = event.getPosterUri();
        if (posterUrl != null && !posterUrl.trim().isEmpty()) {
            Uri uri = Uri.parse(posterUrl.trim());
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.poster_placeholder)
                    .error(R.drawable.poster_placeholder);
            Glide.with(holder.itemView.getContext())
                    .load(uri)
                    .apply(options)
                    .into(holder.poster);
        } else {
            holder.poster.setImageResource(R.drawable.poster_placeholder);
        }
        holder.name.setText(event.getName() != null ? event.getName() : "");
        holder.date.setText(event.getDate() != null ? event.getDate() : "");
        holder.description.setText(event.getDescription() != null ? event.getDescription() : "");

        boolean canViewDetails = eventClickListener != null && event.getId() != null;
        holder.joinLeaveBtn.setVisibility(View.VISIBLE);
        holder.joinLeaveBtn.setEnabled(canViewDetails);
        holder.joinLeaveBtn.setText(R.string.event_view_details);
        holder.joinLeaveBtn.setOnClickListener(v -> {
            if (eventClickListener != null && event.getId() != null) {
                eventClickListener.onEventClick(event);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (eventClickListener != null && event.getId() != null) {
                eventClickListener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final ImageView poster;
        final TextView name;
        final TextView date;
        final TextView description;
        final Button joinLeaveBtn;

        EventViewHolder(View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.event_poster);
            name = itemView.findViewById(R.id.event_name);
            date = itemView.findViewById(R.id.event_date);
            description = itemView.findViewById(R.id.event_description);
            joinLeaveBtn = itemView.findViewById(R.id.event_join_leave_btn);
        }
    }
}