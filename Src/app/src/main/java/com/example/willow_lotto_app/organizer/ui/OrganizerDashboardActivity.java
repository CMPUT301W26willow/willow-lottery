package com.example.willow_lotto_app.organizer.ui;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.example.willow_lotto_app.entrant.EntrantMapActivity;
import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.events.CancelledEntrantsActivity;
import com.example.willow_lotto_app.events.CreateEventActivity;
import com.example.willow_lotto_app.events.InvitedEntrantsActivity;
import com.example.willow_lotto_app.events.PrivateEventInviteManager;
import com.example.willow_lotto_app.events.poster.EventPosterLoader;
import com.example.willow_lotto_app.notification.NotificationStore;
import com.example.willow_lotto_app.notification.NotificationTypes;
import com.example.willow_lotto_app.notification.UserNotification;
import com.example.willow_lotto_app.organizer.EventOrganizerAccess;
import com.example.willow_lotto_app.organizer.OrganizerEntrantExportHelper;
import com.example.willow_lotto_app.organizer.OrganizerLotteryManager;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * One-event organizer UI (needs EXTRA_EVENT_ID): waitlist list, lottery, export, map, invites.
 */

public class OrganizerDashboardActivity extends AppCompatActivity {

    private ListView waitingListView;
    private OrganizerWaitlistAdapter waitlistAdapter;

    public static final String EXTRA_EVENT_ID = "event_id";
    private TextInputEditText drawSizeInput;
    private TextView statValueWaitlisted;
    private TextView statValueDraw;
    private TextView statValueFilled;
    private TextView statValueOpen;
    private TextView currentEventName;
    private TextView currentEventStatus;
    private TextView currentEventDate;
    private TextView currentEventWaitlist;
    private TextView currentEventCapacityLabel;
    private ProgressBar currentEventProgress;
    private ShapeableImageView currentEventPoster;
    private Button createEventButton;
    private Button runLotteryButton;
    private Button drawReplacementButton;
    private Button backToProfileButton;
    private Button exportEntrantsCsvButton;
    private Button notifyWaitlistButton;
    private Switch geolocationSwitch;

    private String eventId;
    private OrganizerLotteryManager lotteryManager;
    private RegistrationStore registrationRepository;
    private FirebaseFirestore db;

    // CHANGED: fields for private-event entrant search and invite
    private TextInputEditText searchEntrantInput;
    private Button searchEntrantButton;
    private Button inviteEntrantButton;
    private ListView searchResultsListView;
    private ArrayAdapter<String> searchResultsAdapter;
    private ArrayList<String> searchResultNames;
    private ArrayList<String> searchResultUserIds;
    private String selectedUserId;
    private PrivateEventInviteManager privateEventInviteManager;
    private NotificationStore notificationStore;

