package com.example.willow_lotto_app.events;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.willow_lotto_app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * CreateEventActivity.java
 *
 * Author: Mehr Dhanda
 *
 * Organizer "Create Event" form.
 *
 * Responsibilities:
 * - Collects core event fields and validates required ones.
 * - Allows organizer to optionally set a waiting list limit (02.03.01).
 * - Uploads an optional poster image to Firebase Storage.
 * - On success, navigates to {@link EventQrActivity}.
 *
 * Outstanding issues:
 * - waitlistLimit is optional; if not set, no limit is enforced on the entrant side yet.
 */
public class CreateEventActivity extends AppCompatActivity {

    private TextView waitlistLimitDisplay;
    private Integer waitlistLimit = null;
    private EditText nameInput;
    private EditText descriptionInput;
    private EditText registrationStartInput;
    private EditText registrationEndInput;
    private EditText eventDateInput;
    private SwitchCompat locationRequiredSwitch;
    private ImageView posterPreview;
    private View posterPlaceholder;
    private View posterArea;
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
        posterArea = findViewById(R.id.create_event_poster_area);
        submitButton = findViewById(R.id.create_event_submit);
        waitlistLimitDisplay = findViewById(R.id.waitlist_limit_display);
        db = FirebaseFirestore.getInstance();

        setupDatePickerField(registrationStartInput);
        setupDatePickerField(registrationEndInput);
        setupDatePickerField(eventDateInput);

        View chooseFileButton = findViewById(R.id.create_event_choose_file);
        View.OnClickListener openPicker = v -> openImagePicker();
        chooseFileButton.setOnClickListener(openPicker);
        posterArea.setOnClickListener(openPicker);
        posterPlaceholder.setOnClickListener(openPicker);

        Button waitlistLimitButton = findViewById(R.id.waitlist_limit_button);
        waitlistLimitButton.setOnClickListener(v -> showNumberPickerDialog());

