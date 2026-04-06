package com.example.willow_lotto_app.admin;


import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.events.Event;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administrator screen for browsing uploaded event images.
 * <p>
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

    /**
     * Signed-in admin email used in audit logs.
     */
    private String adminEmail;

    private String searchQueryNormalized = "";

    private boolean screenReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AdminUiHelper.requireAdminOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_admin_browse_images);

        searchQueryNormalized = AdminSearchTextUtil.normalizeQuery(
                getIntent().getStringExtra(AdminIntentExtras.EXTRA_SEARCH_QUERY));

        MaterialToolbar toolbar = findViewById(R.id.admin_images_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        recyclerView = findViewById(R.id.adminImagesRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        adapter = new AdminImageAdapter(imageEvents, this);
        recyclerView.setAdapter(adapter);
        adminEmail = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "";

        screenReady = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!screenReady || isFinishing()) {
            return;
        }
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
                            if (imageEventMatchesSearch(event)) {
                                imageEvents.add(event);
                            }
                        }
                    }
                    adapter.setEvents(imageEvents);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show());
    }

    private boolean imageEventMatchesSearch(Event event) {
        if (searchQueryNormalized.isEmpty()) {
            return true;
        }
        return AdminSearchTextUtil.containsNormalized(event.getName(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(event.getDate(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(event.getId(), searchQueryNormalized);
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

    private void notifyOrganizerAboutRemovedImage(Event event) {
        db.collection("events")
                .document(event.getId())
                .get()
                .addOnSuccessListener(doc -> {
                    String organizerId = doc.getString("organizerId");

                    AdminNotificationHelper.sendAdminNotification(
                            organizerId,
                            event.getId(),
                            "Image removed",
                            "The image for your event \"" + event.getName() + "\" was removed by an administrator for violating app policy.",
                            "admin_delete_event_image"
                    );
                });
    }

    /**
     * Removes the image for the selected event by clearing posterUri.
     *
     * @param event selected event
     */
    private void removeImage(Event event) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("posterUri", "");
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("events")
                .document(event.getId())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    notifyOrganizerAboutRemovedImage(event);

                    AdminNotificationHelper.logAdminAction(
                            adminEmail,
                            "event_image",
                            event.getId(),
                            "delete_image",
                            "Event posterUri cleared by admin."
                    );

                    Toast.makeText(this, "Event image removed", Toast.LENGTH_SHORT).show();
                    loadImages();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove event image", Toast.LENGTH_SHORT).show());
    }
}
