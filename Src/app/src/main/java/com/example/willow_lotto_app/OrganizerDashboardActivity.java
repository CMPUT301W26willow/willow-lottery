/**
 * OrganizerDashboardActivity.java
 *
 *
 * Displays the organizer dashboard screen, including the waiting list of entrants,
 * lottery controls, geolocation toggle, and map view button.
 *
 * Role: Controller in the MVC pattern.
 *
 * Outstanding issues:
 * - Event ID is passed via Intent but defaults to null if not provided.
 */
package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that shows the organizer dashboard including waiting list,
 * lottery controls, geolocation toggle, and map view.
 */
public class OrganizerDashboardActivity extends AppCompatActivity {

    private ListView waitingListView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> entrantNames;

    public static final String EXTRA_EVENT_ID = "event_id";
    private EditText drawSizeInput;
    private Button createEventButton;
    private Button runLotteryButton;
    private Button drawReplacementButton;
    private Button backToProfileButton;
    private Switch geolocationSwitch;

    private String eventId;
    private OrganizerLotteryManager lotteryManager;
    private RegistrationStore registrationRepository;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);
        db = FirebaseFirestore.getInstance();
        lotteryManager = new OrganizerLotteryManager();
        registrationRepository = new RegistrationStore();

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        waitingListView = findViewById(R.id.waitingListView);
        entrantNames = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, entrantNames);
        waitingListView.setAdapter(adapter);

        drawSizeInput = findViewById(R.id.drawSizeInput);
        createEventButton = findViewById(R.id.createEventButton);
        runLotteryButton = findViewById(R.id.runLotteryButton);
        drawReplacementButton = findViewById(R.id.drawReplacementButton);
        backToProfileButton = findViewById(R.id.backToProfileButton);
        geolocationSwitch = findViewById(R.id.geolocationSwitch);

        createEventButton.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        backToProfileButton.setOnClickListener(v -> finish());

        runLotteryButton.setOnClickListener(v -> runLottery());
        drawReplacementButton.setOnClickListener(v -> drawReplacement());

        Button viewMapButton = findViewById(R.id.viewMapButton);
        viewMapButton.setOnClickListener(v -> startActivity(new Intent(this, EntrantMapActivity.class)));

        geolocationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveGeolocationSetting(isChecked));

        loadEventDrawSize();
        loadWaitingList();
        loadGeolocationSetting();
    }

    /**
     * Loads the waiting list from Firebase using the registration repository.
     */
    private void loadWaitingList() {
        registrationRepository.getRegistrationsForEventByStatus(
                eventId,
                RegistrationStatus.WAITLISTED.getValue(),
                new RegistrationStore.RegistrationListCallback() {
                    @Override
                    public void onSuccess(List<Registration> registrations) {
                        if (registrations.isEmpty()) {
                            entrantNames.clear();
                            adapter.notifyDataSetChanged();
                            Toast.makeText(OrganizerDashboardActivity.this, "No waitlisted entrants.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<String> userIds = new ArrayList<>();
                        for (Registration registration : registrations) {
                            userIds.add(registration.getUserId());
                        }

                        loadUserNames(userIds);
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
     * Loads user names from Firestore for a list of user IDs.
     *
     * @param userIds List of user IDs to fetch names for.
     */
    private void loadUserNames(List<String> userIds) {
        entrantNames.clear();
        if (userIds.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        final int[] completed = {0};
        for (String userId : userIds) {
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");

                        if (name != null && !name.trim().isEmpty()) {
                            entrantNames.add(name);
                        } else if (email != null && !email.trim().isEmpty()) {
                            entrantNames.add(email);
                        } else {
                            entrantNames.add(userId);
                        }

                        completed[0]++;
                        if (completed[0] == userIds.size()) {
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        entrantNames.add(userId);
                        completed[0]++;
                        if (completed[0] == userIds.size()) {
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
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
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(OrganizerDashboardActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(OrganizerDashboardActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Draws a replacement entrant for the event lottery.
     */
    private void drawReplacement() {
        lotteryManager.drawReplacement(eventId, new OrganizerLotteryManager.LotteryCallback() {
            @Override
            public void onSuccess(String message, List<Registration> affectedRegistrations) {
                Toast.makeText(OrganizerDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                loadWaitingList();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(OrganizerDashboardActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}