package com.example.willow_lotto_app.notification;

import com.google.firebase.Timestamp;

/** One notification row loaded from Firestore for the list UI. */

public class UserNotificationItem {
    private String id;
    private String eventId;
    private String title;
    private String message;
    private String type;
    private String inviterId;
    private boolean read;
    private Timestamp createdAt;

    public UserNotificationItem() {
    }

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public boolean isRead() {
        return read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInviterId() {
        return inviterId;
    }

    public void setInviterId(String inviterId) {
        this.inviterId = inviterId;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}