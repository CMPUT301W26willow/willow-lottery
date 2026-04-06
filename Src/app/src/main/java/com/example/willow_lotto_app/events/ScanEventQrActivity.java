package com.example.willow_lotto_app.events;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.willow_lotto_app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opens {@link EventDetailActivity} when the camera reads a QR code encoding
 * {@code willow-lottery://event/{eventId}} (same payload as {@link EventQrActivity}).
 */
@ExperimentalGetImage
public class ScanEventQrActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private final AtomicBoolean navigated = new AtomicBoolean(false);
    private BarcodeScanner barcodeScanner;
    private long lastInvalidQrToastElapsedMs;

    private final ActivityResultLauncher<String> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.scan_event_qr_permission_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_event_qr);

        MaterialToolbar toolbar = findViewById(R.id.scan_qr_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        previewView = findViewById(R.id.scan_qr_preview);
        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (Exception e) {
                Toast.makeText(this, R.string.scan_event_qr_camera_failed, Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis);
        } catch (Exception e) {
            Toast.makeText(this, R.string.scan_event_qr_camera_failed, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (navigated.get()) {
            imageProxy.close();
            return;
        }
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        InputImage image = InputImage.fromMediaImage(mediaImage, rotation);

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> handleBarcodes(barcodes))
                .addOnFailureListener(e -> { /* ignore single-frame errors */ })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleBarcodes(List<Barcode> barcodes) {
        if (navigated.get() || barcodes == null || barcodes.isEmpty()) {
            return;
        }
        for (Barcode barcode : barcodes) {
            String raw = barcode.getRawValue();
            String eventId = EventDetailIntentHelper.parseEventIdFromScannedPayload(raw);
            if (eventId != null) {
                if (navigated.compareAndSet(false, true)) {
                    runOnUiThread(() -> openEventAndFinish(eventId));
                }
                return;
            }
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastInvalidQrToastElapsedMs > 2500L) {
            lastInvalidQrToastElapsedMs = now;
            runOnUiThread(() ->
                    Toast.makeText(this, R.string.scan_event_qr_invalid, Toast.LENGTH_SHORT).show());
        }
    }

    private void openEventAndFinish(String eventId) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
        finish();
    }
}
