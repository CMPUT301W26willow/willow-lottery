package com.example.willow_lotto_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.events.CreateEventActivity;
import com.example.willow_lotto_app.events.EventsActivity;
import com.example.willow_lotto_app.notification.NotificationActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.willow_lotto_app.admin.AdminAccessUtil;
import com.example.willow_lotto_app.admin.AdminDashboardActivity;

/**
 * Profile management screen for the signed-in user.
 *
 * Responsibilities:
 * - Lets users view and edit basic profile fields backed by Firestore.
 * - Provides navigation into organizer flows (create first event or open
 *   {@link OrganizerDashboardActivity} for the latest event).
 * - Exposes registration history for the user based on stored event IDs.
 */
public class ProfileActivity extends AppCompatActivity {

    Switch notificationsOptInSwitch;

    //refrences to UI elements
    EditText nameInput, emailInput, phoneInput;
    // Button UI references from the profile screen
    Button saveButton, cancelButton, organizerDashboardButton, organizerMyEventsButton, deleteButton, registerButton, adminDashboardButton;    // refrences to the NAV( home, events, notifications, profile)
    // Button UI references from the profile screen

    BottomNavigationView bottomNav;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Called when the ProfileActivity screen is first created.
    // Initializes the UI layout, connects all UI elements to the code,
    // sets up Firebase instances, loads the user's profile data,
    // and sets click listeners for buttons and bottom navigation.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        //calling the resource folder for the UI elements
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteProfileButton);
        cancelButton = findViewById(R.id.cancelButton);
        registerButton = findViewById(R.id.registerButton);
        organizerDashboardButton = findViewById(R.id.organizerDashboardButton);
        organizerMyEventsButton = findViewById(R.id.organizerMyEventsButton);
        adminDashboardButton = findViewById(R.id.adminDashboardButton);
        bottomNav = findViewById(R.id.bottom_nav);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        notificationsOptInSwitch = findViewById(R.id.notificationsOptInSwitch);
        loadNotificationPreference();
        notificationsOptInSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveNotificationPreference(isChecked));


        bottomNav.setSelectedItemId(R.id.nav_profile);

        loadProfile(); // Load existing profile data when screen opens

        saveButton.setOnClickListener(v -> saveProfile());
        cancelButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> confirmDeleteProfile());
        registerButton.setOnClickListener(v->showRegistrationHistory());
        // Open the organizer dashboard for the latest event created by this user
        organizerDashboardButton.setOnClickListener(v -> openOrganizerDashboardForLatestEvent());
        organizerMyEventsButton.setOnClickListener(v -> openOrganizerMyEvents());
        adminDashboardButton.setOnClickListener(v -> openAdminDashboard());
        registerButton.setOnClickListener(v -> showRegistrationHistory());
        // click listener for Bottom Navigation to guide to diffrent navigation pages of the app

        bottomNav.setOnItemSelectedListener(item -> {
            // click listener for home page
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            // click listener for events page
            if (item.getItemId() == R.id.nav_events) {
                startActivity(new Intent(this, EventsActivity.class));
                finish();
                return true;
            }
            // click listener for notifications page
            if (item.getItemId() == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                finish();
                return true;
            }
            // click listener for profile page
            if (item.getItemId() == R.id.nav_profile) {
                return true;
            }

            return false;
        });
    }

    private void loadNotificationPreference() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");
                        // If the field is absent (null), default the switch to ON
                        notificationsOptInSwitch.setChecked(notificationsEnabled == null || notificationsEnabled);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notification preference.", Toast.LENGTH_SHORT).show());
    }

    private void saveNotificationPreference(boolean isEnabled) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .update("notificationsEnabled", isEnabled)
                .addOnSuccessListener(aVoid -> {
                    String msg = isEnabled ? "Notifications enabled" : "Notifications disabled";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update notification preference.", Toast.LENGTH_SHORT).show());
    }


    private void openOrganizerDashboardForLatestEvent() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No signed-in user.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("events")
                .whereEqualTo("organizerId", uid)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No events yet → take organizer to create their first one.
                        startActivity(new Intent(ProfileActivity.this, CreateEventActivity.class));
                    } else {
                        String eventId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Intent intent = new Intent(ProfileActivity.this, OrganizerDashboardActivity.class);
                        intent.putExtra(OrganizerDashboardActivity.EXTRA_EVENT_ID, eventId);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not open organizer dashboard.", Toast.LENGTH_SHORT).show());
    }

    private void openOrganizerMyEvents() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No signed-in user.", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, OrganizerMyEventsActivity.class));
    }

    /**
     * Opens the admin dashboard only if the current user's email is hard-coded
     * as an admin email.
     *
     * - If the user is not a hard-coded admin, show:
     * "you do not have admin permissions"
     */


    private void openAdminDashboard() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No signed-in user.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    String rawEmail = document.getString("email");
                    String email = rawEmail == null ? null : rawEmail.trim().toLowerCase();

                    boolean isAdmin =
                            email != null && (
                                    email.equals("admin1@gmail.com") ||
                                            email.equals("admin2@gmail.com") ||
                                            email.equals("admin3@gmail.com")
                            );

                    Toast.makeText(
                            this,
                            "isAdmin=" + isAdmin,
                            Toast.LENGTH_LONG
                    ).show();

                    if (!isAdmin) {
                        Toast.makeText(this, "you do not have admin permissions", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    startActivity(new Intent(ProfileActivity.this, AdminDashboardActivity.class));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not verify admin permissions", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads the user profile by filling the login areas with information if it already exists
     */
    private void loadProfile() {
        //Load existing profile data when screen opens
        if (mAuth.getCurrentUser() == null) return;
        // Get the current user's UID
        String uid = mAuth.getCurrentUser().getUid();
        //store the data in the database
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String email = document.getString("email");
                        String phone = document.getString("phone");

                        if (name != null) nameInput.setText(name);
                        if (email != null) emailInput.setText(email);
                        if (phone != null) phoneInput.setText(phone);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    /**
     * Sends an alert for the user to confirm whether they want to delete there account or not
     */
    private void confirmDeleteProfile() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    /**
     * Actual delete profile function, deletes the user account both in the app and the firebase document
     */

    private void deleteProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> {
                    mAuth.getCurrentUser().delete()
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Saves the Text input into the firebase in the users area
     */

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        String error = validateProfileInput(name, email);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);

        db.collection("users")
                .document(uid)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error Saving Profile", Toast.LENGTH_SHORT).show());
    }

    // Reads the registeredEvents array stored directly on the user's document,
    // then looks up each event name from the events collection

    /** Validates profile fields; returns error message or null if valid. */
    static String validateProfileInput(String name, String email) {
        if (name == null || name.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
            return "Name and Email required";
        }
        return null;
    }

    /**
     * Retrieves and displays current user registration history in an alert dialogue
     */
    private void showRegistrationHistory() {
        // Exit early if no user is signed in
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        // Read the user's own document
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    // Pull the registeredEvents array directly off the user document
                    List<String> registeredEvents = (List<String>) userDoc.get("registeredEvents");
                    if (registeredEvents == null || registeredEvents.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Registration History")
                                .setMessage("You have not registered for any events yet.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }
                    // Fetch each event document to get its name
                    StringBuilder history = new StringBuilder();
                    AtomicInteger remaining = new AtomicInteger(registeredEvents.size());
                    for (String eventId : registeredEvents) {
                        db.collection("events").document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {
                                    String eventName = eventDoc.getString("name");
                                    history.append("• ").append(eventName != null ? eventName : eventId)
                                            .append("\n\n");
                                    // Show the dialog only once all event lookups are done
                                    if (remaining.decrementAndGet() == 0) {
                                        new AlertDialog.Builder(this)
                                                .setTitle("Registration History")
                                                .setMessage(history.toString().trim())
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // If one event lookup fails, still count it as done
                                    if (remaining.decrementAndGet() == 0) {
                                        new AlertDialog.Builder(this)
                                                .setTitle("Registration History")
                                                .setMessage(history.toString().trim())
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load registration history", Toast.LENGTH_SHORT).show());
    }
}