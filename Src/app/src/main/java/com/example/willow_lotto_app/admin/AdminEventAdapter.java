package com.example.willow_lotto_app.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.events.Event;
import com.example.willow_lotto_app.events.EventPosterLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter used by the administrator event moderation screen.
 *
 * Responsibilities:
 * - Displays event cards in the admin dashboard.
 * - Lets the admin remove events.
 * - Lets the admin remove event images.
 * - Lets the admin remove comments for an event.
 */

public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {
    /**
     * Listener used by the hosting activity to respond to admin event actions.
     */
    public interface AdminEventActionListener {
        void onDeleteEventClicked(Event event);
        void onDeleteCommentsClicked(Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final AdminEventActionListener listener;

    /**
     * Creates the adapter with an initial event list.
     *
     * @param events initial events
     * @param listener action listener
     */
    public AdminEventAdapter(List<Event> events, AdminEventActionListener listener) {
        if (events != null) {
            this.events.addAll(events);
        }
        this.listener = listener;
    }

    /**
     * Replaces the event list and refreshes the display.
     *
     * @param updatedEvents new event list
     */
    public void setEvents(List<Event> updatedEvents) {
        events.clear();
        if (updatedEvents != null) {
            events.addAll(updatedEvents);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.nameText.setText(event.getName() != null ? event.getName() : "");
        holder.dateText.setText(event.getDate() != null ? event.getDate() : "");
        holder.descriptionText.setText(event.getDescription() != null ? event.getDescription() : "");

        EventPosterLoader.load(holder.itemView.getContext(), event.getPosterUri(), holder.posterImage, event.getId());

        holder.deleteEventButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteEventClicked(event);
            }
        });


        holder.deleteCommentsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteCommentsClicked(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for a single admin event row.
     */
    static class AdminEventViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImage;
        TextView nameText;
        TextView dateText;
        TextView descriptionText;
        Button deleteEventButton;
        Button deleteCommentsButton;

        AdminEventViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.admin_event_poster);
            nameText = itemView.findViewById(R.id.admin_event_name);
            dateText = itemView.findViewById(R.id.admin_event_date);
            descriptionText = itemView.findViewById(R.id.admin_event_description);
            deleteEventButton = itemView.findViewById(R.id.admin_delete_event_button);
            deleteCommentsButton = itemView.findViewById(R.id.admin_delete_comments_button);
        }
    }
}
