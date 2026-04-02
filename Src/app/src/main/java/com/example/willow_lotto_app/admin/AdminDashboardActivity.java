package com.example.willow_lotto_app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.view.View;

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
    private View browseOrganizerProfilesCard;
    private View browseEntrantProfilesCard;
    private View browseEventsCard;
    private View browseImagesCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);


        browseEventsCard = findViewById(R.id.adminPanelEventManageCard);
        browseImagesCard = findViewById(R.id.adminPanelImageManageCard);

        if (!isCurrentUserAdmin()) {
            Toast.makeText(this, "You do not have organizer permissions", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }



        browseEventsCard.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseEventsActivity.class)));

        browseImagesCard.setOnClickListener(v ->
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
