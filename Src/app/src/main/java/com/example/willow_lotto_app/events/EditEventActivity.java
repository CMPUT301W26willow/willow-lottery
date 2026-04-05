/**
 * EditEventActivity.java
 *
 * Author: Mehr Dhanda
 *
 * Allows an organizer to update the event poster for an existing event.
 * The image is compressed and stored in Firestore as a data URI in {@code posterUri}.
 */
package com.example.willow_lotto_app.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.events.poster.EventPlaceholderDrawables;
import com.example.willow_lotto_app.events.poster.EventPosterLoader;
import com.example.willow_lotto_app.events.poster.PosterFirestoreCodec;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Organizer screen to replace an event poster. Expects {@link #EXTRA_EVENT_ID} in the intent.
 */
public class EditEventActivity extends AppCompatActivity {

    private static final String TAG = "EditEventActivity";

    public static final String EXTRA_EVENT_ID = "event_id";

    private ImageView eventPosterImageView;
    private TextView uploadStatusText;
    private Uri selectedImageUri;
    private String eventId;
    private final ExecutorService posterEncodeExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    eventPosterImageView.setImageURI(selectedImageUri);
                    uploadPoster();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        eventPosterImageView = findViewById(R.id.eventPosterImageView);
        eventPosterImageView.setImageResource(EventPlaceholderDrawables.forEventId(eventId));
        uploadStatusText = findViewById(R.id.uploadStatusText);

        Button uploadPosterButton = findViewById(R.id.uploadPosterButton);
        Button backButton = findViewById(R.id.backButton);

        uploadPosterButton.setOnClickListener(v -> openImagePicker());
        backButton.setOnClickListener(v -> finish());

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> EventPosterLoader.load(
                        this,
                        doc.getString("posterUri"),
                        eventPosterImageView,
                        eventId));
    }

    @Override
    protected void onDestroy() {
        posterEncodeExecutor.shutdownNow();
        super.onDestroy();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadPoster() {
        if (selectedImageUri == null) {
            return;
        }

        uploadStatusText.setText("Compressing and saving…");

        final Uri uri = selectedImageUri;
        posterEncodeExecutor.execute(() -> {
            try {
                String dataUri = PosterFirestoreCodec.encodePosterAsDataUri(getContentResolver(), uri);
                runOnUiThread(() -> savePosterUriToFirestore(dataUri));
            } catch (IOException e) {
                Log.e(TAG, "Poster encoding failed", e);
                runOnUiThread(() -> {
                    uploadStatusText.setText("Could not process image.");
                    Toast.makeText(this,
                            e.getMessage() != null ? e.getMessage() : "Could not process image",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void savePosterUriToFirestore(String dataUri) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .update("posterUri", dataUri)
                .addOnSuccessListener(aVoid -> {
                    uploadStatusText.setText("Poster saved.");
                    Toast.makeText(this, "Poster updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save poster", e);
                    uploadStatusText.setText("Failed to save poster.");
                });
    }
}
