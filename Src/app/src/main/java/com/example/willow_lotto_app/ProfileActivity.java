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

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    EditText nameInput, emailInput, phoneInput;
    Button saveButton, cancelButton, organizerDashboardButton, deleteButton;
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
        organizerDashboardButton = findViewById(R.id.organizerDashboardButton);
        bottomNav = findViewById(R.id.bottom_nav);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bottomNav.setSelectedItemId(R.id.nav_profile);

        saveButton.setOnClickListener(v -> saveProfile());
        cancelButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> confirmDeleteProfile()); // NEW
        organizerDashboardButton.setOnClickListener(
                v -> startActivity(new Intent(this, OrganizerDashboardActivity.class)));

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

    private void confirmDeleteProfile() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // Step 1: Delete Firestore user document
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

    private void saveProfile() {

        String name = nameInput.getText().toString();
        String email = emailInput.getText().toString();
        String phone = phoneInput.getText().toString();

        if(name.isEmpty() || email.isEmpty()){
            Toast.makeText(this, "Name and Email required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return; // null check added
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);

        db.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    finish();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error Saving Profile", Toast.LENGTH_SHORT).show());
    }

}