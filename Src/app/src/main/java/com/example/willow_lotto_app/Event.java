package com.example.willow_lotto_app;


import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;


/**
 * Data model class representing an Event.
 * Provides a clean interface for managing waitlist data and database updates.
 */
public class Event {


    private List<String> waitingList; // Local cache of user IDs
    private FirebaseFirestore db;


    /**
     * Lazy-loading getter for the Firestore instance.
     */
    private FirebaseFirestore getDb(){
        if(db == null) db = FirebaseFirestore.getInstance();
        return db;
    }


    /**
     * Global method to update an entrant's status from outside the Activity.
     */
    public void updateStatus(String eventId, String userId, String status){
        getDb().collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .update("status", status);
    }


    public void setLocalWaitingList(List<String> list){
        waitingList = list;
    }


    /**
     * Logic to safely return the size of the waitlist.
     */
    public int getWaitingCount(){
        return waitingList != null ? waitingList.size() : 0;
    }
}
