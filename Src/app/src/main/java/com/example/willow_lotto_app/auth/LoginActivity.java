package com.example.willow_lotto_app.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.home.MainActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    TextInputLayout emailInput;
    TextInputLayout passwordInput;
    Button signIn;
    Button continueAnon;
    Button adminDash;
    CheckBox rememberMe;
    TextView signUp;
    TextView forgotPassword;
    private ProgressBar progressBar;
    private TextView loginStatus;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private boolean rememberUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getSharedPreferences("appData", MODE_PRIVATE);
        editor = sharedPref.edit();
        rememberUser = sharedPref.getBoolean("rememberUser", false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (rememberUser && mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        rememberMe = findViewById(R.id.rememberLogin);
        emailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        signIn = findViewById(R.id.buttonSignIn);
        continueAnon = findViewById(R.id.loginAsGuest);
        adminDash = findViewById(R.id.loginAdminAccess);
        signUp = findViewById(R.id.loginSignUp);
        forgotPassword = findViewById(R.id.loginForgotPassword);
        progressBar = findViewById(R.id.progressBar);
        loginStatus = findViewById(R.id.loginStatus);

        forgotPassword.setOnClickListener(v -> attemptPasswordReset());
        adminDash.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, AdminLoginActivity.class)));
        continueAnon.setOnClickListener(view -> signInAnonymously());
        signIn.setOnClickListener(view -> attemptEmailSignIn());
        signUp.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (loginStatus != null) {
            loginStatus.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        }
        signIn.setEnabled(!loading);
        continueAnon.setEnabled(!loading);
    }

    private void attemptEmailSignIn() {
        String email = textFrom(emailInput).trim();
        String password = textFrom(passwordInput);
        if (email.isEmpty()) {
            Toast.makeText(this, R.string.login_validation_email_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, R.string.login_validation_password_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        editor.putBoolean("rememberUser", rememberMe.isChecked());
                        editor.apply();
                        checkUserProfile();
                    } else {
                        Toast.makeText(this, R.string.login_sign_in_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void attemptPasswordReset() {
        String email = textFrom(emailInput).trim();
        if (email.isEmpty()) {
            Toast.makeText(this, R.string.login_reset_need_email, Toast.LENGTH_LONG).show();
            return;
        }
        setLoading(true);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.login_reset_email_sent, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.login_reset_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInAnonymously() {
        setLoading(true);
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("FIREBASE_LOGIN", "UID: " + user.getUid());
                        }
                        editor.putBoolean("rememberUser", rememberMe.isChecked());
                        editor.apply();
                        checkUserProfile();
                    } else {
                        Log.e("FIREBASE_LOGIN", "Anonymous sign-in failed", task.getException());
                        Toast.makeText(this, R.string.login_guest_failed, Toast.LENGTH_SHORT).show();
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
                        finish();
                    } else if (user.isAnonymous()) {
                        Log.d("PROFILE_CHECK", "No profile found, creating guest user");

                        String guestName = "Guest-" + uid.substring(0, Math.min(6, uid.length()));

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
                    } else {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
                        userData.put("name", "");
                        userData.put("phone", "");
                        List<String> registeredEvents = new ArrayList<>();
                        userData.put("registeredEvents", registeredEvents);
                        userData.put("isOrganizer", false);
                        userData.put("organizerBanned", false);
                        userData.put("isDeleted", false);
                        userData.put("deletedByAdmin", false);
                        userData.put("isAnonymous", false);

                        db.collection("users")
                                .document(uid)
                                .set(userData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Log.e("PROFILE_CHECK", "Failed to create user profile", e));
                    }
                });
    }

    private static String textFrom(TextInputLayout layout) {
        if (layout == null || layout.getEditText() == null) {
            return "";
        }
        CharSequence cs = layout.getEditText().getText();
        return cs != null ? cs.toString() : "";
    }
}
