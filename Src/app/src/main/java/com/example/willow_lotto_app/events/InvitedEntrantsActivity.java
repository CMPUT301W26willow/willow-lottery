/**
 * InvitedEntrantsActivity.java
 *<p>
 * Author: Mehr Dhanda
 *<p>
 * Displays a list of entrants who have been invited (selected by lottery)
 * but have not yet accepted. Allows the organizer to cancel individual entrants.
 *<p>
 * Role: Controller in the MVC pattern.
 *<p>
 * Outstanding issues:
 * - Event ID must be passed via Intent from OrganizerDashboardActivity.
 */
package com.example.willow_lotto_app.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays invited entrants and allows the organizer to cancel them.
 */
public class InvitedEntrantsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    private ListView invitedListView;
    private TextView invitedEmpty;
    private String eventId;
    private RegistrationStore registrationRepository;
    private FirebaseFirestore db;

    private final List<Registration> registrations = new ArrayList<>();
    private final List<String> entrantNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invited_entrants);

        db = FirebaseFirestore.getInstance();
        registrationRepository = new RegistrationStore();

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        invitedListView = findViewById(R.id.invitedListView);
        invitedEmpty = findViewById(R.id.invited_empty);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        loadInvitedEntrants();
    }

    /**
     * Loads the list of invited entrants from Firestore.
     * Fetches all registrations with INVITED status for the current event.
     */
    private void loadInvitedEntrants() {
        registrationRepository.getRegistrationsForEventByStatus(
                eventId,
                RegistrationStatus.INVITED.getValue(),
                new RegistrationStore.RegistrationListCallback() {
                    @Override
                    public void onSuccess(List<Registration> result) {
                        if (result.isEmpty()) {
                            invitedEmpty.setVisibility(View.VISIBLE);
                            invitedListView.setVisibility(View.GONE);
                            return;
                        }

                        registrations.clear();
                        registrations.addAll(result);

                        List<String> userIds = new ArrayList<>();
                        for (Registration reg : result) {
                            userIds.add(reg.getUserId());
                        }
                        loadUserNames(userIds);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("InvitedEntrants", "Failed to load invited entrants", e);
                        Toast.makeText(InvitedEntrantsActivity.this, "Failed to load invited entrants.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Loads usernames from Firestore for a list of user IDs,
     * then sets up the list adapter with cancel buttons.
     *
     * @param userIds List of user IDs to fetch names for.
     */
    private void loadUserNames(List<String> userIds) {
        entrantNames.clear();
        if (userIds.isEmpty()) return;

        final int[] completed = {0};
        for (int i = 0; i < userIds.size(); i++) {
            final int index = i;
            String userId = userIds.get(i);
            entrantNames.add(userId); // placeholder

            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");

                        if (name != null && !name.trim().isEmpty()) {
                            entrantNames.set(index, name);
                        } else if (email != null && !email.trim().isEmpty()) {
                            entrantNames.set(index, email);
                        }

                        completed[0]++;
                        if (completed[0] == userIds.size()) {
                            setupAdapter();
                        }
                    })
                    .addOnFailureListener(e -> {
                        completed[0]++;
                        if (completed[0] == userIds.size()) {
                            setupAdapter();
                        }
                    });
        }
    }

    /**
     * Sets up the ListView adapter with entrant names and cancel buttons.
     * Each row shows the entrant name and a button to cancel their registration.
     */
    private void setupAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.item_invited_entrant, R.id.entrant_name, entrantNames) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_invited_entrant, parent, false);
                }
                TextView nameText = convertView.findViewById(R.id.entrant_name);
                Button cancelBtn = convertView.findViewById(R.id.cancel_button);

                nameText.setText(entrantNames.get(position));

                cancelBtn.setOnClickListener(v -> cancelEntrant(position));

                return convertView;
            }
        };
        invitedListView.setAdapter(adapter);
    }

    /**
     * Cancels the registration of the entrant at the given position.
     * Updates the registration status to CANCELLED in Firestore.
     *
     * @param position The position of the entrant in the list.
     */
    private void cancelEntrant(int position) {
        if (position >= registrations.size()) return;

        Registration registration = registrations.get(position);

        registrationRepository.updateRegistrationStatus(
                registration.getId(),
                RegistrationStatus.CANCELLED.getValue(),
                new RegistrationStore.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(InvitedEntrantsActivity.this, "Entrant cancelled.", Toast.LENGTH_SHORT).show();
                        registrations.remove(position);
                        entrantNames.remove(position);
                        setupAdapter();

                        if (registrations.isEmpty()) {
                            invitedEmpty.setVisibility(View.VISIBLE);
                            invitedListView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("InvitedEntrants", "Failed to cancel entrant", e);
                        Toast.makeText(InvitedEntrantsActivity.this, "Failed to cancel entrant.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}