package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AdminLoginActivity extends AppCompatActivity {

    ImageView adminShield;
    Button backButton, adminAccessButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backButton = findViewById(R.id.adminBackButton);

        // debug pass to enter directly into admin dashboard without login
        adminShield = findViewById(R.id.adminImage);
        adminShield.setOnClickListener(View -> {startActivity(new Intent(AdminLoginActivity.this,AdminPanelActivity.class));});

        // back button to sign in screen
        backButton.setOnClickListener(View -> startActivity(new Intent(AdminLoginActivity.this, LoginActivity.class)));
    }
}
