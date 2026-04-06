package com.example.willow_lotto_app.events;

import android.content.ContentValues;
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

import com.example.willow_lotto_app.QRCodeHelper;
import com.example.willow_lotto_app.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * EventListActivity.java
 * <p>
 * Displays a simple list of all events stored in Firestore and lets the user open a selected event.
 * <p>
 * Role in application:
 * - Controller/View layer for an older event list flow.
 * - Reads all documents from the Firestore "events" collection.
 * - Launches EventActivity when an event is selected.
 * <p>
 * Outstanding issues:
 * - Still routes to EventActivity instead of the newer EventDetailActivity.
 * - Uses a very simple ListView-based presentation and does not show posters or richer event metadata.
 * - Coexists with EventsActivity, which now provides a more complete event list flow.
 */

public class EventQrActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_NAME = "event_name";

    private static final int QR_SIZE_PX = 512;

    private ImageView qrImage;
    private Bitmap qrBitmap;
    private String eventName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_qr);

        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        eventName = getIntent().getStringExtra(EXTRA_EVENT_NAME);

        MaterialToolbar toolbar = findViewById(R.id.event_qr_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        qrImage = findViewById(R.id.event_qr_image);
        TextView instructionView = findViewById(R.id.event_qr_instruction);
        TextView successMessageView = findViewById(R.id.event_qr_success_message);
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
            qrBitmap = QRCodeHelper.generateEventQrBitmap(eventId, QR_SIZE_PX);
            if (qrBitmap != null) {
                qrImage.setImageBitmap(qrBitmap);
            } else {
                Toast.makeText(this, "Could not generate QR code", Toast.LENGTH_SHORT).show();
            }
        }

        downloadButton.setOnClickListener(v -> downloadQrCode(eventId));
        doneButton.setOnClickListener(v -> finish());
    }

    private void downloadQrCode(String eventId) {
        if (qrBitmap == null) {
            Toast.makeText(this, "No QR code to download", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    // fallback
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
