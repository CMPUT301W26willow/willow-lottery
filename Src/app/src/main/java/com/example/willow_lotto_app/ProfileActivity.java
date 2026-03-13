package com.example.willow_lotto_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileActivity extends AppCompatActivity {

    EditText nameInput, emailInput, phoneInput;
<<<<<<< Updated upstream
    Button saveButton, cancelButton, organizerDashboardButton, deleteButton;
=======
    // Button UI references from the profile screen
    Button saveButton, cancelButton, organizerDashboardButton, deleteButton, registerButton;    // refrences to the NAV( home, events, notifications, profile)
>>>>>>> Stashed changes
    BottomNavigationView bottomNav;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteProfileButton);
        cancelButton = findViewById(R.id.cancelButton);
        registerButton = findViewById(R.id.registerButton);
        organizerDashboardButton = findViewById(R.id.organizerDashboardButton);
        bottomNav = findViewById(R.id.bottom_nav);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bottomNav.setSelectedItemId(R.id.nav_profile);

        loadProfile(); // NEW: Load existing profile data when screen opens

        saveButton.setOnClickListener(v -> saveProfile());
        cancelButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> confirmDeleteProfile());
<<<<<<< Updated upstream
        organizerDashboardButton.setOnClickListener(
                v -> startActivity(new Intent(this, OrganizerDashboardActivity.class)));

=======
        registerButton.setOnClickListener(v -> showRegistrationHistory());
        // click listener for Organizer Dashboard button
        /*organizerDashboardButton.setOnClickListener(
                v -> startActivity(new Intent(this, OrganizerDashboardActivity.class)));*/
        organizerDashboardButton.setOnClickListener(v -> openOrganizerDashboardForLatestEvent());
        // click listener for Bottom Navigation to guide to diffrent navigation pages of the app
>>>>>>> Stashed changes
        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }

            if (item.getItemId() == R.id.nav_events) {
                startActivity(new Intent(this, EventsActivity.class));
                finish();
                return true;
            }

            if (item.getItemId() == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                finish();
                return true;
            }

            if (item.getItemId() == R.id.nav_profile) {
                return true;
            }

            return false;
        });
    }

    private void loadProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

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

    private void confirmDeleteProfile() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

<<<<<<< Updated upstream
=======
    // Deletes the user's profile from Firestore and removes their
    // Firebase authentication account. After successful deletion,
    // the user is redirected to the LoginActivity screen.
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
=======
    // Saves or updates the user's profile information in Firestore.
    // It collects the name, email, and phone from the input fields,
    // validates required fields, and stores the data under the
    // current user's UID in the "users" collection.
>>>>>>> Stashed changes
    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email required", Toast.LENGTH_SHORT).show();
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