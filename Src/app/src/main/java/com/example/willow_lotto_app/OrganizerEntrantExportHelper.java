package com.example.willow_lotto_app;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the entrant CSV for an event and opens the system share sheet.
 * Shared by {@link OrganizerDashboardActivity} and {@link OrganizerMyEventsActivity}.
 */
public final class OrganizerEntrantExportHelper {

    private static final String TAG = "OrganizerEntrantExport";

    private OrganizerEntrantExportHelper() {
    }

    public static void exportEntrantsCsv(AppCompatActivity activity, String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(activity, R.string.organizer_export_csv_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(activity, R.string.organizer_export_csv_loading, Toast.LENGTH_SHORT).show();
        RegistrationStore registrationRepository = new RegistrationStore();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        registrationRepository.getRegistrationsForEvent(eventId, new RegistrationStore.RegistrationListCallback() {
            @Override
            public void onSuccess(List<Registration> registrations) {
                if (registrations.isEmpty()) {
                    String csv = EntrantListCsvExporter.buildCsv(Collections.emptyList());
                    shareCsvOnUiThread(activity, eventId, csv);
                    return;
                }
                Collections.sort(registrations, (a, b) -> {
                    Timestamp ta = a.getCreatedAt();
                    Timestamp tb = b.getCreatedAt();
                    if (ta == null && tb == null) {
                        return 0;
                    }
                    if (ta == null) {
                        return 1;
                    }
                    if (tb == null) {
                        return -1;
                    }
                    int c = Long.compare(ta.getSeconds(), tb.getSeconds());
                    if (c != 0) {
                        return c;
                    }
                    return Integer.compare(ta.getNanoseconds(), tb.getNanoseconds());
                });
                buildEntrantRowsAndShareCsv(activity, db, eventId, registrations);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "export registrations failed", e);
                Toast.makeText(activity, R.string.organizer_export_csv_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void buildEntrantRowsAndShareCsv(
            AppCompatActivity activity,
            FirebaseFirestore db,
            String eventId,
            List<Registration> registrations) {
        final int n = registrations.size();
        final EntrantListCsvExporter.Row[] rows = new EntrantListCsvExporter.Row[n];
        final AtomicInteger remaining = new AtomicInteger(n);

        Runnable onOneDone = () -> {
            if (remaining.decrementAndGet() != 0) {
                return;
            }
            String csv = EntrantListCsvExporter.buildCsv(Arrays.asList(rows));
            shareCsvOnUiThread(activity, eventId, csv);
        };

        for (int i = 0; i < n; i++) {
            final int index = i;
            Registration reg = registrations.get(i);
            String uid = reg.getUserId();
            String status = reg.getStatus() != null ? reg.getStatus() : "";
            String enrolled = EntrantListCsvExporter.formatEnrolledAt(reg.getCreatedAt());

            if (uid == null || uid.isEmpty()) {
                rows[index] = new EntrantListCsvExporter.Row("", "", "", "", status, enrolled);
                onOneDone.run();
                continue;
            }

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone");
                        rows[index] = new EntrantListCsvExporter.Row(
                                name, email, phone, uid, status, enrolled);
                        onOneDone.run();
                    })
                    .addOnFailureListener(e -> {
                        rows[index] = new EntrantListCsvExporter.Row("", "", "", uid, status, enrolled);
                        onOneDone.run();
                    });
        }
    }

    private static void shareCsvOnUiThread(AppCompatActivity activity, String eventId, String csv) {
        activity.runOnUiThread(() -> {
            try {
                File dir = new File(activity.getCacheDir(), "exports");
                if (!dir.exists() && !dir.mkdirs()) {
                    Toast.makeText(activity, R.string.organizer_export_csv_write_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                String safeId = eventId.replaceAll("[^a-zA-Z0-9_-]", "_");
                File file = new File(dir, "entrants_" + safeId + "_" + System.currentTimeMillis() + ".csv");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                    fos.write(csv.getBytes(StandardCharsets.UTF_8));
                }
                Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/csv");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.organizer_export_csv_subject));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(Intent.createChooser(share, activity.getString(R.string.organizer_export_csv_chooser)));
            } catch (Exception e) {
                Log.e(TAG, "share csv failed", e);
                Toast.makeText(activity, R.string.organizer_export_csv_write_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
