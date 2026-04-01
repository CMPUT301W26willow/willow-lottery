package com.example.willow_lotto_app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Main dashboard for administrator-only actions.
 *
 * Responsibilities:
 * - Verifies that the current signed-in user is a hard-coded admin.
 * - Routes the admin to organizer browsing, entrant browsing, event browsing,
 *   and image browsing screens.
 *
 * Required layout ids:
 * - browseOrganizerProfilesButton
 * - browseEntrantProfilesButton
 * - browseEventsButton
 * - browseImagesButton
 */

public class AdminDashboardActivity extends AppCompatActivity{
    private Button browseOrganizerProfilesButton;
    private Button browseEntrantProfilesButton;
    private Button browseEventsButton;
    private Button browseImagesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        browseOrganizerProfilesButton = findViewById(R.id.browseOrganizerProfilesButton);
        browseEntrantProfilesButton = findViewById(R.id.browseEntrantProfilesButton);
        browseEventsButton = findViewById(R.id.browseEventsButton);
        browseImagesButton = findViewById(R.id.browseImagesButton);

        if (!isCurrentUserAdmin()) {
            Toast.makeText(this, "You do not have organizer permissions", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        browseOrganizerProfilesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseProfilesActivity.class);
            intent.putExtra(AdminBrowseProfilesActivity.EXTRA_MODE,
                    AdminBrowseProfilesActivity.MODE_ORGANIZERS);
            startActivity(intent);
        });

        browseEntrantProfilesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminBrowseProfilesActivity.class);
            intent.putExtra(AdminBrowseProfilesActivity.EXTRA_MODE,
                    AdminBrowseProfilesActivity.MODE_ENTRANTS);
            startActivity(intent);
        });

        browseEventsButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseEventsActivity.class)));

        browseImagesButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseImagesActivity.class)));
    }

    /**
     * Returns true if the currently signed-in user is a hard-coded admin.
     *
     * @return true if current user is an admin
     */
    private boolean isCurrentUserAdmin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null && AdminAccessUtil.isAdminEmail(currentUser.getEmail());
    }

}
