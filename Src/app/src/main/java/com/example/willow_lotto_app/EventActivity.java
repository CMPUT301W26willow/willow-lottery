package com.example.willow_lotto_app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the specific interaction for a single event.
 * Manages the "Waiting List" count and the selection response UI.
 * @author Jasdeep Cheema
 * @version 1.0
 * @since 12/03/2026
 */
public class EventActivity extends AppCompatActivity {

    TextView waitingCount, lotteryInfo, userStatusText;
    Button acceptButton, declineButton;

    private FirebaseFirestore db;
    private String myUserId = "user1"; // Simulated unique ID for the logged-in user
    private String eventId;

    /**
     * This initializes the activity, sets up the toolbar, UI elements, and listeners.
     * @param savedInstanceState the previously saved state of the instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_waitlist);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Details");
        }

        db = FirebaseFirestore.getInstance();

        waitingCount = findViewById(R.id.waiting_count);
        lotteryInfo = findViewById(R.id.lottery_info);
        userStatusText = findViewById(R.id.user_status_text);
        acceptButton = findViewById(R.id.accept_invite);
        declineButton = findViewById(R.id.decline_invite);

        eventId = getIntent().getStringExtra("eventId");

        if (eventId == null) {
            Log.e("EventActivity", "Event ID not received");
            finish();
            return;
        }

        lotteryInfo.setText("Criteria: Random draw from waiting list.");
        acceptButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);

        setupFirestoreListener();

        acceptButton.setOnClickListener(v -> updateUserStatus("accepted"));

        declineButton.setOnClickListener(v -> {
            updateUserStatus("declined");
        });
    }

    /**
     * Listens to the 'entrants' sub-collection for live changes.
     * Updates the UI count and alerts the user if their status changes to 'selected'.
     */
    private void setupFirestoreListener() {
        // Attach a real-time listener to the 'entrants' sub-collection of the current event
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .addSnapshotListener((value, error) -> {
                    // Check if the database connection failed or permissions were denied
                    if (error != null) {
                        Log.e("Firestore", "Listener error", error);
                        return;
                    }

                    // Process the snapshot if data is returned from the server
                    if (value != null) {
                        List<String> waitlist = new ArrayList<>();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            // Inspect the status field of each document in the sub-collection
                            String status = doc.getString("status");

                            // Populate a local list with document IDs representing users in 'waiting' status
                            if ("waiting".equals(status))
                                waitlist.add(doc.getId());

                            // Compare document ID with current user ID to trigger status-specific UI changes
                            if (myUserId.equals(doc.getId())) {
                                updateUIForUserStatus(status);
                            }
                        }
                        // Refresh UI with the current total of documents filtered by 'waiting' status
                        waitingCount.setText("Waiting List: " + waitlist.size());
                    }
                });
    }

    /**
     * Updates the UI based on the current user's status from Firestore.
     * @param status String value representing user status: "waiting", "selected", "accepted", "declined"
     */
    private void updateUIForUserStatus(String status) {
        if (status == null) status = "waiting";

        userStatusText.setText("Your Status: " + status.toUpperCase());

        if ("selected".equals(status)) {
            acceptButton.setVisibility(View.VISIBLE);
            declineButton.setVisibility(View.VISIBLE);
            userStatusText.setText("Status: YOU ARE CHOSEN!");
        } else {
            acceptButton.setVisibility(View.GONE);
            declineButton.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the status field for the current user in the database.
     * @param status String value to update in database: "accepted", "declined", etc.
     */
    private void updateUserStatus(String status) {
        // Target the specific user document within the entrants sub-collection for the current event
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(myUserId)
                // Execute an atomic update on the 'status' field
                .update("status", status)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Status updated"));

        // Update local UI immediately to provide immediate feedback to the user
        acceptButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);
        userStatusText.setText("Status: " + status.toUpperCase());
    }
}