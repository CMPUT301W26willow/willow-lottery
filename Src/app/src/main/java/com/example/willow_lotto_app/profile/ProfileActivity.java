package com.example.willow_lotto_app.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.auth.LoginActivity;
import com.example.willow_lotto_app.home.MainActivity;
import com.google.firebase.auth.FirebaseUser;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import com.example.willow_lotto_app.events.CreateEventActivity;
import com.example.willow_lotto_app.events.Event;
import com.example.willow_lotto_app.events.EventsActivity;
import com.example.willow_lotto_app.notification.NotificationActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.willow_lotto_app.admin.AdminAccessUtil;
import com.example.willow_lotto_app.admin.AdminDashboardActivity;
import com.example.willow_lotto_app.organizer.ui.OrganizerDashboardActivity;
import com.example.willow_lotto_app.organizer.ui.OrganizerMyEventsActivity;

/*
 * Profile + notification toggle on users/{uid}, registration history, organizer shortcuts, admin if allow-listed.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String REGISTRATIONS_COLLECTION = "registrations";

    Switch notificationsOptInSwitch;

    private View profileFieldsView;
    private View profileFieldsEdit;
    private LinearLayout profileHeaderEditAction;
    private ImageView profileHeaderEditIcon;
    private TextView profileHeaderEditLabel;
    private TextView profileSignOut;
    private TextView profileCardName;
    private TextView profileCardEmail;
    private TextView profileCardPhone;
    private TextView profileAccountMemberSince;
    private TextView profileAccountEventsJoined;
    private View profileGuestBanner;
    private View profileDangerSection;
    private View profileOrganizerSection;
    private View profileAdminSection;

    private String lastLoadedName = "";
    private String lastLoadedEmail = "";
    private String lastLoadedPhone = "";
    private boolean editMode;

    //references to UI elements
    EditText nameInput, emailInput, phoneInput;
    // Button UI references from the profile screen
    Button saveButton, cancelButton, organizerDashboardButton, organizerMyEventsButton, deleteButton, registerButton, adminDashboardButton;    // references to the NAV( home, events, notifications, profile)
    // Button UI references from the profile screen

    BottomNavigationView bottomNav;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Called when the ProfileActivity screen is first created.
    // Initializes the UI layout, connects all UI elements to the code,
    // sets up Firebase instances, loads the user's profile data,
    // and sets click listeners for buttons and bottom navigation.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        profileFieldsView = findViewById(R.id.profile_fields_view);
        profileFieldsEdit = findViewById(R.id.profile_fields_edit);
        profileHeaderEditAction = findViewById(R.id.profile_header_edit_action);
        profileHeaderEditIcon = findViewById(R.id.profile_header_edit_icon);
        profileHeaderEditLabel = findViewById(R.id.profile_header_edit_label);
        profileSignOut = findViewById(R.id.profile_sign_out);
        profileCardName = findViewById(R.id.profile_card_name_value);
        profileCardEmail = findViewById(R.id.profile_card_email_value);
        profileCardPhone = findViewById(R.id.profile_card_phone_value);
        profileAccountMemberSince = findViewById(R.id.profile_account_member_since);
        profileAccountEventsJoined = findViewById(R.id.profile_account_events_joined);
        profileGuestBanner = findViewById(R.id.profile_guest_banner);
        profileDangerSection = findViewById(R.id.profile_danger_section);
        profileOrganizerSection = findViewById(R.id.profile_organizer_section);
        profileAdminSection = findViewById(R.id.profile_admin_section);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteProfileButton);
        cancelButton = findViewById(R.id.cancelButton);
        registerButton = findViewById(R.id.registerButton);
        organizerDashboardButton = findViewById(R.id.organizerDashboardButton);
        organizerMyEventsButton = findViewById(R.id.organizerMyEventsButton);
        adminDashboardButton = findViewById(R.id.adminDashboardButton);
        bottomNav = findViewById(R.id.bottom_nav);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        notificationsOptInSwitch = findViewById(R.id.notificationsOptInSwitch);
        loadNotificationPreference();
        notificationsOptInSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveNotificationPreference(isChecked));

        profileSignOut.setOnClickListener(v -> signOut());
        profileHeaderEditAction.setOnClickListener(v -> {
            if (editMode) {
                cancelEdit();
            } else {
                enterEditMode();
            }
        });

        bottomNav.setSelectedItemId(R.id.nav_profile);

        applyGuestUiMode();
        loadProfile();
        loadAccountSummary();

        saveButton.setOnClickListener(v -> saveProfile());
        cancelButton.setOnClickListener(v -> cancelEdit());
        deleteButton.setOnClickListener(v -> confirmDeleteProfile());
        registerButton.setOnClickListener(v -> showRegistrationHistory());
        organizerDashboardButton.setOnClickListener(v -> openOrganizerDashboardForLatestEvent());
        organizerMyEventsButton.setOnClickListener(v -> openOrganizerMyEvents());
        adminDashboardButton.setOnClickListener(v -> openAdminDashboard());
        applyAdminEntryVisibility();
        // click listener for Bottom Navigation to guide to different navigation pages of the app

        bottomNav.setOnItemSelectedListener(item -> {
            // click listener for home page
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            // click listener for events page
            if (item.getItemId() == R.id.nav_events) {
                startActivity(new Intent(this, EventsActivity.class));
                finish();
                return true;
            }
            // click listener for notifications page
            if (item.getItemId() == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                finish();
                return true;
            }
            // click listener for profile page
            if (item.getItemId() == R.id.nav_profile) {
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyAdminEntryVisibility();
    }

    /**
     * Shows the admin dashboard entry only for signed-in, non-guest allow-listed emails.
     */
    private void applyAdminEntryVisibility() {
        if (profileAdminSection == null || mAuth == null) {
            return;
        }
        FirebaseUser u = mAuth.getCurrentUser();
        boolean show = u != null && !u.isAnonymous() && AdminAccessUtil.isAdminEmail(u.getEmail());
        profileAdminSection.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Firebase anonymous sessions — same app features except profile Edit (full name shown as Guest).
     */
    private boolean isGuestUser() {
        return mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isAnonymous();
    }

    private void applyGuestUiMode() {
        if (!isGuestUser()) {
            profileGuestBanner.setVisibility(View.GONE);
            profileHeaderEditAction.setVisibility(View.VISIBLE);
            return;
        }
        profileGuestBanner.setVisibility(View.VISIBLE);
        profileHeaderEditAction.setVisibility(View.GONE);
        profileFieldsEdit.setVisibility(View.GONE);
        profileFieldsView.setVisibility(View.VISIBLE);
        editMode = false;
        profileHeaderEditIcon.setVisibility(View.VISIBLE);
        profileHeaderEditLabel.setText(R.string.profile_edit);
    }

    private void loadNotificationPreference() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");
                        // If the field is absent (null), default the switch to ON
                        notificationsOptInSwitch.setChecked(notificationsEnabled == null || notificationsEnabled);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notification preference.", Toast.LENGTH_SHORT).show());
    }

    private void saveNotificationPreference(boolean isEnabled) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .update("notificationsEnabled", isEnabled)
                .addOnSuccessListener(aVoid -> {
                    String msg = isEnabled ? "Notifications enabled" : "Notifications disabled";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update notification preference.", Toast.LENGTH_SHORT).show());
    }


    private void openOrganizerDashboardForLatestEvent() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No signed-in user.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Task<QuerySnapshot> owned = db.collection("events").whereEqualTo("organizerId", uid).get();
        Task<QuerySnapshot> co = db.collection("events").whereArrayContains("coOrganizerIds", uid).get();
        Tasks.whenAllComplete(owned, co).addOnCompleteListener(task -> {
            if (!owned.isSuccessful() && !co.isSuccessful()) {
                Toast.makeText(this, "Could not open organizer dashboard.", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Event> byId = new LinkedHashMap<>();
            if (owned.isSuccessful() && owned.getResult() != null) {
                for (DocumentSnapshot doc : owned.getResult().getDocuments()) {
                    addProfileEventDoc(doc, byId);
                }
            }
            if (co.isSuccessful() && co.getResult() != null) {
                for (DocumentSnapshot doc : co.getResult().getDocuments()) {
                    addProfileEventDoc(doc, byId);
                }
            }
            List<Event> list = new ArrayList<>(byId.values());
            if (list.isEmpty()) {
                startActivity(new Intent(ProfileActivity.this, CreateEventActivity.class));
                return;
            }
            Collections.sort(list, PROFILE_EVENT_BY_DATE_DESC);
            String eventId = list.get(0).getId();
            Intent intent = new Intent(ProfileActivity.this, OrganizerDashboardActivity.class);
            intent.putExtra(OrganizerDashboardActivity.EXTRA_EVENT_ID, eventId);
            startActivity(intent);
        });
    }

    private static void addProfileEventDoc(DocumentSnapshot doc, Map<String, Event> byId) {
        Boolean isDeleted = doc.getBoolean("isDeleted");
        if (isDeleted != null && isDeleted) {
            return;
        }
        Boolean isPrivate = doc.getBoolean("isPrivate");
        if (isPrivate != null && isPrivate) {
            return;
        }
        Event e = new Event();
        e.setId(doc.getId());
        e.setName(doc.getString("name"));
        e.setDate(doc.getString("date"));
        e.setOrganizerId(doc.getString("organizerId"));
        byId.put(doc.getId(), e);
    }

    private static final Comparator<Event> PROFILE_EVENT_BY_DATE_DESC = (a, b) -> {
        String da = a.getDate() != null ? a.getDate() : "";
        String db = b.getDate() != null ? b.getDate() : "";
        return db.compareTo(da);
    };

    private void openOrganizerMyEvents() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No signed-in user.", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, OrganizerMyEventsActivity.class));
    }

    /**
     * Opens the admin dashboard when the signed-in Firebase Auth email is on the allow-list.
     * Uses the same check as {@link AdminDashboardActivity} ({@link AdminAccessUtil#isAdminEmail(String)}).
     * Profile previously read only Firestore {@code users.email}, which is often missing or stale after
     * sign-in — that mismatch is what made admin access appear to “break”.
     */
    private void openAdminDashboard() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No signed-in user.", Toast.LENGTH_SHORT).show();
            return;
        }
        String email = mAuth.getCurrentUser().getEmail();
        if (AdminAccessUtil.isAdminEmail(email)) {
            startActivity(new Intent(ProfileActivity.this, AdminDashboardActivity.class));
        } else {
            Toast.makeText(this, R.string.admin_permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads the user profile by filling the login areas with information if it already exists
     */
    private void loadProfile() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        if (isGuestUser()) {
                            lastLoadedName = "";
                            lastLoadedEmail = "";
                            lastLoadedPhone = "";
                        } else {
                            String name = document.getString("name");
                            String email = document.getString("email");
                            String phone = document.getString("phone");
                            lastLoadedName = name != null ? name : "";
                            lastLoadedEmail = email != null ? email : "";
                            lastLoadedPhone = phone != null ? phone : "";
                        }
                    } else {
                        lastLoadedName = "";
                        lastLoadedEmail = "";
                        lastLoadedPhone = "";
                    }
                    nameInput.setText(lastLoadedName);
                    emailInput.setText(lastLoadedEmail);
                    phoneInput.setText(lastLoadedPhone);
                    applyCardValues(lastLoadedName, lastLoadedEmail, lastLoadedPhone);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void applyCardValues(String name, String email, String phone) {
        if (isGuestUser()) {
            profileCardName.setText(R.string.profile_guest_display_name);
            profileCardEmail.setText(R.string.profile_guest_email_unavailable);
            profileCardPhone.setText(R.string.profile_phone_not_set);
            return;
        }
        profileCardName.setText(name != null && !name.isEmpty() ? name : "—");
        profileCardEmail.setText(email != null && !email.isEmpty() ? email : "—");
        if (phone != null && !phone.trim().isEmpty()) {
            profileCardPhone.setText(phone);
        } else {
            profileCardPhone.setText(R.string.profile_phone_not_set);
        }
    }

    private void enterEditMode() {
        if (isGuestUser()) {
            return;
        }
        nameInput.setText(lastLoadedName);
        emailInput.setText(lastLoadedEmail);
        phoneInput.setText(lastLoadedPhone);
        setEditMode(true);
    }

    private void cancelEdit() {
        nameInput.setText(lastLoadedName);
        emailInput.setText(lastLoadedEmail);
        phoneInput.setText(lastLoadedPhone);
        setEditMode(false);
    }

    private void setEditMode(boolean editing) {
        if (isGuestUser()) {
            editing = false;
        }
        editMode = editing;
        profileFieldsView.setVisibility(editing ? View.GONE : View.VISIBLE);
        profileFieldsEdit.setVisibility(editing ? View.VISIBLE : View.GONE);
        profileHeaderEditIcon.setVisibility(editing ? View.GONE : View.VISIBLE);
        profileHeaderEditLabel.setText(editing ? R.string.profile_cancel_short : R.string.profile_edit);
    }

    private void signOut() {
        mAuth.signOut();
        getSharedPreferences("appData", MODE_PRIVATE).edit()
                .putBoolean("rememberUser", false)
                .apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadAccountSummary() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }
        if (isGuestUser()) {
            profileAccountMemberSince.setText(R.string.profile_guest_session_label);
        } else if (user.getMetadata() != null && user.getMetadata().getCreationTimestamp() > 0) {
            long ts = user.getMetadata().getCreationTimestamp();
            String formatted = new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(ts));
            profileAccountMemberSince.setText(getString(R.string.profile_member_since, formatted));
        } else {
            profileAccountMemberSince.setText(R.string.profile_member_since_unknown);
        }
        String uid = user.getUid();
        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap ->
                        profileAccountEventsJoined.setText(
                                getString(R.string.profile_events_joined, snap.size())))
                .addOnFailureListener(e ->
                        profileAccountEventsJoined.setText(getString(R.string.profile_events_joined, 0)));
    }

    /**
     * Sends an alert for the user to confirm whether they want to delete there account or not
     */
    private void confirmDeleteProfile() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    /**
     * Actual delete profile function, deletes the user account both in the app and the firebase document
     */

    private void deleteProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> {
                    mAuth.getCurrentUser().delete()
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Saves the Text input into the firebase in the users area
     */

    private void saveProfile() {
        if (isGuestUser()) {
            return;
        }
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        String error = validateProfileInput(name, email);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("Name", name);
        user.put("Email", email);
        user.put("Phone", phone);

        db.collection("users")
                .document(uid)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    lastLoadedName = name;
                    lastLoadedEmail = email;
                    lastLoadedPhone = phone;
                    applyCardValues(name, email, phone);
                    setEditMode(false);
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error Saving Profile", Toast.LENGTH_SHORT).show());
    }

    // Reads the registeredEvents array stored directly on the user's document,
    // then looks up each event name from the events collection

    /**
     * Validates profile fields; returns error message or null if valid.
     */
    public static String validateProfileInput(String name, String email) {
        if (name == null || name.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
            return "Name and Email required";
        }
        return null;
    }

    /**
     * Retrieves and displays current user registration history in an alert dialogue
     */
// CHANGED: reads eventName directly from the registration document
// instead of doing a secondary lookup on the events collection
    private void showRegistrationHistory() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Registration History")
                                .setMessage("You have not registered for any events yet.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    // CHANGED: read eventName directly from the registration document
                    // no secondary events collection lookup needed
                    StringBuilder history = new StringBuilder();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String eventName = doc.getString("eventName");
                        String status = doc.getString("status");
                        history.append("• ")
                                .append(eventName != null ? eventName : "(Unknown Event)")
                                .append(status != null ? " — " + status : "")
                                .append("\n\n");
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Registration History")
                            .setMessage(history.toString().trim())
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load registration history", Toast.LENGTH_SHORT).show());
    }
}