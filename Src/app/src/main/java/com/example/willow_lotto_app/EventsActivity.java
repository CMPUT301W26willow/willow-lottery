package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventsActivity extends AppCompatActivity {

    private RecyclerView eventsRecycler;
    private TextView eventsEmpty;
    private EventsAdapter adapter;
    private FirebaseFirestore db;

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

    private void loadEvents() {
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
                })
                .addOnFailureListener(e -> {
                    adapter.setEvents(new ArrayList<>());
                    eventsEmpty.setVisibility(View.VISIBLE);
                    eventsEmpty.setText("Could not load events");
                });
    }

    private static String getString(QueryDocumentSnapshot doc, String field) {
        Object o = doc.get(field);
        return o != null ? o.toString() : "";
    }
}
