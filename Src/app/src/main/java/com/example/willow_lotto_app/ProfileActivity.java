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
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    EditText nameInput, emailInput, phoneInput; //inputs
    Button saveButton, cancelButton, organizerDashboardButton, deleteButton,historyButton; //buttons
    BottomNavigationView bottomNav;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); //link all UI elements

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteProfileButton);
        cancelButton = findViewById(R.id.cancelButton);
        organizerDashboardButton = findViewById(R.id.organizerDashboardButton);
        bottomNav = findViewById(R.id.bottom_nav);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bottomNav.setSelectedItemId(R.id.nav_profile);

        loadProfile(); //load an existing profile if it exists

        saveButton.setOnClickListener(v -> saveProfile()); //set up buttons
        cancelButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> confirmDeleteProfile());
        historyButton = findViewById(R.id.viewHistoryButton);
        historyButton.setOnClickListener(v -> viewRegistrationHistory());
        organizerDashboardButton.setOnClickListener(
                v -> startActivity(new Intent(this, OrganizerDashboardActivity.class)));

        // Navigation
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
    } //**  Fetches the users profile data, and populates the existing fields *
    // */

    private void loadProfile() {
        // if no login do nothing
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // look in the users document
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        //get and set the fields
                        String name = document.getString("name");
                        String email = document.getString("email");
                        String phone = document.getString("phone");

                        if (name != null) nameInput.setText(name);
                        if (email != null) emailInput.setText(email);
                        if (phone != null) phoneInput.setText(phone);
                    }
                })// if the profile can't load
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows a confirmation dialogue before deleting the profile
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
     * deletes the users profile document from firestore
     * deletes the authentication account and the redirects to log in
     */

    private void deleteProfile() {
        // if not logged in do nothing
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // look for user and delete them
        db.collection("users")
                .document(uid)
                .delete()
                //delete firebase authentication account
                .addOnSuccessListener(unused -> {
                    mAuth.getCurrentUser().delete()
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                                // redirect to home page
                                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }) //* if loading the profile failed
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    } /**
    *Validates, stores and updates the users information
     * */

    private void saveProfile() {
        // we use trim so in case of leading, or ending spaces
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            // if the name or email is empty dont let them log in
            Toast.makeText(this, "Name and Email required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;
        // if user is not logged in do nothing
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        // build a map for the fields
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);

        // we merge the information
        db.collection("users")
                .document(uid)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    // go back to home screen when done
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> // if it cant save raise an error
                        Toast.makeText(this, "Error Saving Profile", Toast.LENGTH_SHORT).show());
    }

    /**
     * Fetches all events the user is in and display them
     */
    private void viewRegistrationHistory() {
        // if nothing do nothing
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // Fetch all registrations for the current user
        db.collection("registrations")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "You have not registered for any events yet.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Build a readable list of events and their statuses
                    StringBuilder history = new StringBuilder();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String eventId = doc.getString("eventName"); // the event name
                        String status = doc.getString("status"); // and the status
                        history.append("Event: ").append(eventId)
                                .append("\nStatus: ").append(status)
                                .append("\n\n");
                    }

                    // Show the history in a dialog
                    new AlertDialog.Builder(this)
                            .setTitle("My Event History")
                            .setMessage(history.toString())
                            .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show());
    }



}