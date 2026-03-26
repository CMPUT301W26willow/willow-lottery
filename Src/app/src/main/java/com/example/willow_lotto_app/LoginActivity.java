package com.example.willow_lotto_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // UI elements
    TextInputLayout emailInput, passwordInput;
    Button signIn, continueAnon, adminDash, forgotPass;
    CheckBox rememberMe;
    TextView signUp;

    // SharedPreferences
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private boolean rememberUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences (must be inside onCreate)
        sharedPref = getSharedPreferences("appData", MODE_PRIVATE);
        editor = sharedPref.edit();
        rememberUser = sharedPref.getBoolean("rememberUser", false);

        // Skip login if user chose "remember me"
        if (rememberUser) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Assign views
        rememberMe = findViewById(R.id.rememberLogin);
        emailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        signIn = findViewById(R.id.buttonSignIn);
        continueAnon = findViewById(R.id.loginAsGuest);
        adminDash = findViewById(R.id.loginAdminAccess);
        signUp = findViewById(R.id.loginSignUp);

        // Click listeners
        continueAnon.setOnClickListener(view -> signInAnonymously());
        signIn.setOnClickListener(view -> checkUserProfile());
        signUp.setOnClickListener(view -> {startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });
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
                        Log.e("FIREBASE_LOGIN", "Anonymous sign-in failed", task.getException());
                    }
                });
    }
    
    private void checkUserProfile() {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Log.d("PROFILE_CHECK", "Profile exists");
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Log.d("PROFILE_CHECK", "No profile found, creating guest user");

                        String guestName = "Guest-" + uid.substring(0, 6);

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("displayName", guestName);
                        userData.put("isAnonymous", true);
                        userData.put("createdAt", FieldValue.serverTimestamp());

                        db.collection("users")
                                .document(uid)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("PROFILE_CHECK", "Guest profile created");
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Log.e("PROFILE_CHECK", "Failed to create guest profile", e)
                                );
                    }
                });
    }
}