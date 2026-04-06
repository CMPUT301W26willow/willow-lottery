package com.example.willow_lotto_app.events;

import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;

import java.util.List;

/** Helper for loading and updating waitlist registrations for an event. */
public class EventWaitlist {

    private List<Registration> waitingList; // Local cache of waitlisted registrations
    private RegistrationStore registrationStore;

    /**
     * getter for the RegistrationStore instance.
     * @return the active RegistrationStore instance
     */
    private RegistrationStore getRegistrationStore(){
        if(registrationStore == null) registrationStore = new RegistrationStore();
        return registrationStore;
    }

    /**
     * Global method to update an entrant's status from outside the Activity.
     * @param registrationId the specific registration document ID for the user
     * @param status the new status string to apply (e.g., "accepted", "declined")
     * @param callback callback used to report success or failure
     */
    public void updateStatus(String registrationId, String status, RegistrationStore.SimpleCallback callback){
        // Update the matching registration document in the top-level registrations collection
        getRegistrationStore().updateRegistrationStatus(registrationId, status, callback);
    }

    /**
     * Loads all waitlisted registrations for the specific event.
     * @param eventId the specific document ID for the event
     * @param callback callback returning the list of waitlisted registrations
     */
    public void loadWaitingList(String eventId, RegistrationStore.RegistrationListCallback callback){
        // US 01.05.04
        // Load all waitlisted registrations for the event so the UI can show
        // the total number of entrants currently on the waiting list.
        getRegistrationStore().getRegistrationsForEventByStatus(
                eventId,
                RegistrationStatus.WAITLISTED.getValue(),
                callback
        );
    }

    /**
     * Loads the registration for the current user for this event.
     * @param eventId the specific document ID for the event
     * @param userId the specific Firebase UID for the current user
     * @param callback callback returning the matching registration document
     */
    public void findUserRegistration(String eventId, String userId, RegistrationStore.RegistrationCallback callback){
        // Find the single registration document matching the current event and user
        getRegistrationStore().findRegistration(eventId, userId, callback);
    }

    /**
     * Sets the local cache of the waiting list for this event.
     * @param list a List of Registration objects currently waiting
     */
    public void setLocalWaitingList(List<Registration> list){

        waitingList = list;
    }

    /**
     * Logic to safely return the size of the waitlist.
     * @return the integer count of entrants on the waiting list, or 0 if null
     */
    public int getWaitingCount(){
        // US 01.05.04
        // Return the total number of entrants currently on the waiting list.
        return waitingList != null ? waitingList.size() : 0;
    }
}