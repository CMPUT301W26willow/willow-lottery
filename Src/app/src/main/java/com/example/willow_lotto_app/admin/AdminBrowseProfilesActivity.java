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

/** Admin screen to browse users, remove organizer role, or delete profiles. */
public class AdminBrowseProfilesActivity extends AppCompatActivity implements AdminProfileAdapter.AdminProfileActionListener{

    /**
     * Intent extra key for {@link #MODE_ORGANIZERS} or {@link #MODE_ENTRANTS}.
     */
    public static final String EXTRA_MODE = "mode";

    /**
     * Value for {@link #EXTRA_MODE}: list users with {@code isOrganizer=true}.
     */
    public static final String MODE_ORGANIZERS = "organizers";

    /**
     * Value for {@link #EXTRA_MODE}: list all non-deleted users (default if extra missing).
     */
    public static final String MODE_ENTRANTS = "entrants";

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private String mode;
    private String adminEmail;
    private final List<AdminUserItem> userList = new ArrayList<>();
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

        adapter = new AdminProfileAdapter(userList, this, mode); // mode drives toolbar title + row actions
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

    // Organizers: query isOrganizer==true. Entrants: all users. Always skip isDeleted; then search filter.
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

    // Narrows userList in place when dashboard passed a search query
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

    /**
     * @param user profile row
     * @return true if uid, email, name, or displayName matches {@link #searchQueryNormalized}
     */
    private boolean profileMatchesSearch(AdminUserItem user) {
        return AdminSearchTextUtil.containsNormalized(user.getUid(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(user.getEmail(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(user.getName(), searchQueryNormalized)
                || AdminSearchTextUtil.containsNormalized(user.getDisplayName(), searchQueryNormalized);
    }

    /**
     * @param user organizer row selected for demotion
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
     * @param user profile selected for soft-delete and optional email ban
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
     * @param user user document to set {@code isOrganizer=false}, {@code organizerBanned=true}
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
                    AdminNotificationHelper.sendAdminNotification(
                            user.getUid(),
                            null,
                            "Organizer privileges removed",
                            "Your organizer privileges were removed by an administrator for violating app policy.",
                            "admin_remove_organizer"
                    );

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
     * @param user profile to mark deleted; non-empty email also writes {@code banned_emails/{email}}
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
