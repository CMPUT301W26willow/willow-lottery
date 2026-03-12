package com.example.willow_lotto_app;


import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;


/**
 * Entry point activity that displays a scrollable list of all events.
 * Handles fetching event summaries from Firestore and navigating to details.
 */
public class EventListActivity extends AppCompatActivity {


    private FirebaseFirestore db;
    private ListView eventList;


    // Parallel lists to track both display names and database IDs for Intents
    private ArrayList<String> eventNames = new ArrayList<>();
    private ArrayList<String> eventIds = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);


        db = FirebaseFirestore.getInstance();
        eventList = findViewById(R.id.event_list);


        loadEvents();
    }


    /**
     * Retrieves all documents from the "events" collection.
     * Populates the ListView and sets up the click listener for navigation.
     */
    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventNames.clear();
                    eventIds.clear();


                    // Iterate through documents to build the list data
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name == null) name = "Unnamed Event";


                        eventNames.add(name);
                        eventIds.add(doc.getId()); // Store ID to pass to next activity
                    }


                    // Standard adapter to bridge the ArrayList and the UI ListView
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1,
                            eventNames);
                    eventList.setAdapter(adapter);


                    // Logic to open the specific event details when a list item is tapped
                    eventList.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedEventId = eventIds.get(position);


                        Intent intent = new Intent(EventListActivity.this, EventActivity.class);
                        // Pass the unique Firestore ID so EventActivity knows which data to load
                        intent.putExtra("eventId", selectedEventId);
                        startActivity(intent);
                    });
                });
    }
}
