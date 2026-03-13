package com.example.willow_lotto_app;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter for event cards on Home and Events screens.
 *
 * Responsibilities:
 * - Binds Firestore-backed {@link Event} models to {@code item_event.xml}
 *   cards, including poster, title, date, and short description.
 * - Exposes callbacks for:
 *   - Joining/leaving events (01.01.01 / 01.01.02) via
 *     {@link OnJoinLeaveListener} (used in older flows).
 *   - Opening detailed event view (01.01.03) via
 *     {@link OnEventClickListener}.
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

        // Home/Events list matches mockup: primary action is "View Details".
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
