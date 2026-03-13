package com.example.willow_lotto_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 
 *EventsAdapter
 *
 * RecyclerView adapter responsible for displaying a list of events in the UI.
 *
 * This adapter binds Event data to the item_event layout and manages
 * user interactions such as:
 *  - Joining an event
 *  - Leaving an event
 *  - Clicking an event to view more details
 *
 * The adapter does not directly handle database operations. Instead,
 * it communicates user actions through listener interfaces which are
 * implemented by the hosting activity (e.g., MainActivity).

*/
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {
    /**
     * Listener used to notify when the user presses Join or Leave.
     * The activity using this adapter will implement the logic.
     */
    public interface OnJoinLeaveListener {
        void onJoin(Event event);
        void onLeave(Event event);
    }

    /**
     * Listener used to notify when the user clicks on an event.
     * The activity using this adapter will implement the logic.
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final Set<String> joinedEventIds = new HashSet<>();
    private String currentUserId;
    private OnJoinLeaveListener joinLeaveListener;
    private OnEventClickListener eventClickListener;

    // Set the events list
    public void setEvents(List<Event> events) {
        this.events.clear();
        if (events != null) {
            this.events.addAll(events);
        }
        notifyDataSetChanged();
    }

    // Set the joined event IDs
    public void setJoinedEventIds(Set<String> joinedEventIds) {
        this.joinedEventIds.clear();
        if (joinedEventIds != null) {
            this.joinedEventIds.addAll(joinedEventIds);
        }
        notifyDataSetChanged();
    }

    // Set the current user ID
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    // Set the join leave listener
    public void setOnJoinLeaveListener(OnJoinLeaveListener listener) {
        this.joinLeaveListener = listener;
    }

    // Set the event click listener
    public void setOnEventClickListener(OnEventClickListener listener) {
        this.eventClickListener = listener;
    }

    /** Updates join state for one event and refreshes list. */
    public void setEventJoined(String eventId, boolean joined) {
        if (joined) {
            joinedEventIds.add(eventId);
        } else {
            joinedEventIds.remove(eventId);
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
        holder.name.setText(event.getName() != null ? event.getName() : "");
        holder.date.setText(event.getDate() != null ? event.getDate() : "");
        holder.description.setText(event.getDescription() != null ? event.getDescription() : "");

        boolean joined = event.getId() != null && joinedEventIds.contains(event.getId());
        boolean canJoinLeave = currentUserId != null && joinLeaveListener != null;

        holder.joinLeaveBtn.setEnabled(canJoinLeave);
        holder.joinLeaveBtn.setVisibility(View.VISIBLE);
        holder.joinLeaveBtn.setText(joined
                ? holder.itemView.getContext().getString(R.string.event_leave)
                : holder.itemView.getContext().getString(R.string.event_join));
        holder.joinLeaveBtn.setOnClickListener(v -> {
            if (joinLeaveListener == null) return;
            if (joined) {
                joinLeaveListener.onLeave(event);
            } else {
                joinLeaveListener.onJoin(event);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (eventClickListener != null && event.getId() != null) {
                eventClickListener.onEventClick(event);
            }
        });
    }

    // Get the item count
    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView date;
        final TextView description;
        final Button joinLeaveBtn;

        EventViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.event_name);
            date = itemView.findViewById(R.id.event_date);
            description = itemView.findViewById(R.id.event_description);
            joinLeaveBtn = itemView.findViewById(R.id.event_join_leave_btn);
        }
    }
}
