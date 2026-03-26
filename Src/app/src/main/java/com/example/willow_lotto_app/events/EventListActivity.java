package com.example.willow_lotto_app.events;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;

/**
 * This is an entry point activity that displays a scrollable list of all events.
 * It handles fetching event summaries from Firestore and navigating to details.
 * @author Jasdeep Cheema
 * @version 1.0
 * @since 12/03/2026
 */
public class EventListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListView eventList;

    // Parallel lists to track both display names and database IDs for Intents
    private ArrayList<String> eventNames = new ArrayList<>();
    private ArrayList<String> eventIds = new ArrayList<>();

    /**
     * This initializes the activity, sets the UI layout, and triggers the loading of events.
     * @param savedInstanceState the previously saved state of the instance
     */
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
        // Access the 'events' collection and perform a one-time fetch of all documents
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Clear existing data to prevent duplicate entries if the list is reloaded
                    eventNames.clear();
                    eventIds.clear();

                    // Iterate through the resulting documents to extract event data
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Retrieve the 'name' field; default to 'Unnamed Event' if the field is null
                        String name = doc.getString("name");
                        if (name == null) name = "Unnamed Event";

                        // Add the event name to the UI list and the document ID to the reference list
                        eventNames.add(name);
                        eventIds.add(doc.getId());
                    }

                    // Bind the extracted data to the ListView using a standard layout
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1,
                            eventNames);
                    eventList.setAdapter(adapter);

                    // Map the user's selection to the corresponding document ID for navigation
                    eventList.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedEventId = eventIds.get(position);

                        Intent intent = new Intent(EventListActivity.this, EventActivity.class);
                        // Pass the document ID as an extra to allow the next Activity to query Firestore
                        intent.putExtra("eventId", selectedEventId);
                        startActivity(intent);
                    });
                });
    }
}