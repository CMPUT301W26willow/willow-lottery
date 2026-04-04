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
 * RecyclerView adapter used by the administrator image moderation screen.
 * <p>
 * Current phase-1 design:
 * - The image list is built from event posterUri values.
 * - Removing an image clears the posterUri field on the related event.
 */

public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.AdminImageViewHolder> {
    /**
     * Listener used by the hosting activity to handle image removal.
     */
    public interface AdminImageActionListener {
        void onRemoveImageClicked(Event event);
    }

    private final List<Event> eventsWithImages = new ArrayList<>();
    private final AdminImageActionListener listener;

    /**
     * Creates the adapter for admin image browsing.
     *
     * @param events initial event list containing poster images
     * @param listener callback listener
     */
    public AdminImageAdapter(List<Event> events, AdminImageActionListener listener) {
        if (events != null) {
            this.eventsWithImages.addAll(events);
        }
        this.listener = listener;
    }

    /**
     * Replaces the displayed image source list.
     *
     * @param updatedEvents new list of events with posters
     */
    public void setEvents(List<Event> updatedEvents) {
        eventsWithImages.clear();
        if (updatedEvents != null) {
            eventsWithImages.addAll(updatedEvents);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new AdminImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminImageViewHolder holder, int position) {
        Event event = eventsWithImages.get(position);

        holder.imageTitleText.setText(event.getName() != null ? event.getName() : "Event Poster");
        holder.imageSubtitleText.setText(event.getDate() != null ? event.getDate() : "");

        EventPosterLoader.load(holder.itemView.getContext(), event.getPosterUri(), holder.previewImage, event.getId());

        holder.removeImageButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveImageClicked(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventsWithImages.size();
    }

    /**
     * ViewHolder for a single admin image row.
     */
    static class AdminImageViewHolder extends RecyclerView.ViewHolder {
        ImageView previewImage;
        TextView imageTitleText;
        TextView imageSubtitleText;
        Button removeImageButton;

        AdminImageViewHolder(@NonNull View itemView) {
            super(itemView);
            previewImage = itemView.findViewById(R.id.admin_image_preview);
            imageTitleText = itemView.findViewById(R.id.admin_image_title);
            imageSubtitleText = itemView.findViewById(R.id.admin_image_subtitle);
            removeImageButton = itemView.findViewById(R.id.admin_remove_image_button);
        }
    }
}
