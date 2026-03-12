package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class OrganizerDashboardActivity extends AppCompatActivity {

    private ListView waitingListView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> entrantNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        Button createEventButton = findViewById(R.id.createEventButton);
        Button backButton = findViewById(R.id.backToProfileButton);

        createEventButton.setOnClickListener(v -> startActivity(new Intent(this, CreateEventActivity.class)));
        backButton.setOnClickListener(v -> finish());

        waitingListView = findViewById(R.id.waitingListView);
        entrantNames = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, entrantNames);
        waitingListView.setAdapter(adapter);

        loadWaitingList();
    }

    private void loadWaitingList() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document("event1")
                .collection("waitingList")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    entrantNames.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        if (name != null) {
                            entrantNames.add(name);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (entrantNames.isEmpty()) {
                        Toast.makeText(this, "No entrants yet.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting waiting list", e);
                    Toast.makeText(this, "Failed to load waiting list.", Toast.LENGTH_SHORT).show();
                });
    }
}