package com.example.willow_lotto_app.events;


/**
 * Model class representing an event document stored in the Firestore "events" collection.
 *
 * Role in application:
 * - Serves as the core event data model used across event listing, event detail,
 *   organizer flows, and lottery-related screens.
 * - Stores basic event metadata such as title, description, organizer, registration
 *   period, poster URI, draw size, and registered users.
 *
 * Outstanding issues:
 * - This model currently stores both "limit" and "drawSize", but their usage is not
 *   fully standardized across the application.
 * - Some screens still only populate a subset of these fields.
 * - There is no helper method yet for converting this object directly to or from
 *   Firestore maps/documents.
 */

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
