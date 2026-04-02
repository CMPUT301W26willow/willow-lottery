package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.admin.AdminDashboardActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends AppCompatActivity {

    ImageView adminShield;
    Button backButton, adminAccessButton;

    FirebaseFirestore db;

    TextInputLayout emailInput, passwordInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        setContentView(R.layout.activity_admin_login);

        backButton = findViewById(R.id.adminBackButton);
        emailInput = findViewById(R.id.adminEmailInput);
        passwordInput = findViewById(R.id.adminPasswordInput);
        adminAccessButton = findViewById(R.id.adminAccessButton);


        // debug pass to enter directly into admin dashboard without login
        adminShield = findViewById(R.id.adminImage);
        adminShield.setOnClickListener(View -> {startActivity(new Intent(AdminLoginActivity.this, AdminDashboardActivity.class));});

        // back button to sign in screen
        backButton.setOnClickListener(View -> startActivity(new Intent(AdminLoginActivity.this, LoginActivity.class)));
    }
}
