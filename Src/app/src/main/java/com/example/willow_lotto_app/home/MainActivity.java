package com.example.willow_lotto_app.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.events.Event;
import com.example.willow_lotto_app.events.EventDetailActivity;
import com.example.willow_lotto_app.events.EventsActivity;
import com.example.willow_lotto_app.events.EventsAdapter;
import com.example.willow_lotto_app.notification.NotificationActivity;
import com.example.willow_lotto_app.profile.ProfileActivity;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.WaitlistCountLoader;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Home screen: lists public events from Firestore, search/filters, join/leave waitlist.
// Bottom nav goes to Events, Notifications, Profile. Cards are EventsAdapter.
public class MainActivity extends AppCompatActivity {

    private static final String REGISTRATIONS_COLLECTION = "registrations";

    BottomNavigationView bottomNav;
    private RecyclerView homeEventsRecycler;
    private TextView homeEventsEmpty;
    private View homeFilterScroll;
    private EditText homeSearchInput;
    private EventsAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Event list
        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        homeEventsRecycler = findViewById(R.id.home_events_recycler);
        homeEventsEmpty = findViewById(R.id.home_events_empty);
        homeFilterScroll = findViewById(R.id.home_filter_scroll);
        adapter = new EventsAdapter();
        homeEventsRecycler.setLayoutManager(new LinearLayoutManager(this));
        homeEventsRecycler.setAdapter(adapter);

