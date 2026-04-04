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
 * Administrator screen for browsing and moderating events.
 * <p>
 * Responsibilities:
 * - Allows admins to browse events.
 * - Allows admins to remove events.
 * - Allows admins to remove event images.
 * - Allows admins to remove event comments that violate app policy.
 */

public class AdminBrowseEventsActivity extends AppCompatActivity implements AdminEventAdapter.AdminEventActionListener{
    /**
     * RecyclerView for the event list.
     */
    private RecyclerView recyclerView;

    /**
     * Firestore instance.
     */
    private FirebaseFirestore db;

    /**
     * Local event list displayed in the RecyclerView.
     */
    private final List<Event> events = new ArrayList<>();

    /**
     * Adapter used by the RecyclerView.
     */
    private AdminEventAdapter adapter;

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
        setContentView(R.layout.activity_admin_browse_events);

        searchQueryNormalized = AdminSearchTextUtil.normalizeQuery(
                getIntent().getStringExtra(AdminIntentExtras.EXTRA_SEARCH_QUERY));

        MaterialToolbar toolbar = findViewById(R.id.admin_events_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        recyclerView = findViewById(R.id.adminEventsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        adminEmail = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "";

        adapter = new AdminEventAdapter(events, this);
        recyclerView.setAdapter(adapter);

        screenReady = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!screenReady || isFinishing()) {
            return;
        }
        loadEvents();
    }

    /**
     * Loads all non-deleted events from Firestore.
     * <p>
     * If older events do not yet have an isDeleted field, they are still shown.
     */
    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(query -> {
                    events.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Boolean isDeleted = doc.getBoolean("isDeleted");
                        if (isDeleted != null && isDeleted) {
                            continue;
                        }

                        Event event = new Event();
                        event.setId(doc.getId());
                        event.setName(doc.getString("name"));
                        event.setDescription(doc.getString("description"));
                        event.setDate(doc.getString("date"));
                        event.setOrganizerId(doc.getString("organizerId"));
                        event.setPosterUri(doc.getString("posterUri"));
                        if (eventMatchesSearch(event)) {
                            events.add(event);
                        }
                    }

                    adapter.setEvents(events);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show());
    }

    private boolean eventMatchesSearch(Event event) {
        if (searchQueryNormalized.isEmpty()) {
            return true;
        }
        return AdminSearchTextUtil.containsNormalized(event.getName(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(event.getDescription(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(event.getDate(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(event.getId(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(event.getOrganizerId(), searchQueryNormalized);
    }

    /**
     * Shows confirmation before removing an event.
     *
     * @param event selected event
     */
    @Override
    public void onDeleteEventClicked(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to remove this event?")
                .setPositiveButton("Confirm", (dialog, which) -> deleteEvent(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows confirmation before removing the image for an event.
     *
     * @param event selected event
     */
    @Override
    public void onDeleteImageClicked(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event Image")
                .setMessage("Are you sure you want to remove this event image?")
                .setPositiveButton("Confirm", (dialog, which) -> removeEventImage(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows confirmation before removing comments for an event.
     *
     * @param event selected event
     */
    @Override
    public void onDeleteCommentsClicked(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event Comments")
                .setMessage("Are you sure you want to remove comments for this event that violate app policy?")
                .setPositiveButton("Confirm", (dialog, which) -> removeEventComments(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Soft-deletes an event and notifies the organizer if organizerId exists.
     *
     * @param event selected event
     */
    private void deleteEvent(Event event) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("events")
                .document(event.getId())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    notifyOrganizerAboutRemovedEvent(event);

                    AdminNotificationHelper.logAdminAction(
                            adminEmail,
                            "event",
                            event.getId(),
                            "delete_event",
                            "Event was soft-deleted by admin."
                    );

                    Toast.makeText(this, "Event removed", Toast.LENGTH_SHORT).show();
                    loadEvents();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove event", Toast.LENGTH_SHORT).show());
    }

    /**
     * Removes the event image by clearing the posterUri field.
     *
     * @param event selected event
     */
    private void removeEventImage(Event event) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("posterUri", "");
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("events")
                .document(event.getId())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    AdminNotificationHelper.logAdminAction(
                            adminEmail,
                            "event_image",
                            event.getId(),
                            "delete_image",
                            "Event posterUri cleared by admin."
                    );

                    Toast.makeText(this, "Event image removed", Toast.LENGTH_SHORT).show();
                    loadEvents();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove event image", Toast.LENGTH_SHORT).show());
    }

    /**
     * Marks all comments belonging to an event as removed.
     * <p>
     * This assumes a comments collection with an eventId field and an isRemoved field.
     *
     * @param event selected event
     */
    private void removeEventComments(Event event) {
        db.collection("comments")
                .whereEqualTo("eventId", event.getId())
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        db.collection("comments")
                                .document(doc.getId())
                                .update(
                                        "isRemoved", true,
                                        "updatedAt", FieldValue.serverTimestamp()
                                );
                    }

                    AdminNotificationHelper.logAdminAction(
                            adminEmail,
                            "comment",
                            event.getId(),
                            "delete_comments",
                            "All comments for event were marked isRemoved=true by admin."
                    );

                    Toast.makeText(this, "Event comments removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove event comments", Toast.LENGTH_SHORT).show());
    }

    /**
     * Sends an in-app notification to the organizer if the event document has an organizerId.
     *
     * @param event removed event
     */
    private void notifyOrganizerAboutRemovedEvent(Event event) {
        db.collection("events")
                .document(event.getId())
                .get()
                .addOnSuccessListener(doc -> {
                    String organizerId = doc.getString("organizerId");

                    AdminNotificationHelper.sendAdminNotification(
                            organizerId,
                            event.getId(),
                            "Event removed",
                            "Your event \"" + event.getName() + "\" was removed by an administrator for violating app policy.",
                            "admin_delete_event"
                    );
                });
    }
}
