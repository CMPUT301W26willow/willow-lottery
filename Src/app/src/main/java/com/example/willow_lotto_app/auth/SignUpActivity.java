package com.example.willow_lotto_app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.home.MainActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Email/password account registration screen.
 */
public class SignUpActivity extends AppCompatActivity {

    TextInputLayout nameInput;
    TextInputLayout emailInput;
    TextInputLayout passwordInput;
    Button signUpComplete;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sign_up);

        MaterialToolbar toolbar = findViewById(R.id.sign_up_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        nameInput = findViewById(R.id.signUpNameInput);
        emailInput = findViewById(R.id.signUpEmailInput);
        passwordInput = findViewById(R.id.signUpPasswordInput);
        signUpComplete = findViewById(R.id.buttonFinishSignUp);

        TextView signInLink = findViewById(R.id.signUpSignInLink);
        signInLink.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        signUpComplete.setOnClickListener(view -> {
            String name = textFrom(nameInput).trim();
            String email = textFrom(emailInput).trim();
            String password = textFrom(passwordInput);
            String phone = "000-000-0000";

            String error = validateSignUpInput(name, email, password);
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }

            checkBannedEmailBeforeSignUp(name, email, password, phone);
        });
    }

    /**
     * Checks whether the email entered during sign-up has been banned by an administrator.
     *<p>
     * If the email is banned, sign-up is stopped.
     * If the email is not banned, the existing account creation logic runs.
     *
     * @param name entered name
     * @param email entered email
     * @param password entered password
     * @param phone default phone value
     */
    private void checkBannedEmailBeforeSignUp(String name, String email, String password, String phone) {
        String emailKey = email.toLowerCase(Locale.CANADA).trim();

        db.collection("banned_emails")
                .document(emailKey)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Toast.makeText(this, "This email is banned from signing up.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createUserAndSaveProfile(name, email, password, phone);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not verify sign-up eligibility.", Toast.LENGTH_SHORT).show());
    }

    private void createUserAndSaveProfile(String name, String email, String password, String phone) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        Toast.makeText(this, R.string.sign_up_auth_user_missing, Toast.LENGTH_LONG).show();
                        return;
                    }
                    String uid = firebaseUser.getUid();

                    List<String> registeredEvents = new ArrayList<>();
                    Map<String, Object> user = new HashMap<>();
                    user.put("name", name);
                    user.put("email", email);
                    user.put("phone", phone);
                    user.put("registeredEvents", registeredEvents);
                    user.put("isOrganizer", false);
                    user.put("organizerBanned", false);
                    user.put("isDeleted", false);
                    user.put("deletedByAdmin", false);
                    user.put("isAnonymous", false);

                    db.collection("users")
                            .document(uid)
                            .set(user, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Account Created", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Could not save profile: " + (e.getMessage() != null ? e.getMessage() : "unknown error"),
                                            Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, authErrorMessage(e), Toast.LENGTH_LONG).show());
    }

    private static String textFrom(TextInputLayout layout) {
        if (layout == null || layout.getEditText() == null) {
            return "";
        }
        CharSequence cs = layout.getEditText().getText();
        return cs != null ? cs.toString() : "";
    }

    static String validateSignUpInput(String name, String email, String password) {
        if (name == null || name.trim().isEmpty()) {
            return "Name required";
        }
        if (email == null || email.trim().isEmpty()) {
            return "Email required";
        }
        if (password == null || password.isEmpty()) {
            return "Password required";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        return null;
    }

    private static String authErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Password is too weak. Use at least 6 characters.";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "An account already exists with this email.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Invalid email or password format.";
        }
        String msg = e != null && e.getMessage() != null ? e.getMessage() : "Sign up failed.";
        return "Could not create account: " + msg;
    }
}
