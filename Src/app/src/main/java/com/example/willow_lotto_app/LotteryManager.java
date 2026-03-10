package com.example.willow_lotto_app;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Random;

public class LotteryManager {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Draw a random replacement from the waiting list
    public void drawLottery(String eventId) {
        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> pool = querySnapshot.getDocuments();
                    if (!pool.isEmpty()) {
                        int index = new Random().nextInt(pool.size());
                        String winnerId = pool.get(index).getId();

                        // Mark selected winner in Firestore
                        db.collection("events").document(eventId)
                                .collection("entrants").document(winnerId)
                                .update("status", "selected")
                                .addOnSuccessListener(aVoid -> System.out.println("Replacement selected: " + winnerId));
                    } else {
                        System.out.println("No users left in waiting list for replacement.");
                    }
                });
    }
}