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

/** RecyclerView adapter for event list; supports join/leave and opening event detail. */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    public interface OnJoinLeaveListener {
        void onJoin(Event event);
        void onLeave(Event event);
    }

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final Set<String> joinedEventIds = new HashSet<>();
    private String currentUserId;
    private OnJoinLeaveListener joinLeaveListener;
    private OnEventClickListener eventClickListener;

    public void setEvents(List<Event> events) {
        this.events.clear();
        if (events != null) {
            this.events.addAll(events);
        }
        notifyDataSetChanged();
    }

    public void setJoinedEventIds(Set<String> joinedEventIds) {
        this.joinedEventIds.clear();
        if (joinedEventIds != null) {
            this.joinedEventIds.addAll(joinedEventIds);
        }
        notifyDataSetChanged();
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    public void setOnJoinLeaveListener(OnJoinLeaveListener listener) {
        this.joinLeaveListener = listener;
    }

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
