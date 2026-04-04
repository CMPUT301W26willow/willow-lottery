package com.example.willow_lotto_app.admin;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * User row model for admin profile browsing.
 */
public class AdminUserItem {
    private final String uid;
    private final String name;
    private final String displayName;
    private final String email;
    private final String profilePhotoUrl;
    private final boolean isOrganizer;
    private final boolean isAnonymous;

    public AdminUserItem(
            String uid,
            String name,
            String displayName,
            String email,
            String profilePhotoUrl,
            boolean isOrganizer,
            boolean isAnonymous) {
        this.uid = uid;
        this.name = name != null ? name : "";
        this.displayName = displayName != null ? displayName : "";
        this.email = email != null ? email : "";
        this.profilePhotoUrl = profilePhotoUrl != null ? profilePhotoUrl : "";
        this.isOrganizer = isOrganizer;
        this.isAnonymous = isAnonymous;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public boolean isOrganizer() {
        return isOrganizer;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public static AdminUserItem fromDocument(DocumentSnapshot doc) {
        String uid = doc.getId();
        String name = doc.getString("name");
        String displayName = doc.getString("displayName");
        String email = doc.getString("email");
        String photo = firstNonNull(
                doc.getString("profilePhotoUrl"),
                doc.getString("photoURL"),
                doc.getString("photoUrl"));
        Boolean isOrganizer = doc.getBoolean("isOrganizer");
        Boolean isAnonymous = doc.getBoolean("isAnonymous");

        return new AdminUserItem(
                uid,
                name != null ? name : "",
                displayName != null ? displayName : "",
                email != null ? email : "",
                photo != null ? photo : "",
                isOrganizer != null && isOrganizer,
                Boolean.TRUE.equals(isAnonymous));
    }

    private static String firstNonNull(String a, String b, String c) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        if (c != null && !c.trim().isEmpty()) {
            return c.trim();
        }
        return null;
    }
}
