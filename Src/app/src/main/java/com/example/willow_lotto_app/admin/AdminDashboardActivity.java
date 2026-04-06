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

import com.example.willow_lotto_app.profile.ProfileActivity;
import com.example.willow_lotto_app.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.Locale;

/** Admin home: counts, search, links to moderation screens, notification log. */
public class AdminDashboardActivity extends AppCompatActivity {

    private View browseEventsCard;
    private View browseImagesCard;
    private View browseProfilesCard;
    private View browseOrganizersCard;
    private View profileButton;
    private View notifLogCard;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Must be allow-listed admin (same check as other admin activities, without deep-link guard helper).
        if (!isCurrentUserAdmin()) {
            Toast.makeText(this, R.string.admin_permission_denied, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Summary stat TextViews + management card counts
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
        notifLogCard = findViewById(R.id.adminPanelNotifLogCard);

        // Navigation shortcuts
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

        // Search: IME action or go button opens destination picker, passes query via AdminIntentExtras
        searchInput = findViewById(R.id.admin_dashboard_search_input);
        notifLogCard.setOnClickListener(v -> showNotificationLogDialog());
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

    // Parallel reads: events, users, registrations; then collectionGroup notifications for notif count.
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

    // Show em dash when stats failed to load
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

    // Read all notification subdocs and show title/message/type in one scrollable dialog
    private void showNotificationLogDialog() {
        FirebaseFirestore.getInstance()
                .collectionGroup("notifications")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (snapshots.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Notification Log")
                                .setMessage("No notifications found.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    StringBuilder sb = new StringBuilder();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String title   = doc.getString("title");
                        String message = doc.getString("message");
                        String type    = doc.getString("type");
                        sb.append("● ")
                                .append(title != null ? title : "(no title)")
                                .append("\n")
                                .append(message != null ? message : "")
                                .append("\n")
                                .append(type != null ? "[" + type + "]" : "")
                                .append("\n\n");
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Notification Log (" + snapshots.size() + ")")
                            .setMessage(sb.toString().trim())
                            .setPositiveButton("Close", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show());
    }

    // User picks which browse screen receives the search query
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

    /**
     * @param destinationIndex index into {@code R.array.admin_search_destinations}
     * @param query            raw search string to pass as {@link AdminIntentExtras#EXTRA_SEARCH_QUERY}
     */
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

    /**
     * @param n integer to show in dashboard tiles
     * @return locale-formatted string
     */
    private String formatInt(int n) {
        return intFormat.format(n);
    }

    /**
     * @param snap {@code events} query snapshot
     * @return count of documents without {@code isDeleted=true}
     */
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

    /**
     * @param snap {@code users} query snapshot
     * @return count of non-deleted user profiles
     */
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

    /**
     * @param usersSnap {@code users} query snapshot
     * @return non-deleted users with {@code isOrganizer=true}
     */
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

    /**
     * @param snap {@code events} query snapshot
     * @return non-deleted events with non-empty {@code posterUri}
     */
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

    /**
     * @return true if Firebase Auth user email is on the admin allow-list
     */
    private boolean isCurrentUserAdmin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null && AdminAccessUtil.isAdminEmail(currentUser.getEmail());
    }
}
