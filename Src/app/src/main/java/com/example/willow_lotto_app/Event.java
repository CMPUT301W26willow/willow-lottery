package com.example.willow_lotto_app;


/** Lottery event model; maps to Firestore "events" docs. */
public class Event {
    private String id;
    private String name;
    private String description;
    private String date;
    private String organizerId;
    private String registrationStart;
    private String registrationEnd;
    private String posterUri;
    private Integer limit;      // max spots
    private Integer drawSize;   // lottery winner count
    private java.util.List<String> registeredUsers;

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

    public String getRegistrationStart() {
        return registrationStart;
    }

    public void setRegistrationStart(String registrationStart) {
        this.registrationStart = registrationStart;
    }

    public String getRegistrationEnd() {
        return registrationEnd;
    }

    public void setRegistrationEnd(String registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    public String getPosterUri() {
        return posterUri;
    }

    public void setPosterUri(String posterUri) {
        this.posterUri = posterUri;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getDrawSize() {
        return drawSize;
    }

    public void setDrawSize(Integer drawSize) {
        this.drawSize = drawSize;
    }

    public java.util.List<String> getRegisteredUsers() {
        return registeredUsers;
    }

    public void setRegisteredUsers(java.util.List<String> registeredUsers) {
        this.registeredUsers = registeredUsers;
    }
}
