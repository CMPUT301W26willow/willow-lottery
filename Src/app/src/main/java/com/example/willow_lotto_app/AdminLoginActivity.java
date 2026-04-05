package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

<<<<<<< Updated upstream
=======
import com.example.willow_lotto_app.admin.AdminAccessUtil;
import com.example.willow_lotto_app.admin.AdminDashboardActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Sample admin entry from the main login screen: email + password via Firebase Auth,
 * then opens {@link AdminDashboardActivity} only if the account is on the allow-list.
 */
>>>>>>> Stashed changes
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
