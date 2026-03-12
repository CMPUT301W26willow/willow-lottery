package com.example.willow_lotto_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private final List<Event> events = new ArrayList<>();

    public void setEvents(List<Event> events) {
        this.events.clear();
        if (events != null) {
            this.events.addAll(events);
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

        // Set up register button
        holder.registerButton.setOnClickListener(v -> {

            String uid = com.google.firebase.auth.FirebaseAuth
                    // Get the current user ID
                    .getInstance()
                    .getCurrentUser()
                    .getUid();

            // Access the firestore
            com.google.firebase.firestore.FirebaseFirestore db =
                    com.google.firebase.firestore.FirebaseFirestore.getInstance();

            java.util.Map<String, Object> registration = new java.util.HashMap<>();
            // Build the registration document
            registration.put("userId", uid); //current user
            registration.put("eventId", event.getId()); //current event id
            registration.put("eventName", event.getName()); //current event name
            registration.put("status", "registered");

           // save the registration in the registration documents
            db.collection("registrations")
                    .add(registration)
                    .addOnSuccessListener(doc ->
                            // notify the user when they register for the event
                            android.widget.Toast.makeText(
                                    v.getContext(),
                                    "Registered for event",
                                    android.widget.Toast.LENGTH_SHORT
                            ).show())
                    .addOnFailureListener(e ->
                            // notify the user when it failed
                            android.widget.Toast.makeText(
                                    v.getContext(),
                                    "Registration failed",
                                    android.widget.Toast.LENGTH_SHORT
                            ).show());
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

        final Button registerButton;

        EventViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.event_name);
            date = itemView.findViewById(R.id.event_date);
            description = itemView.findViewById(R.id.event_description);
            registerButton = itemView.findViewById(R.id.register_button);        }
    }
}
