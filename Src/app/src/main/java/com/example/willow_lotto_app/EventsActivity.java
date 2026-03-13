package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EventsActivity:
 * This activity is used to display the events list.
 * 
 * Features:
 * - Display the events list
 * - Display the events empty view
 * - Display the bottom navigation
 * 
 * Flow:
 * 1. The user navigates to the events activity
 * 2. The events list is displayed
 * 3. The user can join or leave an event
 * 4. The user can tap on an event to open the event detail activity
 * 5. The event detail activity is displayed
 * 6. The user can see the event details and join or leave the event
  */
public class EventsActivity extends AppCompatActivity {

    private static final String REGISTRATIONS_COLLECTION = "registrations";

    private RecyclerView eventsRecycler;
    private TextView eventsEmpty;
    private EventsAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        eventsRecycler = findViewById(R.id.events_recycler);
        eventsEmpty = findViewById(R.id.events_empty);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_events);

        adapter = new EventsAdapter();
        eventsRecycler.setLayoutManager(new LinearLayoutManager(this));
        eventsRecycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        adapter.setCurrentUserId(currentUserId);
        adapter.setOnJoinLeaveListener(new EventsAdapter.OnJoinLeaveListener() {
            @Override
            public void onJoin(Event event) {
                if (currentUserId == null) return;
                String docId = event.getId() + "_" + currentUserId;
                Map<String, Object> reg = new HashMap<>();
                reg.put("eventId", event.getId());
                reg.put("userId", currentUserId);
                db.collection(REGISTRATIONS_COLLECTION).document(docId).set(reg)
                        .addOnSuccessListener(aVoid -> adapter.setEventJoined(event.getId(), true))
                        .addOnFailureListener(e -> Toast.makeText(EventsActivity.this, "Could not join event", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onLeave(Event event) {
                if (currentUserId == null) return;
                String docId = event.getId() + "_" + currentUserId;
                db.collection(REGISTRATIONS_COLLECTION).document(docId).delete()
                        .addOnSuccessListener(aVoid -> adapter.setEventJoined(event.getId(), false))
                        .addOnFailureListener(e -> Toast.makeText(EventsActivity.this, "Could not leave event", Toast.LENGTH_SHORT).show());
            }
        });
        adapter.setOnEventClickListener(event -> {
            Intent intent = new Intent(EventsActivity.this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
        });
        loadEvents();

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }

            if (item.getItemId() == R.id.nav_events) {
                return true;
            }

            if (item.getItemId() == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                finish();
                return true;
            }

            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    // Load all events then user's joined IDs.
    private void loadEvents() {
        // load all events from the database
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = new Event();
                        event.setId(doc.getId());
                        event.setName(getString(doc, "name"));
                        event.setDescription(getString(doc, "description"));
                        event.setDate(getString(doc, "date"));
                        event.setOrganizerId(getString(doc, "organizerId"));
                        list.add(event);
                    }
                    adapter.setEvents(list);
                    eventsEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    if (currentUserId != null) {
                        loadJoinedEventIds(joined -> adapter.setJoinedEventIds(joined));
                    } else {
                        adapter.setJoinedEventIds(new HashSet<>());
                    }
                })
                .addOnFailureListener(e -> {
                    adapter.setEvents(new ArrayList<>());
                    eventsEmpty.setVisibility(View.VISIBLE);
                    eventsEmpty.setText("Could not load events");
                });
    }

    // Load the joined event IDs from the database
    private void loadJoinedEventIds(OnJoinedLoadedListener listener) {

        if (currentUserId == null) {
            listener.onLoaded(new HashSet<>());
            return;
        }
        // load the joined event IDs from the database
        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(snap -> {
                    Set<String> ids = new HashSet<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Object o = doc.get("eventId");
                        String eventId = o != null ? o.toString() : "";
                        if (!eventId.isEmpty()) ids.add(eventId);
                    }
                    listener.onLoaded(ids);
                })
                .addOnFailureListener(e -> listener.onLoaded(new HashSet<>()));
    }
    // Interface for the joined event IDs loaded listener
    private interface OnJoinedLoadedListener {
        void onLoaded(Set<String> joinedEventIds);
    }
    // Get the string value from the document snapshot
    private static String getString(QueryDocumentSnapshot doc, String field) {
        Object o = doc.get(field);
        return o != null ? o.toString() : "";
    }
}
