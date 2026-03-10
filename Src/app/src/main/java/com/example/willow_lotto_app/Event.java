package com.example.willow_lotto_app;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Event {

    private List<String> waitingList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String eventId = "sample_event_id";

    // Add current user to the waiting list
    public void addToWaitingList(String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "waiting");

        db.collection("events").document(eventId)
                .collection("entrants").document(userId).set(data)
                .addOnSuccessListener(aVoid -> System.out.println("User added to waiting list: " + userId))
                .addOnFailureListener(e -> System.out.println("Error adding user: " + e.getMessage()));
    }

    // Update user's status (accepted, declined, selected)
    public void updateStatus(String userId, String status) {
        db.collection("events").document(eventId)
                .collection("entrants").document(userId).update("status", status)
                .addOnSuccessListener(aVoid -> System.out.println("User " + userId + " status updated to " + status))
                .addOnFailureListener(e -> System.out.println("Error updating status: " + e.getMessage()));
    }

    // Set local waiting list for UI updates
    public void setLocalWaitingList(List<String> list) {
        this.waitingList = list;
    }

    public int getWaitingCount() {
        return waitingList.size();
    }
}