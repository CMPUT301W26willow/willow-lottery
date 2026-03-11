package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

            Log.d("FIREBASE_LOGIN", "Already logged in UID: " + currentUser.getUid());
            checkUserProfile();

        } else {

            signInAnonymously();

        }
    }

    private void signInAnonymously() {

        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            Log.d("FIREBASE_LOGIN", "UID: " + user.getUid());
                        }

                        checkUserProfile();

                    } else {

                        Log.e("FIREBASE_LOGIN", "Login Failed", task.getException());

                    }

                });
    }

    private void checkUserProfile() {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }
        String uid = user.getUid();
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Log.d("PROFILE_CHECK", "Profile exists");
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));

                    } else {

                        Log.d("PROFILE_CHECK", "Profile missing");
                        startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                    }

                    finish();
                })
                .addOnFailureListener(e ->
                        Log.e("PROFILE_CHECK", "Error checking profile", e));
    }
}