package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import android.widget.EditText;
import java.util.List;


/**
 * OrganizerDashboardActivity: This activity is used to display the organizer dashboard.
 * 
 * Features:
 * - Display the waiting list
 * - Display the create event button
 * - Display the back button
  */
public class OrganizerDashboardActivity extends AppCompatActivity {
    // reference to the waiting list view
    private ListView waitingListView;
    // reference to the array adapter
    private ArrayAdapter<String> adapter;
    // reference to the array list of entrant names
    private ArrayList<String> entrantNames;

    public static final String EXTRA_EVENT_ID = "event_id";
    private EditText drawSizeInput;
    /*private Button saveDrawSizeButton;*/
    private Button runLotteryButton;
    private Button drawReplacementButton;

    private String eventId;
    private OrganizerLotteryManager lotteryManager;
    private RegistrationStore registrationRepository;
    private FirebaseFirestore db;

    // Called when the activity is created.
    // Initializes the UI layout, connects all UI elements to the code,
    // and sets up the waiting list.
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        /*
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        Button createEventButton = findViewById(R.id.createEventButton);
        Button backButton = findViewById(R.id.backToProfileButton);
        // set the create event button to the create event activity
        createEventButton.setOnClickListener(v -> startActivity(new Intent(this, CreateEventActivity.class)));
        // set the back button to the finish method
        backButton.setOnClickListener(v -> finish());
        // get the waiting list view and set it to the waitingListView variable
        waitingListView = findViewById(R.id.waitingListView);
        entrantNames = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, entrantNames);
        waitingListView.setAdapter(adapter);

        loadWaitingList();
         */
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        lotteryManager = new OrganizerLotteryManager();
        registrationRepository = new RegistrationStore();

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        drawSizeInput = findViewById(R.id.drawSizeInput);
        /*saveDrawSizeButton = findViewById(R.id.saveDrawSizeButton);*/
        runLotteryButton = findViewById(R.id.runLotteryButton);
        drawReplacementButton = findViewById(R.id.drawReplacementButton);

        /*saveDrawSizeButton.setOnClickListener(v -> saveDrawSize());*/
        runLotteryButton.setOnClickListener(v -> runLottery());
        drawReplacementButton.setOnClickListener(v -> drawReplacement());

        loadEventDrawSize();
        loadWaitingList();
    }

    // Load the waiting list from the database
    private void loadWaitingList() {
        /*
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document("event1")
                .collection("waitingList")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    entrantNames.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        if (name != null) {
                            entrantNames.add(name);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (entrantNames.isEmpty()) {
                        Toast.makeText(this, "No entrants yet.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting waiting list", e);
                    Toast.makeText(this, "Failed to load waiting list.", Toast.LENGTH_SHORT).show();
                });*/
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

    private void loadEventDrawSize() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    Long drawSize = documentSnapshot.getLong("drawSize");
                    if (drawSize != null) {
                        drawSizeInput.setText(String.valueOf(drawSize.intValue()));
                    }
                })
                .addOnFailureListener(e -> Log.e("OrganizerDashboard", "Failed to load draw size", e));
    }

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
/*
    private void saveDrawSize() {
        String input = drawSizeInput.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(this, "Enter a draw size.", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(OrganizerDashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(OrganizerDashboardActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

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