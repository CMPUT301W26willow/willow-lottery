package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    EditText emailInput,passwordInput;
    Button signIn,continueAnon,signUp,adminDash,forgotPass;
    CheckBox rememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        //Getting Firebase Keys
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        //Assigning views to buttons
        rememberMe = findViewById(R.id.rememberLogin);
        emailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        signIn = findViewById(R.id.buttonSignIn);
        continueAnon = findViewById(R.id.loginAsGuest);
        signUp = findViewById(R.id.loginSignUp);
        adminDash = findViewById(R.id.loginAdminAccess);
        forgotPass = findViewById(R.id.forgotPassword);

        //Setting On clicks
        continueAnon.setOnClickListener(view -> signInAnonymously());
        signIn.setOnClickListener(view -> signInAccount());

        /*
        if (currentUser != null) {

            Log.d("FIREBASE_LOGIN", "Already logged in UID: " + currentUser.getUid());
            checkUserProfile();

        } else {

            signInAnonymously();

        }
         */
    }

    private void signInAccount(){

        String Email = emailInput.getText().toString();
        String Password = passwordInput.getText().toString();

        mAuth.signInWithEmailAndPassword(Email,Password)
                .addOnCompleteListener(this, task -> {
            if (task.isSuccessful()){
                Toast.makeText(this, "Signed in Successfully",Toast.LENGTH_SHORT).show();
            }
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