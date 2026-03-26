/**
 * EditEventActivity.java
 *
 * Author: Mehr Dhanda
 *
 * Allows an organizer to update the event poster for an existing event.
 * The organizer can select an image from their device, which is uploaded
 * to Firebase Storage. The download URL is then saved to Firestore.
 *
 * Role: Controller in the MVC pattern.
 *
 * Outstanding issues:
 * - Event ID is currently hardcoded as "event1". Should be passed dynamically via Intent.
 * - No image compression before upload.
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * CreateEventActivity.java
 *
 * Allows organizers to create a new event by entering event metadata,
 * selecting registration dates, optionally requiring geolocation, and
 * optionally uploading a poster image.
 *
 * Role in application:
 * - Controller/View layer for organizer event creation.
 * - Writes new event documents to the Firestore "events" collection.
 * - Uploads poster images to Firebase Storage and stores the resulting URL
 *   in the related Firestore event document.
 * - Navigates to EventQrActivity after successful creation so the organizer
 *   can view and share the QR code for the new event.
 *
 * Outstanding issues:
 * - The legacy subcollection creation block at the end of createEventInFirestore()
 *   uses event.toString() instead of the real Firestore document ID and should be
 *   removed or replaced.
 * - Event limits / capacity are not configured here yet.
 * - Validation currently requires event name, description, and event date, but
 *   registration dates remain optional.
 */

public class EditEventActivity extends AppCompatActivity {

    private static final String EVENT_ID = "event1"; // TODO: pass dynamically via Intent

    private ImageView eventPosterImageView;
    private TextView uploadStatusText;
    private Uri selectedImageUri;

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

        eventPosterImageView = findViewById(R.id.eventPosterImageView);
        uploadStatusText = findViewById(R.id.uploadStatusText);

        Button uploadPosterButton = findViewById(R.id.uploadPosterButton);
        Button backButton = findViewById(R.id.backButton);

        uploadPosterButton.setOnClickListener(v -> openImagePicker());
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Opens the device image gallery so the organizer can select a poster image.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Uploads the selected image to Firebase Storage under the event's folder.
     * On success, saves the image download URL to the Firestore event document.
     * Updates the UI with upload status.
     */
    private void uploadPoster() {
        if (selectedImageUri == null) return;

        uploadStatusText.setText("Uploading...");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference()
                .child("events/" + EVENT_ID + "/poster.jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveUrlToFirestore(downloadUri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("Storage", "Upload failed", e);
                    uploadStatusText.setText("Upload failed.");
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Saves the Firebase Storage download URL of the poster to the Firestore event document.
     *
     * @param url The download URL of the uploaded poster image.
     */
    private void saveUrlToFirestore(String url) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(EVENT_ID)
                .update("posterUrl", url)
                .addOnSuccessListener(aVoid -> {
                    uploadStatusText.setText("Poster uploaded successfully!");
                    Toast.makeText(this, "Poster updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to save URL", e);
                    uploadStatusText.setText("Failed to save poster URL.");
                });
    }
}