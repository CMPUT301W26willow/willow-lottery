package com.example.willow_lotto_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Create event form; uploads poster to Storage and saves event to Firestore. */
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

        //Getting all the user inputted items into Vars to input into event HashMap
        String name = nameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String registrationStart = registrationStartInput.getText().toString().trim();
        String registrationEnd = registrationEndInput.getText().toString().trim();
        String eventDate = eventDateInput.getText().toString().trim();

        //Toast Msgs to ensure all fields are filled
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

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (posterUri != null) {
            createEventThenUploadPoster(name, description, registrationStart, registrationEnd, eventDate, locationRequiredSwitch.isChecked(), uid);
        } else {
            createEventInFirestore(name, description, registrationStart, registrationEnd, eventDate, locationRequiredSwitch.isChecked(), uid, null);
        }
    }

    private static final String TAG = "CreateEventActivity";
    private static final long DOWNLOAD_URL_DELAY_MS = 800;

    // Create event first to get eventId, then upload poster and update event with download URL.
    private void createEventThenUploadPoster(String name, String description, String registrationStart, String registrationEnd, String eventDate, boolean locationRequired, String uid) {
        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("description", description);
        event.put("date", eventDate);
        event.put("registrationStart", registrationStart);
        event.put("registrationEnd", registrationEnd);
        event.put("locationRequired", locationRequired);
        event.put("organizerId", uid);

        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();
                    uploadPosterForEvent(eventId, name, description, registrationStart, registrationEnd, eventDate, locationRequired, uid);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                });
    }

    // Upload to event_posters/{eventId}.jpg; delay before getDownloadUrl to avoid "object does not exist".
    private void uploadPosterForEvent(String eventId, String name, String description, String registrationStart, String registrationEnd, String eventDate, boolean locationRequired, String uid) {
        String path = "event_posters/" + eventId + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);

        String mimeType = getContentResolver().getType(posterUri);
        if (mimeType == null || !mimeType.startsWith("image/")) {
            mimeType = "image/jpeg";
        }
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(mimeType)
                .build();

        InputStream stream;
        try {
            stream = getContentResolver().openInputStream(posterUri);
        } catch (Exception e) {
            Log.e(TAG, "Could not open poster stream", e);
            Toast.makeText(this, "Could not read image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            submitButton.setEnabled(true);
            return;
        }
        if (stream == null) {
            Toast.makeText(this, "Could not read image", Toast.LENGTH_SHORT).show();
            submitButton.setEnabled(true);
            return;
        }

        InputStream streamToClose = stream;
        ref.putStream(stream, metadata)
                .addOnSuccessListener(taskSnapshot -> {
                    closeQuietly(streamToClose);
                    StorageReference uploadedRef = taskSnapshot.getStorage();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        uploadedRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    db.collection("events").document(eventId)
                                            .update("posterUri", uri.toString())
                                            .addOnSuccessListener(aVoid -> {
                                                Intent intent = new Intent(this, EventQrActivity.class);
                                                intent.putExtra(EventQrActivity.EXTRA_EVENT_ID, eventId);
                                                intent.putExtra(EventQrActivity.EXTRA_EVENT_NAME, name);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Event created but poster link failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                submitButton.setEnabled(true);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to get download URL", e);
                                    Toast.makeText(this, "Upload done but URL failed. Enable Storage read in Firebase Console. " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    submitButton.setEnabled(true);
                                });
                    }, DOWNLOAD_URL_DELAY_MS);
                })
                .addOnFailureListener(e -> {
                    closeQuietly(streamToClose);
                    Log.e(TAG, "Poster upload failed", e);
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("object does not exist")) {
                        Toast.makeText(this, "Storage not set up. In Firebase Console: enable Storage and set rules to allow read/write.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Upload failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                    submitButton.setEnabled(true);
                });
    }

    /** Closes stream; ignores exceptions. */
    private static void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ignored) {
            }
        }
    }

    // Write event to Firestore (no poster or with posterUrl).
    private void createEventInFirestore(String name, String description, String registrationStart, String registrationEnd, String eventDate, boolean locationRequired, String uid, String posterUrl) {
        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("description", description);
        event.put("date", eventDate);
        event.put("registrationStart", registrationStart);
        event.put("registrationEnd", registrationEnd);
        event.put("locationRequired", locationRequired);
        if (posterUrl != null) {
            event.put("posterUri", posterUrl);
        }
        event.put("organizerId", uid);

        //Creating the event in Firebase with unique id from Hash
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

        //Creating Sub Collections for event
        db.collection("events")
                .document(event.toString())
                .collection("WaitList");
        db.collection("events")
                .document(event.toString())
                .collection("ChosenList");
        db.collection("events").document(event.toString())
                .collection("EnrolledList");

    }
}
