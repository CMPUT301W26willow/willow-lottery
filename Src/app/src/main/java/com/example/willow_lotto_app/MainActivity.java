package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.events.Event;
import com.example.willow_lotto_app.events.EventDetailActivity;
import com.example.willow_lotto_app.events.EventsActivity;
import com.example.willow_lotto_app.events.EventsAdapter;
import com.example.willow_lotto_app.notification.NotificationActivity;
import com.example.willow_lotto_app.registration.RegistrationStatus;
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
 * Home screen for entrants.
 *
 * Responsibilities:
 * - Implements 01.01.03 "View events available to join" by showing a
 *   limited feed of events the user can browse.
 * - Wires up bottom navigation to Events, Notifications, and Profile.
 * - Delegates rendering of individual cards to {@link EventsAdapter}.
 */
public class MainActivity extends AppCompatActivity {

    private static final String REGISTRATIONS_COLLECTION = "registrations";

    BottomNavigationView bottomNav;
    private RecyclerView homeEventsRecycler;
    private EventsAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        homeEventsRecycler = findViewById(R.id.home_events_recycler);
        adapter = new EventsAdapter();
        homeEventsRecycler.setLayoutManager(new LinearLayoutManager(this));
        homeEventsRecycler.setAdapter(adapter);

        EditText homeSearch = findViewById(R.id.home_search_input);
        homeSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s != null ? s.toString() : "");
            }
        });
        findViewById(R.id.home_filter_btn).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, EventsActivity.class)));

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
                reg.put("status", RegistrationStatus.WAITLISTED.getValue());
                db.collection(REGISTRATIONS_COLLECTION).document(docId).set(reg)
                        .addOnSuccessListener(aVoid -> {
                            adapter.setEventJoined(event.getId(), true);
                        })
                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Could not join event", Toast.LENGTH_SHORT).show());
            }
            
            @Override
            public void onLeave(Event event) {
                if (currentUserId == null) return;
                String docId = event.getId() + "_" + currentUserId;
                db.collection(REGISTRATIONS_COLLECTION).document(docId).delete()
                        .addOnSuccessListener(aVoid -> adapter.setEventJoined(event.getId(), false))
                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Could not leave event", Toast.LENGTH_SHORT).show());
            }
        });
        adapter.setOnEventClickListener(event -> {
            Intent intent = new Intent(MainActivity.this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
        });
        loadEvents();

        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_home) {
                return true;
            }

            if (item.getItemId() == R.id.nav_events) {
                startActivity(new Intent(this, EventsActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                return true;
            }

            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }

            return false;
        });
    }

    // Load events (limit 10) then current user's joined IDs.
    private void loadEvents() {

        db.collection("events")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isDeleted = doc.getBoolean("isDeleted");
                        if (isDeleted != null && isDeleted) {
                            continue;
                        }
                        // added to not run loop if event is private
                        Boolean isPrivate = doc.getBoolean("isPrivate");
                        if (isPrivate != null && isPrivate) {
                            continue;
                        }
                        Event event = new Event();
                        event.setId(doc.getId());
                        event.setName(getString(doc, "name"));
                        event.setDescription(getString(doc, "description"));
                        event.setDate(getString(doc, "date"));
                        event.setOrganizerId(getString(doc, "organizerId"));
                        event.setPosterUri(getString(doc, "posterUri"));
                        event.setRegistrationStart(getString(doc, "registrationStart"));
                        event.setRegistrationEnd(getString(doc, "registrationEnd"));
                        event.setRegisteredUsers(readStringList(doc, "registeredUsers"));
                        list.add(event);
                    }
                    adapter.setEvents(list);
                    if (currentUserId != null) {
                        loadJoinedEventIds(joined -> adapter.setJoinedEventIds(joined));
                    } else {
                        adapter.setJoinedEventIds(new HashSet<>());
                    }
                });

    }

    // Query registrations where userId = currentUser → set of eventIds.
    private void loadJoinedEventIds(OnJoinedLoadedListener listener) {
        if (currentUserId == null) {
            listener.onLoaded(new HashSet<>());
            return;
        }
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

    private interface OnJoinedLoadedListener {
        void onLoaded(Set<String> joinedEventIds);
    }

    private static String getString(QueryDocumentSnapshot doc, String field) {
        Object o = doc.get(field);
        return o != null ? o.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private static List<String> readStringList(QueryDocumentSnapshot doc, String field) {
        List<String> out = new ArrayList<>();
        Object o = doc.get(field);
        if (!(o instanceof List)) {
            return out;
        }
        for (Object x : (List<?>) o) {
            if (x != null) {
                out.add(x.toString());
            }
        }
        return out;
    }
}