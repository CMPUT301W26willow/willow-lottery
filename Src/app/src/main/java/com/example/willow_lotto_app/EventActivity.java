package com.example.willow_lotto_app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class EventActivity extends AppCompatActivity {

    private TextView waitingCount, lotteryInfo;
    private Button joinButton, acceptButton, declineButton;

    private Event event = new Event();
    private LotteryManager lottery = new LotteryManager();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String myUserId;
    private final String eventId = "sample_event_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        myUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        waitingCount = findViewById(R.id.waiting_count);
        lotteryInfo = findViewById(R.id.lottery_info);
        joinButton = findViewById(R.id.join_waiting);
        acceptButton = findViewById(R.id.accept_invite);
        declineButton = findViewById(R.id.decline_invite);

        // US 01.05.05: Show lottery guidelines
        lotteryInfo.setText("Criteria: Random draw from waiting list.");

        // US 01.05.04: Real-time waiting list listener
        db.collection("events").document(eventId)
                .collection("entrants")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<String> tempWaitlist = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String status = doc.getString("status");

                            // Only include waiting users in local list
                            if ("waiting".equals(status)) tempWaitlist.add(doc.getId());

                            // Check if current user is selected
                            if (doc.getId().equals(myUserId) && "selected".equals(status)) {
                                acceptButton.setVisibility(View.VISIBLE);
                                declineButton.setVisibility(View.VISIBLE);
                                lotteryInfo.setText("Status: YOU ARE CHOSEN!");
                            }
                        }
                        event.setLocalWaitingList(tempWaitlist);
                        waitingCount.setText("Waiting List: " + event.getWaitingCount());
                        Log.d("Lottery", "Waiting list updated: " + tempWaitlist);
                    }
                });

        // Join waiting list
        joinButton.setOnClickListener(v -> {
            event.addToWaitingList(myUserId);
            joinButton.setEnabled(false); // Disable to prevent multiple joins
        });

        // Accept invitation
        acceptButton.setOnClickListener(v -> {
            event.updateStatus(myUserId, "accepted");
            lotteryInfo.setText("Status: Accepted");
            acceptButton.setVisibility(View.GONE);
            declineButton.setVisibility(View.GONE);
        });

        // Decline invitation and trigger redraw
        declineButton.setOnClickListener(v -> {
            event.updateStatus(myUserId, "declined");
            lotteryInfo.setText("Status: Declined");
            acceptButton.setVisibility(View.GONE);
            declineButton.setVisibility(View.GONE);

            // US 01.05.01: Trigger replacement lottery
            lottery.drawLottery(eventId);
        });
    }
}