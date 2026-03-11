package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;
    private RecyclerView homeEventsRecycler;
    private EventsAdapter adapter;
    private FirebaseFirestore db;

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

        db = FirebaseFirestore.getInstance();
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

    private void loadEvents() {
        db.collection("events")
                .limit(10)
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
                });
    }

    private static String getString(QueryDocumentSnapshot doc, String field) {
        Object o = doc.get(field);
        return o != null ? o.toString() : "";
    }
}