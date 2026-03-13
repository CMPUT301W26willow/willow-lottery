package com.example.willow_lotto_app;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

/**
 * Data model class representing an Event.
 * Provides a clean interface for managing waitlist data and database updates.
 * @author Jasdeep Cheema
 * @version 1.0
 * @since 12/03/2026
 */
public class EventWaitlist {

    private List<String> waitingList; // Local cache of user IDs
    private FirebaseFirestore db;

    /**
     * Lazy-loading getter for the Firestore instance.
     * @return the active FirebaseFirestore instance
     */
    private FirebaseFirestore getDb(){
        if(db == null) db = FirebaseFirestore.getInstance();
        return db;
    }

    /**
     * Global method to update an entrant's status from outside the Activity.
     * @param eventId the specific document ID for the event
     * @param userId the specific document ID for the user
     * @param status the new status string to apply (e.g., "accepted", "declined")
     */
    public void updateStatus(String eventId, String userId, String status){
        // Reference the nested 'entrants' collection via the parent 'events' document
        getDb().collection("events")
                .document(eventId)
                .collection("entrants")
                .document(userId)
                // Apply the new status value to the document field
                .update("status", status);
    }

    /**
     * Sets the local cache of the waiting list for this event.
     * @param list a List of user ID strings currently waiting
     */
    public void setLocalWaitingList(List<String> list){
        waitingList = list;
    }

    /**
     * Logic to safely return the size of the waitlist.
     * @return the integer count of entrants on the waiting list, or 0 if null
     */
    public int getWaitingCount(){
        return waitingList != null ? waitingList.size() : 0;
    }
}