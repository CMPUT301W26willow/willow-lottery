package com.example.willow_lotto_app;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Event QR Activity:
 * This activity is used to display the QR code for an event.
 * 
 * Features:
 * - Display the QR code for an event
 * - Download the QR code as a PNG file
 * - Display a success message if the event is created
 * - Display an instruction message if the event is not created
 * - Display a download button if the event is created
 * - Display a done button if the event is not created
 * 
 * Flow:
 *  1. Event is created in Firestore
 *  2. The creating activity launches this activity with the event ID and name
 *  3. The Intent passes:
 *      - EXTRA_EVENT_ID  (required)
 *      - EXTRA_EVENT_NAME (optional)
 *  4. EventQrActivity generates a QR code bitmap using QRCodeHelper.
 *  5. The QR code is displayed to the user.
 *  6. The user may download the QR image to their device.
 * 
 */
public class EventQrActivity extends AppCompatActivity {

    /** Intent extra: Firestore event document ID. */
    public static final String EXTRA_EVENT_ID = "event_id";
    /** Intent extra: event name (for success message and download filename). */
    public static final String EXTRA_EVENT_NAME = "event_name";
    // QR code size in pixels
    private static final int QR_SIZE_PX = 512;
    // reference to the QR code image view
    private ImageView qrImage;
    // reference to the QR code bitmap
    private Bitmap qrBitmap;
    // reference to the event name
    private String eventName;

    // Called when the activity is created.
    // Initializes the UI layout, connects all UI elements to the code,
    // and sets up the event ID and name.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // call the super class onCreate method
        super.onCreate(savedInstanceState);
        // set the content view to the event_qr layout
        setContentView(R.layout.activity_event_qr);

        // get the event ID and name from the intent
        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventName = getIntent().getStringExtra(EXTRA_EVENT_NAME);
        // get the toolbar and set it to the support action bar
        MaterialToolbar toolbar = findViewById(R.id.event_qr_toolbar);
        // set the toolbar to the support action bar
        setSupportActionBar(toolbar);
        // if the support action bar is not null, set the display home as up enabled
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        // get the QR code image view and set it to the qrImage variable
        qrImage = findViewById(R.id.event_qr_image);
        // get the instruction text view and set it to the instructionView variable
        TextView instructionView = findViewById(R.id.event_qr_instruction);
        // get the success message text view and set it to the successMessageView variable
        TextView successMessageView = findViewById(R.id.event_qr_success_message);
        // get the download button and set it to the downloadButton variable
        Button downloadButton = findViewById(R.id.event_qr_download);
        Button doneButton = findViewById(R.id.event_qr_done);

        instructionView.setText(R.string.event_qr_instruction);

        if (eventName != null) {
            successMessageView.setText(getString(R.string.event_created_success, eventName));
            successMessageView.setVisibility(android.view.View.VISIBLE);
        } else {
            successMessageView.setVisibility(android.view.View.GONE);
        }

        if (eventId != null) {
            // *generate the QR code bitmap using the QRCodeHelper class
            qrBitmap = QRCodeHelper.generateEventQrBitmap(eventId, QR_SIZE_PX);
            if (qrBitmap != null) {
                qrImage.setImageBitmap(qrBitmap);
            } else {
                // if the QR code bitmap is null, show a toast message and return
                Toast.makeText(this, "Could not generate QR code", Toast.LENGTH_SHORT).show();
            }
        }
        // set the download button to the downloadQrCode method
        downloadButton.setOnClickListener(v -> downloadQrCode(eventId));
        // set the done button to the finish method
        doneButton.setOnClickListener(v -> finish());
    }

    /**
     * Saves the current QR bitmap: on API 29+ to MediaStore (Gallery), otherwise to Downloads.
     * Filename: event_qr_{eventName}.png (unsafe chars replaced with '_').
     */
    private void downloadQrCode(String eventId) {
        // if the QR code bitmap is null, show a toast message and return
        if (qrBitmap == null) {
            Toast.makeText(this, "No QR code to download", Toast.LENGTH_SHORT).show();
            return;
        }
        // set the file name to the event name
        String fileName = "event_qr_" + (eventName != null ? eventName.replaceAll("[^a-zA-Z0-9]", "_") : "event") + ".png";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri imageUri = getContentResolver().insert(collection, values);
            if (imageUri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
                    if (out != null && qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        Toast.makeText(this, "QR code saved to Gallery", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (IOException e) {
                }
            }
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Toast.makeText(this, "QR code saved to Downloads", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Could not save QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
