package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    Button home, events, notifications, profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Redirect to login if not logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        home = findViewById(R.id.home);
        events = findViewById(R.id.events);
        notifications = findViewById(R.id.notifications);
        profile = findViewById(R.id.profile);

        home.setOnClickListener(v -> {/* Optional: navigate to home screen */});
        events.setOnClickListener(v -> startActivity(new Intent(this, EventActivity.class)));
        notifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        profile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }
}