    @Nullable
    private String primaryOrganizerId;
    private TextView coOrganizerSummary;
    private MaterialButton addCoOrganizerButton;
    private MaterialButton coManageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        db = FirebaseFirestore.getInstance();
        lotteryManager = new OrganizerLotteryManager();
        registrationRepository = new RegistrationStore();
        notificationStore = new NotificationStore();

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.organizer_my_events_sign_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    if (!EventOrganizerAccess.canManageEvent(doc, user.getUid())) {
                        Toast.makeText(this, R.string.organizer_access_denied, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    primaryOrganizerId = doc.getString("organizerId");
                    bindOrganizerDashboardUi();
                })
                .addOnFailureListener(e -> {
                    Log.e("OrganizerDashboard", "access check failed", e);
                    Toast.makeText(this, R.string.organizer_access_denied, Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void bindOrganizerDashboardUi() {
        MaterialToolbar toolbar = findViewById(R.id.organizer_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        statValueWaitlisted = findViewById(R.id.organizer_stat_value_waitlisted);
        statValueDraw = findViewById(R.id.organizer_stat_value_draw);
        statValueFilled = findViewById(R.id.organizer_stat_value_filled);
        statValueOpen = findViewById(R.id.organizer_stat_value_open);
        currentEventName = findViewById(R.id.organizer_current_event_name);
        currentEventStatus = findViewById(R.id.organizer_current_event_status);
        currentEventDate = findViewById(R.id.organizer_current_event_date);
        currentEventWaitlist = findViewById(R.id.organizer_current_event_waitlist);
        currentEventCapacityLabel = findViewById(R.id.organizer_current_event_capacity_label);
        currentEventProgress = findViewById(R.id.organizer_current_event_progress);
        currentEventPoster = findViewById(R.id.organizer_current_event_poster);

        findViewById(R.id.organizer_manage_events_btn).setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerMyEventsActivity.class)));

        waitingListView = findViewById(R.id.waitingListView);
        waitlistAdapter = new OrganizerWaitlistAdapter(this);
        waitlistAdapter.setListener(new OrganizerWaitlistAdapter.Listener() {
            @Override
            public void onInvite(Registration registration) {
                lotteryManager.promoteWaitlistedToInvited(eventId, registration,
                        new OrganizerLotteryManager.LotteryCallback() {
                            @Override
                            public void onSuccess(String message, List<Registration> affectedRegistrations) {
                                Toast.makeText(OrganizerDashboardActivity.this, message, Toast.LENGTH_LONG).show();
                                loadWaitingList();
                                loadDashboardSummary();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                lotteryErrorToast(e);
                            }
                        });
            }

            @Override
            public void onRemove(Registration registration) {
                new AlertDialog.Builder(OrganizerDashboardActivity.this)
                        .setTitle(R.string.organizer_waitlist_remove)
                        .setMessage(R.string.organizer_waitlist_remove_confirm)
                        .setPositiveButton(android.R.string.ok, (d, w) ->
                                registrationRepository.deleteRegistration(registration.getId(),
                                        new RegistrationStore.SimpleCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Toast.makeText(OrganizerDashboardActivity.this,
                                                        R.string.organizer_waitlist_removed, Toast.LENGTH_SHORT).show();
                                                loadWaitingList();
                                                loadDashboardSummary();
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                lotteryErrorToast(e);
                                            }
                                        }))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        waitingListView.setAdapter(waitlistAdapter);

        drawSizeInput = findViewById(R.id.drawSizeInput);
        drawSizeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveDrawSizeOnly();
            }
        });
        createEventButton = findViewById(R.id.createEventButton);
        runLotteryButton = findViewById(R.id.runLotteryButton);
        drawReplacementButton = findViewById(R.id.drawReplacementButton);
        backToProfileButton = findViewById(R.id.backToProfileButton);
        exportEntrantsCsvButton = findViewById(R.id.exportEntrantsCsvButton);
        geolocationSwitch = findViewById(R.id.geolocationSwitch);
        notifyWaitlistButton = findViewById(R.id.notifyWaitlistButton);
        notifyWaitlistButton.setOnClickListener(v -> notifyWaitingList());

        createEventButton.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        exportEntrantsCsvButton.setOnClickListener(v ->
                OrganizerEntrantExportHelper.exportEntrantsCsv(this, eventId));

        backToProfileButton.setOnClickListener(v -> finish());

        runLotteryButton.setOnClickListener(v -> runLottery());
        drawReplacementButton.setOnClickListener(v -> drawReplacement());

        Button viewInvitedButton = findViewById(R.id.viewInvitedButton);
        viewInvitedButton.setOnClickListener(v -> openInvitedEntrants());

