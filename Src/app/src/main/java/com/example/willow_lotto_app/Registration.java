package com.example.willow_lotto_app;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a single registration document in the Firestore "registrations" collection.
 *
 * Role in application:
 * - Serves as the main data model for entrant state in the lottery workflow.
 * - Stores the relationship between a user and an event, along with the user's current registration status.
 *
 * Firestore fields represented here:
 * - eventId
 * - userId
 * - status
 * - createdAt
 * - updatedAt
 *
 * Outstanding issues:
 * - This class does not currently validate status values on assignment.
 * - It assumes Firestore document fields use the exact expected names.
 */

public class Registration {
    private String id;
    private String eventId;
    private String userId;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Registration() {
        // Required empty constructor for Firestore
    }

    public Registration(String eventId, String userId, String status) {
        this.eventId = eventId;
        this.userId = userId;
        this.status = status;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    public static Registration fromDocument(@NonNull DocumentSnapshot doc) {
        Registration registration = new Registration();
        registration.setId(doc.getId());
        registration.setEventId(doc.getString("eventId"));
        registration.setUserId(doc.getString("userId"));
        registration.setStatus(doc.getString("status"));
        registration.setCreatedAt(doc.getTimestamp("createdAt"));
        registration.setUpdatedAt(doc.getTimestamp("updatedAt"));
        return registration;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("userId", userId);
        data.put("status", status);
        data.put("createdAt", createdAt == null ? Timestamp.now() : createdAt);
        data.put("updatedAt", Timestamp.now());
        return data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public RegistrationStatus getStatusEnum() {
        return RegistrationStatus.fromString(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}