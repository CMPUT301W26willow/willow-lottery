package com.example.willow_lotto_app.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Administrator screen for browsing user profiles.
 * <p>
 * Responsibilities:
 * - Supports separate organizer and entrant browsing modes.
 * - Allows administrators to remove organizer privileges.
 * - Allows administrators to remove entrant/user profiles.
 * - Bans removed entrant emails from signing up again.
 */

public class AdminBrowseProfilesActivity extends AppCompatActivity implements AdminProfileAdapter.AdminProfileActionListener{
    /**
     * Intent extra key used to decide which type of profiles to display.
     */
    public static final String EXTRA_MODE = "mode";

    /**
     * Mode constant for organizer profile browsing.
     */
    public static final String MODE_ORGANIZERS = "organizers";

    /**
     * Mode constant for entrant/user profile browsing.
     */
    public static final String MODE_ENTRANTS = "entrants";

    /**
     * RecyclerView for displaying profiles.
     */
    private RecyclerView recyclerView;

    /**
     * Firestore reference.
     */
    private FirebaseFirestore db;

    /**
     * Current browsing mode.
     */
    private String mode;

    /**
     * Signed-in admin email used for logs.
     */
    private String adminEmail;

    /**
     * Backing list for the adapter.
     */
    private final List<AdminUserItem> userList = new ArrayList<>();

    /**
     * Adapter used by the RecyclerView.
     */
    private AdminProfileAdapter adapter;

    private String searchQueryNormalized = "";