        Button viewCancelledButton = findViewById(R.id.viewCancelledButton);
        viewCancelledButton.setOnClickListener(v -> {
            Intent i = new Intent(this, CancelledEntrantsActivity.class);
            i.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID, eventId);
            startActivity(i);
        });

        Button viewMapButton = findViewById(R.id.viewMapButton);
        viewMapButton.setOnClickListener(v -> openEntrantMap());

        geolocationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveGeolocationSetting(isChecked));

        coOrganizerSummary = findViewById(R.id.organizer_co_summary);
        addCoOrganizerButton = findViewById(R.id.addCoOrganizerButton);
        coManageButton = findViewById(R.id.organizer_co_manage_button);
        addCoOrganizerButton.setOnClickListener(v -> addCoOrganizerFromSearch());
        coManageButton.setOnClickListener(v -> showRemoveCoOrganizerDialog());

        setupStatCardClicks();

        loadEventDrawSize();
        loadWaitingList();
        loadGeolocationSetting();
        loadDashboardSummary();

        // US 2:01:03
        // CHANGED: initialize private-event invite manager and UI
        privateEventInviteManager = new PrivateEventInviteManager();

        searchEntrantInput = findViewById(R.id.searchEntrantInput);
        searchEntrantButton = findViewById(R.id.searchEntrantButton);
        inviteEntrantButton = findViewById(R.id.inviteEntrantButton);
        searchResultsListView = findViewById(R.id.searchResultsListView);

        searchResultNames = new ArrayList<>();
        searchResultUserIds = new ArrayList<>();
        searchResultsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, searchResultNames);
        searchResultsListView.setAdapter(searchResultsAdapter);

        searchEntrantButton.setOnClickListener(v -> searchEntrants());
        inviteEntrantButton.setOnClickListener(v -> inviteSelectedEntrant());

        searchResultsListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedUserId = searchResultUserIds.get(position);
            Toast.makeText(this, "Selected: " + searchResultNames.get(position), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Loads the waiting list from Firebase using the registration repository.
     */
    public void loadWaitingList() {
        registrationRepository.getRegistrationsForEventByStatus(
                eventId,
                RegistrationStatus.WAITLISTED.getValue(),
                new RegistrationStore.RegistrationListCallback() {
                    @Override
                    public void onSuccess(List<Registration> registrations) {
                        if (registrations.isEmpty()) {
                            waitlistAdapter.setRows(new ArrayList<>());
                            loadDashboardSummary();
                            return;
                        }

                        List<OrganizerWaitlistAdapter.Row> rows = new ArrayList<>();
                        for (Registration registration : registrations) {
                            rows.add(new OrganizerWaitlistAdapter.Row(registration, ""));
                        }
                        waitlistAdapter.setRows(rows);

                        final int total = registrations.size();
                        final int[] done = {0};
                        for (int i = 0; i < registrations.size(); i++) {
                            final int index = i;
                            Registration reg = registrations.get(i);
                            String uid = reg.getUserId();
                            if (uid == null || uid.isEmpty()) {
                                rows.get(index).displayName = "—";
                                done[0]++;
                                if (done[0] == total) {
                                    waitlistAdapter.notifyDataSetChanged();
                                    loadDashboardSummary();
                                }
                                continue;
                            }
                            db.collection("users")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        String name = documentSnapshot.getString("name");
                                        String email = documentSnapshot.getString("email");
                                        if (name != null && !name.trim().isEmpty()) {
                                            rows.get(index).displayName = name.trim();
                                        } else if (email != null && !email.trim().isEmpty()) {
                                            rows.get(index).displayName = email.trim();
                                        } else {
                                            rows.get(index).displayName = uid;
                                        }
                                        done[0]++;
                                        if (done[0] == total) {
                                            waitlistAdapter.notifyDataSetChanged();
                                            loadDashboardSummary();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        rows.get(index).displayName = uid;
                                        done[0]++;
                                        if (done[0] == total) {
                                            waitlistAdapter.notifyDataSetChanged();
                                            loadDashboardSummary();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("OrganizerDashboard", "Failed to load waiting list", e);
                        Toast.makeText(OrganizerDashboardActivity.this, "Failed to load waiting list.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

    }

    /**
     * Loads the draw size for the event from Firestore and populates the input field.
     */
    private void loadEventDrawSize() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;
                    Long drawSize = documentSnapshot.getLong("drawSize");
                    if (drawSize != null) {
                        drawSizeInput.setText(String.valueOf(drawSize.intValue()));
                    }
                })
                .addOnFailureListener(e -> Log.e("OrganizerDashboard", "Failed to load draw size", e));
    }

    /**
     * Sends a notification to all entrants currently on the waiting list.
     * Called when the organizer taps the Notify Waiting List button.
     */
    private void notifyWaitingList() {
        lotteryManager.notifyWaitlistedEntrants(eventId, new OrganizerLotteryManager.LotteryCallback() {
            @Override
            public void onSuccess(String message, List<Registration> affectedRegistrations) {
                Toast.makeText(OrganizerDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                lotteryErrorToast(e);
            }
        });
    }

    private void openInvitedEntrants() {
        Intent i = new Intent(this, InvitedEntrantsActivity.class);
        i.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_ID, eventId);
        startActivity(i);
    }

    private void openEntrantMap() {
        Intent i = new Intent(this, EntrantMapActivity.class);
        i.putExtra(EntrantMapActivity.EXTRA_EVENT_ID, eventId);
        startActivity(i);
    }

    /**
     * Fills stat cards and the “This event” summary from Firestore.
     */
    private void loadDashboardSummary() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }
                    String name = documentSnapshot.getString("name");
                    String date = documentSnapshot.getString("date");
                    String posterUri = documentSnapshot.getString("posterUri");
                    Long drawSizeLong = documentSnapshot.getLong("drawSize");
                    int drawSize = drawSizeLong == null ? 0 : drawSizeLong.intValue();
                    String regEnd = documentSnapshot.getString("registrationEnd");

                    currentEventName.setText(name != null && !name.isEmpty() ? name : getString(R.string.notif_event_fallback_name));
                    currentEventDate.setText(date != null && !date.isEmpty() ? date : "—");
                    statValueDraw.setText(String.valueOf(drawSize));
                    updateRegistrationStatusBadge(regEnd);
                    EventPosterLoader.load(OrganizerDashboardActivity.this, posterUri, currentEventPoster, eventId);
                    refreshCoOrganizerSummary(documentSnapshot);

                    registrationRepository.getRegistrationsForEvent(eventId, new RegistrationStore.RegistrationListCallback() {
                        @Override
                        public void onSuccess(List<Registration> registrations) {
                            int waitlisted = 0;
                            int filledSlots = 0;
                            for (Registration r : registrations) {
                                RegistrationStatus s = r.getStatusEnum();
                                if (s == RegistrationStatus.WAITLISTED) {
                                    waitlisted++;
                                }
                                if (s == RegistrationStatus.INVITED || s == RegistrationStatus.ACCEPTED) {
                                    filledSlots++;
                                }
                            }
                            statValueWaitlisted.setText(String.valueOf(waitlisted));
                            statValueFilled.setText(String.valueOf(filledSlots));
                            int open = Math.max(0, drawSize - filledSlots);
                            statValueOpen.setText(String.valueOf(open));
                            currentEventWaitlist.setText(getString(R.string.home_event_waitlist_count, waitlisted));
                            int pct = drawSize > 0 ? (int) Math.min(100L, (100L * filledSlots) / drawSize) : 0;
                            currentEventProgress.setProgress(pct);
                            currentEventCapacityLabel.setText(getString(R.string.organizer_capacity_percent, pct));
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("OrganizerDashboard", "Failed to load registrations for summary", e);
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e("OrganizerDashboard", "Failed to load event for summary", e));
    }

    private void updateRegistrationStatusBadge(@Nullable String registrationEnd) {
        if (registrationEnd == null || registrationEnd.trim().isEmpty()) {
            currentEventStatus.setText(R.string.organizer_registration_open);
            return;
        }
        Date end = tryParseEventDate(registrationEnd.trim());
        if (end == null) {
            currentEventStatus.setText(R.string.organizer_registration_open);
            return;
        }
        if (new Date().after(end)) {
            currentEventStatus.setText(R.string.organizer_registration_closed);
        } else {
            currentEventStatus.setText(R.string.organizer_registration_open);
        }
    }

    @Nullable
    private static Date tryParseEventDate(String value) {
        String[] patterns = {
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "M/d/yyyy",
                "dd/MM/yyyy",
                "yyyy-MM-dd HH:mm",
                "MMM d, yyyy"
        };
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p, Locale.getDefault()).parse(value);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    /**
     * Loads the current geolocation requirement setting for the event from Firestore
     * and updates the switch UI accordingly.
     */
    private void loadGeolocationSetting() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean geolocationRequired = documentSnapshot.getBoolean("geolocationRequired");
                        if (geolocationRequired != null) {
                            geolocationSwitch.setChecked(geolocationRequired);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("OrganizerDashboard", "Failed to load geolocation setting", e));
    }

    /**
     * Saves the geolocation requirement setting for the event to Firestore.
     *
     * @param isEnabled Whether geolocation is required for the event.
     */
    private void saveGeolocationSetting(boolean isEnabled) {
        db.collection("events")
                .document(eventId)
                .update("geolocationRequired", isEnabled)
                .addOnSuccessListener(aVoid -> {
                    String msg = isEnabled ? "Geolocation enabled" : "Geolocation disabled";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("OrganizerDashboard", "Failed to save geolocation setting", e);
                    Toast.makeText(this, "Failed to update geolocation setting", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Saves only the draw size value entered by the organizer.
     *
     * @since 31/03/2026
     */
    private void saveDrawSizeOnly() {
        String input = drawSizeInput.getText().toString().trim();

        if (input.isEmpty()) {
            return;
        }

        int drawSize;
        try {
            drawSize = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Draw size must be a number.", Toast.LENGTH_SHORT).show();
            return;
        }

        lotteryManager.setDrawSize(eventId, drawSize, new OrganizerLotteryManager.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                loadDashboardSummary();
            }

            @Override
            public void onFailure(Exception e) {
                lotteryErrorToast(e);
            }
        });
    }

    /**
     * Runs the lottery for the event using the draw size input.
     */
    private void runLottery() {
        String input = drawSizeInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Enter a draw size first.", Toast.LENGTH_SHORT).show();
            return;
        }

        int drawSize;
        try {
            drawSize = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Draw size must be a number.", Toast.LENGTH_SHORT).show();
            return;
        }

        lotteryManager.setDrawSize(eventId, drawSize, new OrganizerLotteryManager.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                lotteryManager.drawLotteryForEvent(eventId, drawSize, new OrganizerLotteryManager.LotteryCallback() {
                    @Override
                    public void onSuccess(String message, List<Registration> affectedRegistrations) {
                        Toast.makeText(OrganizerDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                        loadWaitingList();
                        loadDashboardSummary();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        lotteryErrorToast(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                lotteryErrorToast(e);
            }
        });
    }

    /**
     * Draws a replacement entrant for the event lottery.
     */
    private void drawReplacement() {
        saveDrawSizeOnly();
        lotteryManager.drawReplacement(eventId, new OrganizerLotteryManager.LotteryCallback() {
            @Override
            public void onSuccess(String message, List<Registration> affectedRegistrations) {
                Toast.makeText(OrganizerDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                loadWaitingList();
                loadDashboardSummary();
            }

            @Override
            public void onFailure(Exception e) {
                lotteryErrorToast(e);
            }
        });
    }

    // US 2:01:03
    // CHANGED: search entrants by name, phone, or email
    /**
     * Searches users by name, phone number, or email for private-event invitations.
     *
     * @since 31/03/2026
     */
    private void searchEntrants() {
        String query = searchEntrantInput.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter a name, phone, or email.", Toast.LENGTH_SHORT).show();
            return;
        }

        privateEventInviteManager.searchUsers(query, new PrivateEventInviteManager.UserSearchCallback() {
            @Override
            public void onSuccess(List<com.google.firebase.firestore.DocumentSnapshot> users) {
                searchResultNames.clear();
                searchResultUserIds.clear();
                selectedUserId = null;

                for (com.google.firebase.firestore.DocumentSnapshot user : users) {
                    String userId = user.getId();
                    String name = user.getString("name");
                    String email = user.getString("email");
                    String phone = user.getString("phone");

                    String display = (name != null ? name : "Unknown")
                            + " | "
                            + (email != null ? email : "No email")
                            + " | "
                            + (phone != null ? phone : "No phone");

                    searchResultNames.add(display);
                    searchResultUserIds.add(userId);
                }

                searchResultsAdapter.notifyDataSetChanged();

                if (users.isEmpty()) {
                    Toast.makeText(OrganizerDashboardActivity.this, "No matching entrants found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(OrganizerDashboardActivity.this, "Search failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Us 2:01:03
    // CHANGED: send private-event invitation to the selected entrant
    /**
     * Sends a private-event invitation to the currently selected entrant.
     *
     * @since 31/03/2026
     */
    private void inviteSelectedEntrant() {
        if (selectedUserId == null || selectedUserId.trim().isEmpty()) {
            Toast.makeText(this, "Select an entrant first.", Toast.LENGTH_SHORT).show();
            return;
        }

        privateEventInviteManager.inviteUserToPrivateEvent(
                eventId,
                selectedUserId,
                new PrivateEventInviteManager.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(OrganizerDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                        loadWaitingList();
                        loadDashboardSummary();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(OrganizerDashboardActivity.this, "Could not send invitation.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void lotteryErrorToast(@Nullable Exception e) {
        String detail = e != null && e.getMessage() != null && !e.getMessage().isEmpty()
                ? e.getMessage()
                : getString(R.string.organizer_lottery_error_unknown);
        Toast.makeText(this, getString(R.string.organizer_lottery_error, detail), Toast.LENGTH_LONG).show();
    }

    private void setupStatCardClicks() {
        findViewById(R.id.organizer_stat_card_waitlisted).setOnClickListener(v ->
                scrollToView(R.id.organizer_waiting_list_heading));
        findViewById(R.id.organizer_stat_card_draw).setOnClickListener(v -> {
            scrollToView(R.id.drawSizeInput);
            drawSizeInput.requestFocus();
        });
        findViewById(R.id.organizer_stat_card_filled).setOnClickListener(v -> openInvitedEntrants());
        findViewById(R.id.organizer_stat_card_open).setOnClickListener(v -> {
            scrollToView(R.id.runLotteryButton);
            Toast.makeText(this, R.string.organizer_stat_open_hint, Toast.LENGTH_SHORT).show();
        });
    }

    private void scrollToView(int viewId) {
        NestedScrollView scroll = findViewById(R.id.organizer_dashboard_scroll);
        View target = findViewById(viewId);
        if (scroll == null || target == null) {
            return;
        }
        scroll.post(() -> {
            Rect r = new Rect();
            target.getDrawingRect(r);
            scroll.offsetDescendantRectToMyCoords(target, r);
            scroll.smoothScrollTo(0, Math.max(0, r.top - 24));
        });
    }

    private void refreshCoOrganizerSummary(DocumentSnapshot eventDoc) {
        if (coOrganizerSummary == null) {
            return;
        }
        List<String> coIds = EventOrganizerAccess.readCoOrganizerIds(eventDoc);
        List<String> pendingRaw = EventOrganizerAccess.readPendingCoOrganizerIds(eventDoc);
        List<String> pendingOnly = new ArrayList<>();
        for (String p : pendingRaw) {
            if (!coIds.contains(p)) {
                pendingOnly.add(p);
            }
        }
        if (coIds.isEmpty() && pendingOnly.isEmpty()) {
            coOrganizerSummary.setText(getString(R.string.organizer_co_none));
            return;
        }
        List<String> rowUids = new ArrayList<>();
        List<Boolean> rowPending = new ArrayList<>();
        for (String uid : coIds) {
            rowUids.add(uid);
            rowPending.add(false);
        }
        for (String uid : pendingOnly) {
            rowUids.add(uid);
            rowPending.add(true);
        }
        final int n = rowUids.size();
        final String[] lines = new String[n];
        final int[] done = {0};
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String uid = rowUids.get(i);
            boolean pending = rowPending.get(i);
            db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(ud -> {
                        String name = ud.getString("name");
                        String email = ud.getString("email");
                        String base = name != null && !name.trim().isEmpty()
                                ? name.trim()
                                : (email != null && !email.trim().isEmpty() ? email.trim() : uid);
                        lines[idx] = pending ? getString(R.string.organizer_co_pending_label, base) : base;
                        done[0]++;
                        if (done[0] == n) {
                            appendCoSummaryLines(lines);
                        }
                    })
                    .addOnFailureListener(e -> {
                        lines[idx] = pending
                                ? getString(R.string.organizer_co_pending_label, uid)
                                : uid;
                        done[0]++;
                        if (done[0] == n) {
                            appendCoSummaryLines(lines);
                        }
                    });
        }
    }

    private void appendCoSummaryLines(String[] lines) {
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append("• ").append(line);
        }
        coOrganizerSummary.setText(out.toString());
    }

    private void addCoOrganizerFromSearch() {
        if (selectedUserId == null || selectedUserId.trim().isEmpty()) {
            Toast.makeText(this, "Select a user in the search results first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (primaryOrganizerId != null && selectedUserId.equals(primaryOrganizerId)) {
            Toast.makeText(this, R.string.organizer_co_cannot_add_owner, Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.organizer_my_events_sign_in, Toast.LENGTH_SHORT).show();
            return;
        }
        final String inviteeId = selectedUserId;
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> existing = EventOrganizerAccess.readCoOrganizerIds(doc);
                    if (existing.contains(inviteeId)) {
                        Toast.makeText(this, R.string.organizer_co_already, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<String> pending = EventOrganizerAccess.readPendingCoOrganizerIds(doc);
                    if (pending.contains(inviteeId)) {
                        Toast.makeText(this, R.string.organizer_co_already_invited, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String eventName = doc.getString("name");
                    String title = eventName != null && !eventName.trim().isEmpty()
                            ? eventName.trim()
                            : getString(R.string.notif_event_fallback_name);
                    db.collection("events")
                            .document(eventId)
                            .update("pendingCoOrganizerIds", FieldValue.arrayUnion(inviteeId))
                            .addOnSuccessListener(aVoid -> {
                                String body = getString(R.string.co_invite_notification_body);
                                UserNotification notification = new UserNotification(
                                        eventId, title, body, NotificationTypes.CO_ORGANIZER_INVITE);
                                notification.setInviterId(currentUser.getUid());
                                notificationStore.sendNotificationToUser(inviteeId, notification,
                                        new NotificationStore.SimpleCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Toast.makeText(OrganizerDashboardActivity.this,
                                                        R.string.organizer_co_invite_sent, Toast.LENGTH_SHORT).show();
                                                loadDashboardSummary();
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                lotteryErrorToast(e);
                                            }
                                        });
                            })
                            .addOnFailureListener(this::lotteryErrorToast);
                })
                .addOnFailureListener(this::lotteryErrorToast);
    }

    private void showRemoveCoOrganizerDialog() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> coIds = new ArrayList<>(EventOrganizerAccess.readCoOrganizerIds(doc));
                    List<String> pendingRaw = EventOrganizerAccess.readPendingCoOrganizerIds(doc);
                    List<String> pendingOnly = new ArrayList<>();
                    for (String p : pendingRaw) {
                        if (!coIds.contains(p)) {
                            pendingOnly.add(p);
                        }
                    }
                    if (coIds.isEmpty() && pendingOnly.isEmpty()) {
                        Toast.makeText(this, R.string.organizer_co_none, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<String> rowUids = new ArrayList<>();
                    List<Boolean> rowPending = new ArrayList<>();
                    for (String uid : coIds) {
                        rowUids.add(uid);
                        rowPending.add(false);
                    }
                    for (String uid : pendingOnly) {
                        rowUids.add(uid);
                        rowPending.add(true);
                    }
                    final List<String> labels = new ArrayList<>();
                    for (String ignored : rowUids) {
                        labels.add("");
                    }
                    final int[] done = {0};
                    final int n = rowUids.size();
                    for (int i = 0; i < n; i++) {
                        final int idx = i;
                        String uid = rowUids.get(i);
                        boolean pending = rowPending.get(i);
                        db.collection("users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(ud -> {
                                    String name = ud.getString("name");
                                    String base = name != null && !name.trim().isEmpty() ? name.trim() : uid;
                                    labels.set(idx, pending
                                            ? getString(R.string.organizer_co_pending_label, base)
                                            : base);
                                    done[0]++;
                                    if (done[0] == n) {
                                        showCoRemoveChooser(rowUids, labels, rowPending);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    labels.set(idx, pending
                                            ? getString(R.string.organizer_co_pending_label, uid)
                                            : uid);
                                    done[0]++;
                                    if (done[0] == n) {
                                        showCoRemoveChooser(rowUids, labels, rowPending);
                                    }
                                });
                    }
                })
                .addOnFailureListener(this::lotteryErrorToast);
    }

    private void showCoRemoveChooser(List<String> ids, List<String> labels, List<Boolean> pendingOnlyRow) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.organizer_co_remove_pick)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    String removeId = ids.get(which);
                    String label = labels.get(which);
                    boolean pendingInvite = pendingOnlyRow.get(which);
                    int msg = pendingInvite
                            ? R.string.organizer_co_remove_pending_confirm
                            : R.string.organizer_co_remove_confirm;
                    new AlertDialog.Builder(this)
                            .setMessage(getString(msg, label))
                            .setPositiveButton(R.string.organizer_waitlist_remove, (d2, w2) -> {
                                String field = pendingInvite ? "pendingCoOrganizerIds" : "coOrganizerIds";
                                db.collection("events")
                                        .document(eventId)
                                        .update(field, FieldValue.arrayRemove(removeId))
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, R.string.organizer_co_removed, Toast.LENGTH_SHORT).show();
                                            loadDashboardSummary();
                                        })
                                        .addOnFailureListener(this::lotteryErrorToast);
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                })
                .show();
    }

}