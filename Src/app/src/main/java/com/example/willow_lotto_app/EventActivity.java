package com.example.willow_lotto_app;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Main Activity for Entrants to view event details,
 * see waitlist counts, and respond to lottery invitations.
 */
public class EventActivity extends AppCompatActivity {


    TextView waitingCount, lotteryInfo;
    Button acceptButton, declineButton;


    private FirebaseFirestore db;
    private String myUserId = "user1"; // Mock ID for current entrant
    private String eventId = "event1"; // Mock ID for current event


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);


        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();


        // Bind UI components
        waitingCount = findViewById(R.id.waiting_count);
        lotteryInfo = findViewById(R.id.lottery_info);
        acceptButton = findViewById(R.id.accept_invite);
        declineButton = findViewById(R.id.decline_invite);


        // US 01.05.05: Inform user about the selection criteria
        lotteryInfo.setText("Criteria: Random draw from waiting list.");


        // Initialize buttons as hidden until a user is "selected"
        acceptButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);


        // Start listening for real-time updates from the database
        setupFirestoreListener();


        // Handle user accepting the lottery win
        acceptButton.setOnClickListener(v -> updateUserStatus("accepted"));


        // Handle user declining the lottery win
        declineButton.setOnClickListener(v -> {
            updateUserStatus("declined");
            // US 01.05.01: Automatically pick a replacement if someone declines

            //drawLottery();(add once draw lottery is implemented)
        });
    }


    /**
     * Listen for changes in the 'entrants' sub-collection.
     * Updates waitlist count and toggles invite buttons.
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


                            // Build list of everyone currently waiting
                            if ("waiting".equals(status)) waitlist.add(doc.getId());


                            // US 01.05.02 & 01.05.03: Show response UI if THIS user is chosen
                            if (myUserId.equals(doc.getId()) && "selected".equals(status)) {
                                acceptButton.setVisibility(View.VISIBLE);
                                declineButton.setVisibility(View.VISIBLE);
                                lotteryInfo.setText("Status: YOU ARE CHOSEN!");
                            }
                        }


                        // US 01.05.04: Real-time update of the total number of waiting entrants
                        waitingCount.setText("Waiting List: " + waitlist.size());
                        Log.d("Lottery", "Waiting list updated: " + waitlist);
                    }
                });
    }


    /**
     * Updates the 'status' field for the current user in Firestore.
     */
    private void updateUserStatus(String status) {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(myUserId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Status updated: " + status))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to update status", e));


        // Hide buttons once response is submitted
        acceptButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);
        lotteryInfo.setText("Status: " + status);
    }


}
