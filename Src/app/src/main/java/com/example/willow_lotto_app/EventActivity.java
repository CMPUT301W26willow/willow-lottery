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
 */
public class EventActivity extends AppCompatActivity {


    // UI components for displaying event details and user status
    TextView waitingCount, lotteryInfo, userStatusText;
    Button acceptButton, declineButton;


    private FirebaseFirestore db;
    private String myUserId = "user1"; // Simulated unique ID for the logged-in user
    private String eventId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_waitlist);


        //Toolbar & Back Arrow Setup
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


        // Retrieve ID passed from EventListActivity via Intent
        eventId = getIntent().getStringExtra("eventId");


        if (eventId == null) {
            Log.e("EventActivity", "Event ID not received");
            finish(); // Close activity if no ID is present to avoid crashes
            return;
        }


        // US 01.05.05: Set static text informing the user of the draw criteria
        lotteryInfo.setText("Criteria: Random draw from waiting list.");
        acceptButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);


        // Start the real-time sync with Firestore to monitor status and waitlist count
        setupFirestoreListener();


        // US 01.05.02: User clicks to accept their invitation
        acceptButton.setOnClickListener(v -> updateUserStatus("accepted"));


        // US 01.05.03: User clicks to decline their invitation
        declineButton.setOnClickListener(v -> {
            updateUserStatus("declined");
            // Trigger a redraw to replace the entrant who declined (US 01.05.01)

            //drawLottery(); (need to be implement in organizer story)
        });
    }


    /**
     * Listens to the 'entrants' sub-collection for live changes.
     * Updates the UI count and alerts the user if their status changes to 'selected'.
     */
    private void setupFirestoreListener() {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Listener error", error);
                        return;
                    }


                    if (value != null) {
                        List<String> waitlist = new ArrayList<>();


                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String status = doc.getString("status");


                            // US 01.05.04: Filter documents to count current waiting entrants
                            if ("waiting".equals(status))
                                waitlist.add(doc.getId());


                            // Logic to "Notify" the user by showing action buttons and status updates
                            if (myUserId.equals(doc.getId())) {
                                updateUIForUserStatus(status);
                            }
                        }
                        // Update UI with the live count of people in the queue
                        waitingCount.setText("Waiting List: " + waitlist.size());
                    }
                });
    }


    /**
     * Updates the UI based on the current user's status from Firestore.
     *
     * @param status String value: "waiting", "selected", "accepted", "declined"
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
     *
     * @param status String value: "accepted", "declined", etc.
     */
    private void updateUserStatus(String status) {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(myUserId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Status updated"));


        // Clean up UI locally after response is sent
        acceptButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);
        userStatusText.setText("Status: " + status.toUpperCase());
    }
}
