package com.example.willow_lotto_app;

/**
 * Model for a lottery event.
 */
public class Event {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    private String id;
    private String name;
    private String description;
    private String date;
    private String organizerId;

    public Event() {
    }

    public Event(String id, String name, String description, String date, String organizerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.date = date;
        this.organizerId = organizerId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }
}
