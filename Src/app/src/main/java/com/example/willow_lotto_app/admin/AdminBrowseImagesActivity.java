package com.example.willow_lotto_app.admin;


import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.events.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Administrator screen for browsing uploaded event images.
 *
 * Current scope:
 * - Uses event posterUri values as the image source.
 * - Lets the admin remove the image by clearing posterUri on the event.
 */

public class AdminBrowseImagesActivity extends AppCompatActivity implements AdminImageAdapter.AdminImageActionListener{
    /**
     * RecyclerView for image moderation rows.
     */
    private RecyclerView recyclerView;

    /**
     * Firestore instance.
     */
    private FirebaseFirestore db;

    /**
     * Local list of events that currently have images.
     */
    private final List<Event> imageEvents = new ArrayList<>();

    /**
     * Adapter used by the RecyclerView.
     */
    private AdminImageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_images);

        recyclerView = findViewById(R.id.adminImagesRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        adapter = new AdminImageAdapter(imageEvents, this);
        recyclerView.setAdapter(adapter);

        loadImages();
    }

    /**
     * Loads events that currently have a non-empty posterUri.
     */
    private void loadImages() {
        db.collection("events")
                .get()
                .addOnSuccessListener(query -> {
                    imageEvents.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String posterUri = doc.getString("posterUri");
                        Boolean isDeleted = doc.getBoolean("isDeleted");

                        if (isDeleted != null && isDeleted) {
                            continue;
                        }

                        if (posterUri != null && !posterUri.trim().isEmpty()) {
                            Event event = new Event();
                            event.setId(doc.getId());
                            event.setName(doc.getString("name"));
                            event.setDate(doc.getString("date"));
                            event.setPosterUri(posterUri);
                            imageEvents.add(event);
                        }
                    }
                    Toast.makeText(this, "Loaded images: " + imageEvents.size(), Toast.LENGTH_SHORT).show();

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show());
    }


    /**
     * Shows confirmation before removing an uploaded event image.
     *
     * @param event selected event containing the image
     */
    @Override
    public void onRemoveImageClicked(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Image")
                .setMessage("Are you sure you want to remove this uploaded image?")
                .setPositiveButton("Confirm", (dialog, which) -> removeImage(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Removes the image for the selected event by clearing posterUri.
     *
     * @param event selected event
     */
    private void removeImage(Event event) {
        db.collection("events")
                .document(event.getId())
                .update("posterUri", "")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
                    loadImages();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove image", Toast.LENGTH_SHORT).show());
    }
}
