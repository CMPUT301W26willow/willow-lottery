package com.example.willow_lotto_app;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class UserNotification {
    private String eventId;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private Timestamp createdAt;

    public UserNotification() {
    }

    public UserNotification(String eventId, String title, String message, String type) {
        this.eventId = eventId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = false;
        this.createdAt = Timestamp.now();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("title", title);
        data.put("message", message);
        data.put("type", type);
        data.put("read", read);
        data.put("createdAt", createdAt == null ? Timestamp.now() : createdAt);
        return data;
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
}