package com.example.willow_lotto_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText descriptionInput;
    private EditText registrationStartInput;
    private EditText registrationEndInput;
    private EditText eventDateInput;
    private SwitchCompat locationRequiredSwitch;
    private ImageView posterPreview;
    private View posterPlaceholder;
    private Button submitButton;
    private FirebaseFirestore db;

    private Uri posterUri;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    posterUri = result.getData().getData();
                    if (posterUri != null) {
                        posterPreview.setImageURI(posterUri);
                        posterPreview.setVisibility(View.VISIBLE);
                        posterPlaceholder.setVisibility(View.GONE);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        MaterialToolbar toolbar = findViewById(R.id.create_event_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        nameInput = findViewById(R.id.create_event_name);
        descriptionInput = findViewById(R.id.create_event_description);
        registrationStartInput = findViewById(R.id.create_event_registration_start);
        registrationEndInput = findViewById(R.id.create_event_registration_end);
        eventDateInput = findViewById(R.id.create_event_date);
        locationRequiredSwitch = findViewById(R.id.create_event_location_required);
        posterPreview = findViewById(R.id.create_event_poster_preview);
        posterPlaceholder = findViewById(R.id.create_event_poster_placeholder);
        submitButton = findViewById(R.id.create_event_submit);
        db = FirebaseFirestore.getInstance();

        findViewById(R.id.create_event_choose_file).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickImage.launch(Intent.createChooser(intent, "Select poster image"));
        });

        submitButton.setOnClickListener(v -> createEvent());
    }

    private void createEvent() {
        String name = nameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String registrationStart = registrationStartInput.getText().toString().trim();
        String registrationEnd = registrationEndInput.getText().toString().trim();
        String eventDate = eventDateInput.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Event name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (eventDate.isEmpty()) {
            Toast.makeText(this, "Event date is required", Toast.LENGTH_SHORT).show();
            return;
        }

        submitButton.setEnabled(false);

        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("description", description);
        event.put("date", eventDate);
        event.put("registrationStart", registrationStart);
        event.put("registrationEnd", registrationEnd);
        event.put("locationRequired", locationRequiredSwitch.isChecked());
        if (posterUri != null) {
            event.put("posterUri", posterUri.toString());
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        event.put("organizerId", uid);

        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();
                    Intent intent = new Intent(this, EventQrActivity.class);
                    intent.putExtra(EventQrActivity.EXTRA_EVENT_ID, eventId);
                    intent.putExtra(EventQrActivity.EXTRA_EVENT_NAME, name);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                });
    }
}
