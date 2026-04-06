package com.example.willow_lotto_app.events;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizer list of entrants who accepted an invitation ({@link RegistrationStatus#ACCEPTED}),
 * i.e. final enrolled for lottery-selected spots.
 */
public class AcceptedEntrantsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    private ListView acceptedListView;
    private TextView acceptedEmpty;
    private String eventId;
    private RegistrationStore registrationRepository;
    private FirebaseFirestore db;

    private final List<String> entrantNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accepted_entrants);

        db = FirebaseFirestore.getInstance();
        registrationRepository = new RegistrationStore();

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        acceptedListView = findViewById(R.id.acceptedListView);
        acceptedEmpty = findViewById(R.id.accepted_empty);

        Button backButton = findViewById(R.id.accepted_back_button);
        backButton.setOnClickListener(v -> finish());

        loadAcceptedEntrants();
    }

    private void loadAcceptedEntrants() {
        registrationRepository.getRegistrationsForEventByStatus(
                eventId,
                RegistrationStatus.ACCEPTED.getValue(),
                new RegistrationStore.RegistrationListCallback() {
                    @Override
                    public void onSuccess(List<Registration> result) {
                        if (result.isEmpty()) {
                            acceptedEmpty.setVisibility(TextView.VISIBLE);
                            acceptedListView.setVisibility(ListView.GONE);
                            return;
                        }

                        List<String> userIds = new ArrayList<>();
                        for (Registration reg : result) {
                            userIds.add(reg.getUserId());
                        }
                        loadUserNamesOrdered(userIds);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("AcceptedEntrants", "Failed to load enrolled entrants", e);
                        Toast.makeText(AcceptedEntrantsActivity.this,
                                R.string.organizer_enrolled_load_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserNamesOrdered(List<String> userIds) {
        entrantNames.clear();
        if (userIds.isEmpty()) {
            return;
        }
        for (int i = 0; i < userIds.size(); i++) {
            entrantNames.add(userIds.get(i));
        }

        final int[] completed = {0};
        final int n = userIds.size();
        for (int i = 0; i < n; i++) {
            final int index = i;
            String userId = userIds.get(i);
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
                        if (completed[0] == n) {
                            bindList();
                        }
                    })
                    .addOnFailureListener(e -> {
                        completed[0]++;
                        if (completed[0] == n) {
                            bindList();
                        }
                    });
        }
    }

    private void bindList() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, entrantNames);
        acceptedListView.setAdapter(adapter);
        acceptedEmpty.setVisibility(TextView.GONE);
        acceptedListView.setVisibility(ListView.VISIBLE);
    }
}
