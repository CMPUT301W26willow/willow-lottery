package com.example.willow_lotto_app.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.events.poster.EventPosterLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * EventsAdapter.java
 * <p>
 * Author: Mehr Dhanda
 * <p>
 * RecyclerView adapter for event cards on Home and Events screens.
 * Supports keyword filtering across event name, description, and date.
 * <p>
 * Responsibilities:
 * - Binds Firestore-backed {@link Event} models to {@code item_event.xml}
 *   cards, including poster, title, date, and short description.
 * - Supports keyword search filtering via {@link #filter(String)}.
 * - Exposes callbacks for:
 *   - Joining/leaving events (01.01.01 / 01.01.02) via
 *     {@link OnJoinLeaveListener}.
 *   - Opening detailed event view (01.01.03) via
 *     {@link OnEventClickListener}.
 * <p>
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

    /**
     * Filters events to show which ones are open
     * compares the end date to today's date
     * events with no end date are treated as open
     */

    public void filterByOpenStatus() {
        events.clear();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        for (Event event : allEvents) {
            String end = event.getRegistrationEnd();
            // If no end date set, treat as open
            if (end == null || end.isEmpty() || end.compareTo(today) >= 0) {
                events.add(event);
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Filters events based on spots remaining
     * compares the limit with registrateduser count
     * events with no limits are treated as unlimited
     */

    public void filterByAvailability() {
        events.clear();
        for (Event event : allEvents) {
            Integer limit = event.getLimit();
            int registered = event.getWaitlistDisplayCount();
            if (limit == null || registered < limit) {
                events.add(event);
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Sorts events by date
     */

    public void filterByDate() {
        events.clear();
        events.addAll(allEvents);
        events.sort((a, b) -> {
            String dateA = a.getDate() != null ? a.getDate() : "";
            String dateB = b.getDate() != null ? b.getDate() : "";
            return dateA.compareTo(dateB);
        });
        notifyDataSetChanged();
    }

    public void clearFilters() {
        events.clear();
        events.addAll(allEvents);
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
        EventPosterLoader.load(holder.itemView.getContext(), event.getPosterUri(), holder.poster, event.getId());
        holder.name.setText(event.getName() != null ? event.getName() : "");
        String dateStr = event.getDate() != null ? event.getDate() : "";
        holder.date.setText(dateStr.isEmpty() ? "—" : dateStr);

        int registered = event.getWaitlistDisplayCount();
        holder.waitlist.setText(holder.itemView.getContext().getString(R.string.home_event_waitlist_count, registered));

        String regEnd = event.getRegistrationEnd();
        if (regEnd != null && !regEnd.trim().isEmpty()) {
            holder.deadline.setVisibility(View.VISIBLE);
            holder.deadline.setText(holder.itemView.getContext().getString(R.string.home_event_deadline, regEnd.trim()));
        } else {
            holder.deadline.setVisibility(View.GONE);
        }

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
        final TextView waitlist;
        final TextView deadline;
        final TextView description;
        final Button joinLeaveBtn;

        EventViewHolder(View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.event_poster);
            name = itemView.findViewById(R.id.event_name);
            date = itemView.findViewById(R.id.event_date);
            waitlist = itemView.findViewById(R.id.event_waitlist);
            deadline = itemView.findViewById(R.id.event_deadline);
            description = itemView.findViewById(R.id.event_description);
            joinLeaveBtn = itemView.findViewById(R.id.event_join_leave_btn);
        }
    }
}