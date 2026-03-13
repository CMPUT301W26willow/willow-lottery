package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Email/password account registration screen.
 *
 * Responsibilities:
 * - Implements 01.02.01 "Account Registration" by creating a Firebase Auth
 *   user with email and password.
 * - Seeds a profile document in the {@code users} collection so
 *   {@link ProfileActivity} can load and update the same data later.
 */
public class SignUpActivity extends AppCompatActivity {

    //User Input UI elements
    TextInputLayout nameInput, emailInput, passwordInput;
    Button signUpComplete;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String[] registeredEvents = new String[40];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sign_up);

        //Initialising Ui Interactable elements
        nameInput = findViewById(R.id.signUpNameInput);
        emailInput = findViewById(R.id.signUpEmailInput);
        passwordInput = findViewById(R.id.signUpPasswordInput);
        signUpComplete = findViewById(R.id.buttonFinishSignUp);

        //Initialising Firebase Related services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        //OnclickListener to allow user to proceed
        signUpComplete.setOnClickListener(view -> {
            String name = String.valueOf(nameInput.getEditText().getText()).trim();
            String email = String.valueOf(emailInput.getEditText().getText()).trim();
            String password = String.valueOf(passwordInput.getEditText().getText());
            String phone = "000-000-0000";

            String error = validateSignUpInput(name, email, password);
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();

                        //Mapping User Hashmap (keys match ProfileActivity expectations)
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);
                        user.put("phone", phone);
                        user.put("registeredEvents",registeredEvents);

                        db.collection("users")
                                .document(uid)
                                .set(user, SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Account Created", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error Saving Profile", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error Creating Account: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        });

    }

    /** Validates sign-up fields; returns first error message or null if valid. */
    static String validateSignUpInput(String name, String email, String password) {
        if (name == null || name.trim().isEmpty()) return "Name required";
        if (email == null || email.trim().isEmpty()) return "Email Required";
        if (password == null || password.trim().isEmpty()) return "Password Invalid";
        return null;
    }

}
