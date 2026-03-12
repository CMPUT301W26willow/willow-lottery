package com.example.willow_lotto_app;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    //Initialising all the various clickables
    TextInputLayout emailInput,passwordInput;
    Button signIn,continueAnon,signUp,adminDash,forgotPass;
    CheckBox rememberMe;

    // SharedPreferences (initialize in onCreate; Activity has no Context in constructor)
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private boolean rememberUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = getSharedPreferences("appData", MODE_PRIVATE);
        editor = sharedPref.edit();
        rememberUser = sharedPref.getBoolean("rememberUser", false);

        if (rememberUser) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        //Assigning views to buttons
        rememberMe = findViewById(R.id.rememberLogin);
        emailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        signIn = findViewById(R.id.buttonSignIn);
        continueAnon = findViewById(R.id.loginAsGuest);
        //signUp = findViewById(R.id.loginSignUp);
        adminDash = findViewById(R.id.loginAdminAccess);
        //forgotPass = findViewById(R.id.forgotPassword);

        //Setting On clicks
        continueAnon.setOnClickListener(view -> signInAnonymously());
        signIn.setOnClickListener(view -> checkUserProfile());

        /* commented out to allow for login testing
        if (currentUser != null) {

            Log.d("FIREBASE_LOGIN", "Already logged in UID: " + currentUser.getUid());
            checkUserProfile();

        } else {

            signInAnonymously();

        }
         */
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
                        if (rememberMe.isChecked()) {
                            editor.putBoolean("rememberUser", true);
                            editor.apply();
                        }
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