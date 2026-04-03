package com.example.willow_lotto_app.admin;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Simple model used by admin profile browsing screens.
 */

public class AdminUserItem {
    private final String uid;
    private final String name;
    private final String email;
    private final boolean isOrganizer;

    public AdminUserItem(String uid, String name, String email, boolean isOrganizer) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.isOrganizer = isOrganizer;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isOrganizer() {
        return isOrganizer;
    }

    /**
     * Builds an AdminUserItem from a Firestore user document.
     *
     * @param doc user document
     * @return converted admin user item
     */
    public static AdminUserItem fromDocument(DocumentSnapshot doc) {
        String uid = doc.getId();
        String name = doc.getString("name");
        String email = doc.getString("email");
        Boolean isOrganizer = doc.getBoolean("isOrganizer");

        return new AdminUserItem(
                uid,
                name != null ? name : "",
                email != null ? email : "",
                isOrganizer != null && isOrganizer
        );
    }

}