        submitButton.setOnClickListener(v -> createEvent());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImage.launch(Intent.createChooser(intent, "Select poster image"));
    }

    /** Attach a DatePickerDialog to the given date field. */
    private void setupDatePickerField(EditText field) {
        field.setInputType(InputType.TYPE_NULL);
        field.setOnClickListener(v -> showDatePicker(field));
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker(field);
        });
    }

    private void showDatePicker(EditText target) {
        final Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (DatePicker view, int y, int m, int d) -> {
                    String formatted = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                    target.setText(formatted);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    /**
     * Shows a NumberPicker dialog for the organizer to optionally set
     * the maximum number of entrants allowed on the waiting list.
     * Selecting 0 means no limit.
     */
    private void showNumberPickerDialog() {
        NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(500);
        numberPicker.setValue(waitlistLimit != null ? waitlistLimit : 0);

        new AlertDialog.Builder(this)
                .setTitle("Set Waiting List Limit")
                .setMessage("Set to 0 for no limit")
                .setView(numberPicker)
                .setPositiveButton("OK", (dialog, which) -> {
                    int val = numberPicker.getValue();
                    if (val == 0) {
                        waitlistLimit = null;
                        waitlistLimitDisplay.setText("No limit");
                    } else {
                        waitlistLimit = val;
                        waitlistLimitDisplay.setText("Max " + val + " entrants");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Validates required event fields.
     *
     * @param name        Event name entered by organizer.
     * @param description Event description entered by organizer.
     * @param eventDate   Event date entered by organizer.
     * @return Error message string, or null if valid.
     */
    public static String validateEventForm(String name, String description, String eventDate) {
        if (name == null || name.trim().isEmpty()) return "Event name is required";
        if (description == null || description.trim().isEmpty()) return "Description is required";
        if (eventDate == null || eventDate.trim().isEmpty()) return "Event date is required";
        return null;
    }

    private void createEvent() {
        String name = nameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String registrationStart = registrationStartInput.getText().toString().trim();
        String registrationEnd = registrationEndInput.getText().toString().trim();
        String eventDate = eventDateInput.getText().toString().trim();

        String error = validateEventForm(name, description, eventDate);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }

        submitButton.setEnabled(false);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (posterUri != null) {
            createEventThenUploadPoster(name, description, registrationStart, registrationEnd, eventDate, locationRequiredSwitch.isChecked(), uid, waitlistLimit);
        } else {
            createEventInFirestore(name, description, registrationStart, registrationEnd, eventDate, locationRequiredSwitch.isChecked(), uid, null, waitlistLimit);
        }
    }

    private static final String TAG = "CreateEventActivity";
    private static final long DOWNLOAD_URL_DELAY_MS = 800;

    /**
     * Creates event in Firestore first to get eventId, then uploads poster.
     *
     * @param waitlistLimit Optional max number of entrants on waiting list.
     */
    private void createEventThenUploadPoster(String name, String description, String registrationStart, String registrationEnd, String eventDate, boolean locationRequired, String uid, Integer waitlistLimit) {
        Map<String, Object> event = buildEventMap(name, description, registrationStart, registrationEnd, eventDate, locationRequired, uid, null, waitlistLimit);

        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();
                    uploadPosterForEvent(eventId, name, description, registrationStart, registrationEnd, eventDate, locationRequired, uid, waitlistLimit);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                });
    }

    private void uploadPosterForEvent(String eventId, String name, String description, String registrationStart, String registrationEnd, String eventDate, boolean locationRequired, String uid, Integer waitlistLimit) {
        String path = "event_posters/" + eventId + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);

        String mimeType = getContentResolver().getType(posterUri);
        if (mimeType == null || !mimeType.startsWith("image/")) mimeType = "image/jpeg";

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
                                    Toast.makeText(this, "Upload done but URL failed. " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    submitButton.setEnabled(true);
                                });
                    }, DOWNLOAD_URL_DELAY_MS);
                })
                .addOnFailureListener(e -> {
                    closeQuietly(streamToClose);
                    Log.e(TAG, "Poster upload failed", e);
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("object does not exist")) {
                        Toast.makeText(this, "Storage not set up. Enable Storage in Firebase Console.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Upload failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                    submitButton.setEnabled(true);
                });
    }

    private static void closeQuietly(InputStream stream) {
        if (stream != null) {
            try { stream.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Writes the event document to Firestore with all fields including optional waitlist limit.
     *
     * @param waitlistLimit Optional max number of entrants. Null means unlimited.
     */
    private void createEventInFirestore(String name, String description, String registrationStart, String registrationEnd, String eventDate, boolean locationRequired, String uid, String posterUrl, Integer waitlistLimit) {
        Map<String, Object> event = buildEventMap(name, description, registrationStart, registrationEnd, eventDate, locationRequired, uid, posterUrl, waitlistLimit);

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

        db.collection("events").document(event.toString()).collection("WaitList");
        db.collection("events").document(event.toString()).collection("ChosenList");
        db.collection("events").document(event.toString()).collection("EnrolledList");
    }

    /**
     * Builds the event data map for Firestore from the given fields.
     * Includes waitlistLimit only if it is non-null.
     *
     * @param waitlistLimit Optional max waiting list size. Null means unlimited.
     * @return Map of event fields ready for Firestore.
     */
    private Map<String, Object> buildEventMap(String name, String description, String registrationStart, String registrationEnd, String eventDate, boolean locationRequired, String uid, String posterUrl, Integer waitlistLimit) {
        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("description", description);
        event.put("date", eventDate);
        event.put("registrationStart", registrationStart);
        event.put("registrationEnd", registrationEnd);
        event.put("locationRequired", locationRequired);
        event.put("organizerId", uid);
        event.put("drawSize", 0);
        event.put("registeredUsers", new java.util.ArrayList<String>());
        if (posterUrl != null) event.put("posterUri", posterUrl);
        if (waitlistLimit != null) event.put("waitlistLimit", waitlistLimit);
        return event;
    }
}