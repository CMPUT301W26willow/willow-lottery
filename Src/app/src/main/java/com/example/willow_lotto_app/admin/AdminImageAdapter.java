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

/** Lists event posters for admin review; remove is handled by the activity. */
public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.AdminImageViewHolder> {

    public interface AdminImageActionListener {
        /**
         * @param event event whose poster should be removed (activity updates Firestore)
         */
        void onRemoveImageClicked(Event event);
    }

    private final List<Event> eventsWithImages = new ArrayList<>();
    private final AdminImageActionListener listener;

    /**
     * @param events   initial events with posters; may be null
     * @param listener receives remove actions; may be null (buttons no-op)
     */
    public AdminImageAdapter(List<Event> events, AdminImageActionListener listener) {
        if (events != null) {
            this.eventsWithImages.addAll(events);
        }
        this.listener = listener;
    }

    /**
     * @param updatedEvents replaces the backing list; may be null to clear
     */
    public void setEvents(List<Event> updatedEvents) {
        eventsWithImages.clear();
        if (updatedEvents != null) {
            eventsWithImages.addAll(updatedEvents);
        }
        notifyDataSetChanged();
    }

    /**
     * @param parent   parent ViewGroup
     * @param viewType row type (unused)
     * @return holder for {@code R.layout.item_admin_image}
     */
    @NonNull
    @Override
    public AdminImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new AdminImageViewHolder(view);
    }

    /**
     * @param holder   row views
     * @param position index in the backing list
     */
    @Override
    public void onBindViewHolder(@NonNull AdminImageViewHolder holder, int position) {
        Event event = eventsWithImages.get(position);

        // Title / date + poster preview via EventPosterLoader
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
