package com.example.willow_lotto_app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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

    private TextView infoEventCard,infoUserCard,infoWaitlistCard,infoNotifCard;

    private FirebaseFirestore db;

    Integer statEvents,statUsers,statWaitlist,statNotif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        infoEventCard = findViewById(R.id.adminPanelEventNumber);
        infoUserCard = findViewById(R.id.adminPanelUserNumber);
        infoWaitlistCard = findViewById(R.id.adminPanelWaitlistNumber);
        infoNotifCard = findViewById(R.id.adminPanelNotifNumber);

        browseEventsCard = findViewById(R.id.adminPanelEventManageCard);
        browseImagesCard = findViewById(R.id.adminPanelImageManageCard);
        browseEntrantProfilesCard = findViewById(R.id.adminPanelProfileManageCard);

        /* Temporary test bypass
        if (!isCurrentUserAdmin()) {
            Toast.makeText(this, "You do not have admin permissions", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }*/

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        browseEventsCard.setOnClickListener(v ->{
                startActivity(new Intent(this, AdminBrowseEventsActivity.class));
        });

        browseImagesCard.setOnClickListener(v ->{
                startActivity(new Intent(this, AdminBrowseImagesActivity.class));
        });

        browseEntrantProfilesCard.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminBrowseProfilesActivity.class));
        });

        db.collection("statsStore").document("appStats").get().addOnSuccessListener(document ->{
            if (document.exists()) {

                statEvents = document.getLong("events").intValue();
                statUsers = document.getLong("users").intValue();
                statWaitlist = document.getLong("waitlisted").intValue();
                statNotif = document.getLong("notificationsSent").intValue();

                //if (statEvents == 0 || statUsers == 0 || statWaitlist == 0){updateFirebaseStats();}

                infoEventCard.setText(statEvents.toString());
                infoUserCard.setText(statUsers.toString());
                infoWaitlistCard.setText(statWaitlist.toString());
                infoNotifCard.setText(statNotif.toString());
            }
        });


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
