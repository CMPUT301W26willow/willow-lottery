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
import com.example.willow_lotto_app.events.poster.EventPosterLoader;

import java.util.ArrayList;
import java.util.List;

/** Lists events for admin moderation; delete actions run in the activity. */
public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    public interface AdminEventActionListener {
        /**
         * @param event event to soft-delete
         */
        void onDeleteEventClicked(Event event);

        /**
         * @param event event whose comments should be marked removed
         */
        void onDeleteCommentsClicked(Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final AdminEventActionListener listener;

    /**
     * @param events   initial list; may be null
     * @param listener action callbacks; may be null
     */
    public AdminEventAdapter(List<Event> events, AdminEventActionListener listener) {
        if (events != null) {
            this.events.addAll(events);
        }
        this.listener = listener;
    }

    /**
     * @param updatedEvents replaces backing list; may be null to clear
     */
    public void setEvents(List<Event> updatedEvents) {
        events.clear();
        if (updatedEvents != null) {
            events.addAll(updatedEvents);
        }
        notifyDataSetChanged();
    }

    /**
     * @param parent   parent ViewGroup
     * @param viewType unused view type
     * @return holder for {@code R.layout.item_admin_event}
     */
    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(view);
    }

    /**
     * @param holder   row holder
     * @param position index in the backing list
     */
    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        Event event = events.get(position);

        // Poster + copy; buttons delegate to activity
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