        // Search box: filter adapter as user types
        homeSearchInput = findViewById(R.id.home_search_input);
        homeSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s != null ? s.toString() : "");
                updateHomeEmptyState(getString(R.string.home_no_events_yet));
            }
        });

        // Filter chips: open spots, availability, date, clear (re-applies search text)
        Button homeFilterOpen = findViewById(R.id.home_filter_open);
        Button homeFilterAvailable = findViewById(R.id.home_filter_available);
        Button homeFilterDate = findViewById(R.id.home_filter_date);
        Button homeFilterClear = findViewById(R.id.home_filter_clear);
        homeFilterOpen.setOnClickListener(v -> {
            adapter.filterByOpenStatus();
            updateHomeEmptyState(getString(R.string.home_no_open_events));
        });
        homeFilterAvailable.setOnClickListener(v -> {
            adapter.filterByAvailability();
            updateHomeEmptyState(getString(R.string.home_no_spots_events));
        });
        homeFilterDate.setOnClickListener(v -> {
            adapter.filterByDate();
            updateHomeEmptyState(getString(R.string.home_no_events_yet));
        });
        homeFilterClear.setOnClickListener(v -> {
            adapter.clearFilters();
            String q = homeSearchInput.getText() != null ? homeSearchInput.getText().toString() : "";
            adapter.filter(q);
            updateHomeEmptyState(getString(R.string.home_no_events_yet));
        });

        // Toggle filter row visibility
        findViewById(R.id.home_filter_btn).setOnClickListener(v -> {
            boolean show = homeFilterScroll.getVisibility() != View.VISIBLE;
            homeFilterScroll.setVisibility(show ? View.VISIBLE : View.GONE);
        });

        // Signed-in user id for registrations and joined state
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        adapter.setCurrentUserId(currentUserId);
        // Join: write registrations/{eventId_userId} waitlisted; Leave: delete that doc
        adapter.setOnJoinLeaveListener(new EventsAdapter.OnJoinLeaveListener() {
            @Override
            public void onJoin(Event event) {
                if (currentUserId == null) {
                    return;
                }
                if (isEventClosedForJoining(event)) {
                    Toast.makeText(MainActivity.this,
                            R.string.event_join_closed_message,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Integer lim = event.getLimit();
                if (lim != null && lim > 0 && event.getWaitlistDisplayCount() >= lim) {
                    Toast.makeText(MainActivity.this, R.string.waitlist_full_message, Toast.LENGTH_SHORT).show();
                    return;
                }
                String docId = event.getId() + "_" + currentUserId;
                Map<String, Object> reg = new HashMap<>();
                reg.put("eventId", event.getId());
                reg.put("userId", currentUserId);
                reg.put("status", RegistrationStatus.WAITLISTED.getValue());
                db.collection(REGISTRATIONS_COLLECTION).document(docId).set(reg)
                        .addOnSuccessListener(aVoid -> {
                            int cur = event.getWaitlistedRegistrationCount();
                            if (cur >= 0) {
                                event.setWaitlistedRegistrationCount(cur + 1);
                            }
                            adapter.setEventJoined(event.getId(), true);
                        })
                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Could not join event", Toast.LENGTH_SHORT).show());
            }
            
            @Override
            public void onLeave(Event event) {
                if (currentUserId == null) {
                    return;
                }
                String docId = event.getId() + "_" + currentUserId;
                db.collection(REGISTRATIONS_COLLECTION).document(docId).delete()
                        .addOnSuccessListener(aVoid -> {
                            int cur = event.getWaitlistedRegistrationCount();
                            if (cur >= 0) {
                                event.setWaitlistedRegistrationCount(Math.max(0, cur - 1));
                            }
                            adapter.setEventJoined(event.getId(), false);
                        })
                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Could not leave event", Toast.LENGTH_SHORT).show());
            }
        });
        // Open event detail
        adapter.setOnEventClickListener(event -> {
            Intent intent = new Intent(MainActivity.this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
        });
        loadEvents();

        // Bottom navigation
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

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    // Pull events from Firestore, skip private/deleted, fill adapter, waitlist counts, joined ids
    private void loadEvents() {

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> list = new ArrayList<>();
                    // Map each doc to Event; registeredUsers may be stored as List or other shape
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isDeleted = doc.getBoolean("isDeleted");
                        if (isDeleted != null && isDeleted) {
                            continue;
                        }
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
                        List<String> registeredUsers =
                                doc.get("registeredUsers") instanceof List
                                        ? castStringList(doc.get("registeredUsers"))
                                        : readStringList(doc, "registeredUsers");
                        event.setRegisteredUsers(registeredUsers);
                        WaitlistCountLoader.applyWaitlistLimitFromDoc(doc, event);
                        list.add(event);
                    }
                    adapter.setEvents(list);
                    String q = homeSearchInput.getText() != null ? homeSearchInput.getText().toString() : "";
                    adapter.filter(q);
                    updateHomeEmptyState(getString(R.string.home_no_events_yet));
                    // Refresh per-event waitlist numbers from Firestore
                    WaitlistCountLoader.loadWaitlistedCounts(db, list,
                            () -> runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                updateHomeEmptyState(getString(R.string.home_no_events_yet));
                            }));
                    // Which events this user already joined (registration docs)
                    if (currentUserId != null) {
                        loadJoinedEventIds(joined -> adapter.setJoinedEventIds(joined));
                    } else {
                        adapter.setJoinedEventIds(new HashSet<>());
                    }
                })
                .addOnFailureListener(e -> {
                    adapter.setEvents(new ArrayList<>());
                    String msg = "Could not load events";
                    homeEventsEmpty.setText(msg);
                    homeEventsEmpty.setVisibility(View.VISIBLE);
                    homeEventsRecycler.setVisibility(View.GONE);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                });

    }

    // Show empty message vs recycler depending on filtered item count
    private void updateHomeEmptyState(String defaultEmptyMessage) {
        if (homeEventsEmpty == null) {
            return;
        }
        if (adapter.getItemCount() == 0) {
            homeEventsEmpty.setText(defaultEmptyMessage);
            homeEventsEmpty.setVisibility(View.VISIBLE);
            homeEventsRecycler.setVisibility(View.GONE);
        } else {
            homeEventsEmpty.setVisibility(View.GONE);
            homeEventsRecycler.setVisibility(View.VISIBLE);
        }
    }

    // Firestore sometimes returns a raw List; normalize to List<String>
    private static List<String> castStringList(Object o) {
        List<String> out = new ArrayList<>();
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

    // registrations where userId matches: collect eventId strings
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

    // Safe string from Firestore field (null becomes "")
    private static String getString(QueryDocumentSnapshot doc, String field) {
        Object o = doc.get(field);
        return o != null ? o.toString() : "";
    }

    // Build string list from a list-typed field on the snapshot
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

    // Block join if registration end or event date is before today (yyyy-MM-dd string compare)
    private static boolean isEventClosedForJoining(Event event) {
        if (event == null) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = sdf.format(new Date());
        String registrationEnd = event.getRegistrationEnd();
        if (registrationEnd != null && !registrationEnd.trim().isEmpty()) {
            if (registrationEnd.trim().compareTo(today) < 0) {
                return true;
            }
        }
        String eventDate = event.getDate();
        if (eventDate != null && !eventDate.trim().isEmpty()) {
            if (eventDate.trim().compareTo(today) < 0) {
                return true;
            }
        }
        return false;
    }
}