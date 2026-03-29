/**
 * CancelledEntrantsActivity.java
 *
 * Author: Mehr Dhanda
 *
 * Displays a list of entrants who have cancelled their registration
 * for a specific event. Fetches cancelled registrations from Firebase
 * Firestore and displays entrant names in a ListView.
 *
 * Role: Controller in the MVC pattern.
 *
 * Outstanding issues:
 * - Event ID must be passed via Intent from OrganizerDashboardActivity.
 */
package com.example.willow_lotto_app.events;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays the list of cancelled entrants for an event.
 */
public class CancelledEntrantsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    private ListView cancelledListView;
    private TextView cancelledEmpty;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> entrantNames;
    private String eventId;
    private RegistrationStore registrationRepository;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancelled_entrants);

        db = FirebaseFirestore.getInstance();
        registrationRepository = new RegistrationStore();

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        cancelledListView = findViewById(R.id.cancelledListView);
        cancelledEmpty = findViewById(R.id.cancelled_empty);
        entrantNames = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, entrantNames);
        cancelledListView.setAdapter(adapter);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        loadCancelledEntrants();
    }

    /**
     * Loads the list of cancelled entrants from Firestore using the registration repository.
     * Fetches all registrations with CANCELLED status for the current event,
     * then loads each entrant's name from the users collection.
     */
    private void loadCancelledEntrants() {
        registrationRepository.getRegistrationsForEventByStatus(
                eventId,
                RegistrationStatus.CANCELLED.getValue(),
                new RegistrationStore.RegistrationListCallback() {
                    @Override
                    public void onSuccess(List<Registration> registrations) {
                        if (registrations.isEmpty()) {
                            cancelledEmpty.setVisibility(View.VISIBLE);
                            cancelledListView.setVisibility(View.GONE);
                            return;
                        }

                        List<String> userIds = new ArrayList<>();
                        for (Registration registration : registrations) {
                            userIds.add(registration.getUserId());
                        }
                        loadUserNames(userIds);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("CancelledEntrants", "Failed to load cancelled entrants", e);
                        Toast.makeText(CancelledEntrantsActivity.this, "Failed to load cancelled entrants.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Loads user names from Firestore for a list of user IDs.
     * Falls back to email, then userId if name is not available.
     *
     * @param userIds List of user IDs to fetch names for.
     */
    private void loadUserNames(List<String> userIds) {
        entrantNames.clear();
        if (userIds.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        final int[] completed = {0};
        for (String userId : userIds) {
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");

                        if (name != null && !name.trim().isEmpty()) {
                            entrantNames.add(name);
                        } else if (email != null && !email.trim().isEmpty()) {
                            entrantNames.add(email);
                        } else {
                            entrantNames.add(userId);
                        }

                        completed[0]++;
                        if (completed[0] == userIds.size()) {
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        entrantNames.add(userId);
                        completed[0]++;
                        if (completed[0] == userIds.size()) {
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }
}