    private boolean screenReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AdminUiHelper.requireAdminOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_admin_browse_profiles);

        searchQueryNormalized = AdminSearchTextUtil.normalizeQuery(
                getIntent().getStringExtra(AdminIntentExtras.EXTRA_SEARCH_QUERY));

        recyclerView = findViewById(R.id.adminProfilesRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        adminEmail = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "";

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) {
            mode = MODE_ENTRANTS;
        }

        adapter = new AdminProfileAdapter(userList, this, mode);
        recyclerView.setAdapter(adapter);

        MaterialToolbar toolbar = findViewById(R.id.admin_profiles_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        toolbar.setTitle(MODE_ORGANIZERS.equals(mode)
                ? R.string.admin_browse_profiles_screen_title_organizers
                : R.string.admin_browse_profiles_screen_title);

        screenReady = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!screenReady || isFinishing()) {
            return;
        }
        loadProfiles();
    }

    /**
     * Loads profiles depending on the current screen mode.
     * <p>
     * Organizer mode:
     * - show users where isOrganizer is true
     * <p>
     * Entrant mode:
     * - show all non-deleted users
     * <p>
     * In both cases, soft-deleted users are excluded.
     */
    private void loadProfiles() {
        if (MODE_ORGANIZERS.equals(mode)) {
            db.collection("users")
                    .whereEqualTo("isOrganizer", true)
                    .get()
                    .addOnSuccessListener(query -> {
                        userList.clear();
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            if (Boolean.TRUE.equals(doc.getBoolean("isDeleted"))) {
                                continue;
                            }
                            userList.add(AdminUserItem.fromDocument(doc));
                        }
                        applyProfileSearchFilter();
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, R.string.admin_load_organizers_failed, Toast.LENGTH_SHORT).show());
        } else {
            db.collection("users")
                    .get()
                    .addOnSuccessListener(query -> {
                        userList.clear();
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            if (Boolean.TRUE.equals(doc.getBoolean("isDeleted"))) {
                                continue;
                            }
                            userList.add(AdminUserItem.fromDocument(doc));
                        }
                        applyProfileSearchFilter();
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, R.string.admin_load_profiles_failed, Toast.LENGTH_SHORT).show());
        }
    }

    private void applyProfileSearchFilter() {
        if (searchQueryNormalized.isEmpty()) {
            return;
        }
        List<AdminUserItem> filtered = new ArrayList<>();
        for (AdminUserItem user : userList) {
            if (profileMatchesSearch(user)) {
                filtered.add(user);
            }
        }
        userList.clear();
        userList.addAll(filtered);
    }

    private boolean profileMatchesSearch(AdminUserItem user) {
        return AdminSearchTextUtil.containsNormalized(user.getUid(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(user.getEmail(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(user.getName(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(user.getDisplayName(), searchQueryNormalized);
    }

    /**
     * Called when the admin taps "Remove Organizer Privileges".
     *
     * @param user selected user
     */
    @Override
    public void onRemoveOrganizerClicked(AdminUserItem user) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_dialog_remove_organizer_title)
                .setMessage(R.string.admin_dialog_remove_organizer_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> removeOrganizerPrivileges(user))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Called when the admin taps "Delete Profile".
     *
     * @param user selected user
     */
    @Override
    public void onDeleteProfileClicked(AdminUserItem user) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_dialog_delete_profile_title)
                .setMessage(R.string.admin_dialog_delete_profile_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> deleteProfileAndBanEmail(user))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Removes organizer privileges while still allowing the user to remain
     * a normal user and/or entrant.
     * <p>
     * This also permanently blocks the user from becoming an organizer again
     * by setting organizerBanned to true.
     *
     * @param user selected organizer profile
     */
    private void removeOrganizerPrivileges(AdminUserItem user) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isOrganizer", false);
        updates.put("organizerBanned", true);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    /**
                     * Notify the affected user through the existing in-app
                     * notification system.
                     */
                    AdminNotificationHelper.sendAdminNotification(
                            user.getUid(),
                            null,
                            "Organizer privileges removed",
                            "Your organizer privileges were removed by an administrator for violating app policy.",
                            "admin_remove_organizer"
                    );

                    /**
                     * Write an admin audit log entry.
                     */
                    AdminNotificationHelper.logAdminAction(
                            adminEmail,
                            "profile",
                            user.getUid(),
                            "remove_organizer",
                            "Organizer privileges removed and organizerBanned set to true."
                    );

                    Toast.makeText(this, R.string.admin_toast_organizer_removed, Toast.LENGTH_SHORT).show();
                    loadProfiles();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.admin_toast_organizer_remove_failed, Toast.LENGTH_SHORT).show());
    }

    /**
     * Soft-deletes the user profile and bans the email from signing up again.
     * <p>
     * This does not delete Firebase Authentication from the client because
     * that is risky for moderation flows. Instead, it:
     * - marks the profile as deleted
     * - removes organizer privileges if present
     * - records the banned email in a banned_emails collection
     *
     * @param user selected user profile
     */
    private void deleteProfileAndBanEmail(AdminUserItem user) {
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("isDeleted", true);
        userUpdates.put("deletedByAdmin", true);
        userUpdates.put("isOrganizer", false);
        userUpdates.put("updatedAt", FieldValue.serverTimestamp());

        String normalizedEmail = user.getEmail() == null
                ? ""
                : user.getEmail().toLowerCase(Locale.ROOT).trim();

        Map<String, Object> bannedEmailDoc = new HashMap<>();
        bannedEmailDoc.put("email", normalizedEmail);
        bannedEmailDoc.put("reason", "Profile removed by administrator");
        bannedEmailDoc.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(user.getUid())
                .update(userUpdates)
                .addOnSuccessListener(unused -> {
                    Runnable finishOk = () -> {
                        AdminNotificationHelper.logAdminAction(
                                adminEmail,
                                "profile",
                                user.getUid(),
                                "delete_profile",
                                normalizedEmail.isEmpty()
                                        ? "Profile soft-deleted (guest / no email; skipped banned_emails)."
                                        : "Profile soft-deleted and email banned from future sign-up."
                        );
                        Toast.makeText(this, R.string.admin_toast_profile_deleted, Toast.LENGTH_SHORT).show();
                        loadProfiles();
                    };
                    if (normalizedEmail.isEmpty()) {
                        finishOk.run();
                        return;
                    }
                    db.collection("banned_emails")
                            .document(normalizedEmail)
                            .set(bannedEmailDoc)
                            .addOnSuccessListener(unused2 -> finishOk.run())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, R.string.admin_toast_banned_email_failed, Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.admin_toast_profile_delete_failed, Toast.LENGTH_SHORT).show());
    }

}
