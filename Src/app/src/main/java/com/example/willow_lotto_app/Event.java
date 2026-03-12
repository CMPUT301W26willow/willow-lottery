package com.example.willow_lotto_app;


import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;


/**
 * Represent an Event and handles data management for its waiting list.
 */
public class Event {


    private List<String> waitingList; // List of user IDs on the waitlist
    private FirebaseFirestore db;


    private FirebaseFirestore getDb(){
        if(db == null) db = FirebaseFirestore.getInstance();
        return db;
    }


    // Helper method to update status from anywhere in the app
    public void updateStatus(String eventId, String userId, String status){
        getDb().collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                .update("status", status);
    }


    public void setLocalWaitingList(List<String> list){
        this.waitingList = list;
    }


    public int getWaitingCount(){

        return (waitingList != null) ? waitingList.size() : 0;
    }
}
