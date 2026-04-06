package com.example.willow_lotto_app.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.events.Event;
import com.example.willow_lotto_app.events.EventDetailActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import com.example.willow_lotto_app.events.EventDetailActivity;

/** Admin screen to browse events, delete them, or clear their comments. */
public class AdminBrowseEventsActivity extends AppCompatActivity
        implements AdminEventAdapter.AdminEventActionListener{

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private final List<Event> events = new ArrayList<>();
    private AdminEventAdapter adapter;
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

        // Optional filter from dashboard search
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

    // Skip isDeleted; optional text search on name/description/date/id/organizerId
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

    /**
     * @param event candidate event row
     * @return true if search is inactive or any of name, description, date, id, organizerId matches
     */
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
     * @param event event the admin chose to soft-delete
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

    @Override
    public void onViewEventClicked(Event event) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    /**
     * @param event removed event; each registration with this {@code eventId} receives a notification
     */
    private void notifyAffectedUsersAboutRemovedEvent(Event event) {
        db.collection("registrations")
                .whereEqualTo("eventId", event.getId())
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String userId = doc.getString("userId");

                        AdminNotificationHelper.sendAdminNotification(
                                userId,
                                event.getId(),
                                "Event removed",
                                "The event \"" + event.getName() + "\" was removed by an administrator. Your registration or waitlist entry is no longer active.",
                                "admin_delete_event_user"
                        );
                    }
                });
    }

    /**
     * @param event deleted event; all {@code registrations} docs with this {@code eventId} are deleted
     */
    private void deleteRegistrationsForRemovedEvent(Event event) {
        db.collection("registrations")
                .whereEqualTo("eventId", event.getId())
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        db.collection("registrations")
                                .document(doc.getId())
                                .delete();
                    }
                });
    }

    /**
     * @param event event to soft-delete ({@code isDeleted=true})
     */
    private void deleteEvent(Event event) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("events")
                .document(event.getId())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    notifyAffectedUsersAboutRemovedEvent(event);
                    deleteRegistrationsForRemovedEvent(event);
                    notifyOrganizerAboutRemovedEvent(event);

                    AdminNotificationHelper.logAdminAction(
                            adminEmail,
                            "event",
                            event.getId(),
                            "delete_event",
                            "Event was soft-deleted and related registrations were cleaned up by admin."
                    );

                    Toast.makeText(this, "Event removed", Toast.LENGTH_SHORT).show();
                    loadEvents();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove event", Toast.LENGTH_SHORT).show());
    }

    /**
     * @param event event whose {@code comments} docs (matching {@code eventId}) are marked {@code isRemoved}
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
     * @param event removed event; organizer receives {@code admin_delete_event} if {@code organizerId} is set
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
