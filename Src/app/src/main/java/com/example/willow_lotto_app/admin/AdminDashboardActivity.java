package com.example.willow_lotto_app.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.ProfileActivity;
import com.example.willow_lotto_app.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Administrator home: platform metrics, search entry point, and shortcuts to moderation screens.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private View browseEventsCard;
    private View browseImagesCard;
    private View browseProfilesCard;
    private View browseOrganizersCard;
    private View profileButton;

    private TextView eventNumber;
    private TextView userNumber;
    private TextView waitlistNumber;
    private TextView notifNumber;
    private TextView manageEventsCount;
    private TextView manageProfilesCount;
    private TextView manageOrganizersCount;
    private TextView manageImagesCount;

    private EditText searchInput;
    private ImageButton searchGoButton;

    private final NumberFormat intFormat = NumberFormat.getIntegerInstance(Locale.getDefault());

    private TextView infoEventCard,infoUserCard,infoWaitlistCard,infoNotifCard;

    private FirebaseFirestore db;

    Integer statEvents,statUsers,statWaitlist,statNotif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        if (!isCurrentUserAdmin()) {
            Toast.makeText(this, R.string.admin_permission_denied, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventNumber = findViewById(R.id.adminPanelEventNumber);
        userNumber = findViewById(R.id.adminPanelUserNumber);
        waitlistNumber = findViewById(R.id.adminPanelWaitlistNumber);
        notifNumber = findViewById(R.id.adminPanelNotifNumber);
        manageEventsCount = findViewById(R.id.adminManagementEventsCount);
        manageProfilesCount = findViewById(R.id.adminManagementProfilesCount);
        manageOrganizersCount = findViewById(R.id.adminManagementOrganizersCount);
        manageImagesCount = findViewById(R.id.adminManagementImagesCount);

        browseEventsCard = findViewById(R.id.adminPanelEventManageCard);
        browseImagesCard = findViewById(R.id.adminPanelImageManageCard);
        browseProfilesCard = findViewById(R.id.adminPanelProfileManageCard);
        browseOrganizersCard = findViewById(R.id.adminPanelOrganizersManageCard);
        profileButton = findViewById(R.id.AdminPanelProfileButton);

        profileButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));


        browseImagesCard.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseImagesActivity.class)));

        browseProfilesCard.setOnClickListener(v -> {
            Intent i = new Intent(this, AdminBrowseProfilesActivity.class);
            i.putExtra(AdminBrowseProfilesActivity.EXTRA_MODE, AdminBrowseProfilesActivity.MODE_ENTRANTS);
            startActivity(i);
        });

        browseOrganizersCard.setOnClickListener(v -> {
            Intent i = new Intent(this, AdminBrowseProfilesActivity.class);
            i.putExtra(AdminBrowseProfilesActivity.EXTRA_MODE, AdminBrowseProfilesActivity.MODE_ORGANIZERS);
            startActivity(i);
        });

        browseEventsCard.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseEventsActivity.class)));

        searchInput = findViewById(R.id.admin_dashboard_search_input);
        searchGoButton = findViewById(R.id.admin_dashboard_search_go);
        searchGoButton.setOnClickListener(v -> showSearchDestinationPicker());
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                showSearchDestinationPicker();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> eventsTask = db.collection("events").get();
        Task<QuerySnapshot> usersTask = db.collection("users").get();
        Task<QuerySnapshot> regTask = db.collection("registrations").get();

        Tasks.whenAllSuccess(eventsTask, usersTask, regTask)
                .addOnSuccessListener(results -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    QuerySnapshot eventsSnap = (QuerySnapshot) results.get(0);
                    QuerySnapshot usersSnap = (QuerySnapshot) results.get(1);
                    QuerySnapshot regSnap = (QuerySnapshot) results.get(2);

                    int activeEvents = countActiveEvents(eventsSnap);
                    int activeUsers = countActiveUsers(usersSnap);
                    int organizerCount = countActiveOrganizers(usersSnap);
                    int registrations = regSnap.size();
                    int imagesWithPoster = countEventsWithPoster(eventsSnap);

                    eventNumber.setText(formatInt(activeEvents));
                    userNumber.setText(formatInt(activeUsers));
                    waitlistNumber.setText(formatInt(registrations));
                    manageEventsCount.setText(formatInt(activeEvents));
                    manageProfilesCount.setText(formatInt(activeUsers));
                    manageOrganizersCount.setText(formatInt(organizerCount));
                    manageImagesCount.setText(formatInt(imagesWithPoster));

                    db.collectionGroup("notifications")
                            .get()
                            .addOnSuccessListener(q -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    notifNumber.setText(formatInt(q.size()));
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    notifNumber.setText("—");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        clearStatsNumbers();
                        Toast.makeText(this, R.string.admin_dashboard_stats_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearStatsNumbers() {
        String dash = "—";
        eventNumber.setText(dash);
        userNumber.setText(dash);
        waitlistNumber.setText(dash);
        notifNumber.setText(dash);
        manageEventsCount.setText(dash);
        manageProfilesCount.setText(dash);
        manageOrganizersCount.setText(dash);
        manageImagesCount.setText(dash);
    }

    private void showSearchDestinationPicker() {
        String raw = searchInput != null && searchInput.getText() != null
                ? searchInput.getText().toString().trim()
                : "";
        if (raw.isEmpty()) {
            Toast.makeText(this, R.string.admin_search_empty_query, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = getResources().getStringArray(R.array.admin_search_destinations);
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_search_pick_destination)
                .setItems(labels, (d, which) -> launchSearch(which, raw))
                .show();
    }

    private void launchSearch(int destinationIndex, String query) {
        switch (destinationIndex) {
            case 0:
                startActivity(new Intent(this, AdminBrowseEventsActivity.class)
                        .putExtra(AdminIntentExtras.EXTRA_SEARCH_QUERY, query));
                break;
            case 1:
                startActivity(new Intent(this, AdminBrowseProfilesActivity.class)
                        .putExtra(AdminBrowseProfilesActivity.EXTRA_MODE, AdminBrowseProfilesActivity.MODE_ENTRANTS)
                        .putExtra(AdminIntentExtras.EXTRA_SEARCH_QUERY, query));
                break;
            case 2:
                startActivity(new Intent(this, AdminBrowseProfilesActivity.class)
                        .putExtra(AdminBrowseProfilesActivity.EXTRA_MODE, AdminBrowseProfilesActivity.MODE_ORGANIZERS)
                        .putExtra(AdminIntentExtras.EXTRA_SEARCH_QUERY, query));
                break;
            case 3:
                startActivity(new Intent(this, AdminBrowseImagesActivity.class)
                        .putExtra(AdminIntentExtras.EXTRA_SEARCH_QUERY, query));
                break;
            default:
                break;
        }
    }

    private String formatInt(int n) {
        return intFormat.format(n);
    }

    private static int countActiveEvents(QuerySnapshot snap) {
        int n = 0;
        for (DocumentSnapshot d : snap.getDocuments()) {
            Boolean del = d.getBoolean("isDeleted");
            if (del != null && del) {
                continue;
            }
            n++;
        }
        return n;
    }

    private static int countActiveUsers(QuerySnapshot snap) {
        int n = 0;
        for (DocumentSnapshot d : snap.getDocuments()) {
            Boolean del = d.getBoolean("isDeleted");
            if (del != null && del) {
                continue;
            }
            n++;
        }
        return n;
    }

    private static int countActiveOrganizers(QuerySnapshot usersSnap) {
        int n = 0;
        for (DocumentSnapshot d : usersSnap.getDocuments()) {
            if (Boolean.TRUE.equals(d.getBoolean("isDeleted"))) {
                continue;
            }
            Boolean org = d.getBoolean("isOrganizer");
            if (org != null && org) {
                n++;
            }
        }
        return n;
    }

    private static int countEventsWithPoster(QuerySnapshot snap) {
        int n = 0;
        for (DocumentSnapshot d : snap.getDocuments()) {
            Boolean del = d.getBoolean("isDeleted");
            if (del != null && del) {
                continue;
            }
            String posterUri = d.getString("posterUri");
            if (posterUri != null && !posterUri.trim().isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private boolean isCurrentUserAdmin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null && AdminAccessUtil.isAdminEmail(currentUser.getEmail());
    }
}
