package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.events.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lists every event created by the signed-in organizer (excluding soft-deleted documents).
 * Each row can open {@link OrganizerDashboardActivity} or run the same entrant CSV export as the dashboard.
 */
public class OrganizerMyEventsActivity extends AppCompatActivity implements OrganizerMyEventsAdapter.Listener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private OrganizerMyEventsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_my_events);

        recyclerView = findViewById(R.id.organizer_my_events_recycler);
        emptyView = findViewById(R.id.organizer_my_events_empty);
        Button back = findViewById(R.id.organizer_my_events_back);
        back.setOnClickListener(v -> finish());

        adapter = new OrganizerMyEventsAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadMyEvents();
    }

    private void loadMyEvents() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, R.string.organizer_my_events_sign_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("organizerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Event> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Boolean isDeleted = doc.getBoolean("isDeleted");
                        if (isDeleted != null && isDeleted) {
                            continue;
                        }
                        Event e = new Event();
                        e.setId(doc.getId());
                        e.setName(doc.getString("name"));
                        e.setDescription(doc.getString("description"));
                        e.setDate(doc.getString("date"));
                        e.setOrganizerId(doc.getString("organizerId"));
                        list.add(e);
                    }
                    Collections.sort(list, BY_DATE_DESC_THEN_NAME);
                    adapter.setEvents(list);
                    boolean showEmpty = list.isEmpty();
                    emptyView.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.organizer_my_events_load_failed, Toast.LENGTH_SHORT).show());
    }

    private static final Comparator<Event> BY_DATE_DESC_THEN_NAME = (a, b) -> {
        String da = a.getDate() != null ? a.getDate() : "";
        String db = b.getDate() != null ? b.getDate() : "";
        int c = db.compareTo(da);
        if (c != 0) {
            return c;
        }
        String na = a.getName() != null ? a.getName() : "";
        String nb = b.getName() != null ? b.getName() : "";
        return na.compareToIgnoreCase(nb);
    };

    @Override
    public void onOpenDashboard(Event event) {
        if (event.getId() == null || event.getId().isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, OrganizerDashboardActivity.class);
        intent.putExtra(OrganizerDashboardActivity.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    @Override
    public void onExportCsv(Event event) {
        if (event.getId() == null || event.getId().isEmpty()) {
            return;
        }
        OrganizerEntrantExportHelper.exportEntrantsCsv(this, event.getId());
    }
}
