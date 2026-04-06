package com.example.willow_lotto_app.entrant;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/** Map showing where waitlisted entrants joined for one event. */
public class EntrantMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "EntrantMapActivity";
    private static final String REGISTRATIONS_COLLECTION = "registrations";

    public static final String EXTRA_EVENT_ID = "event_id";
    private static final String FALLBACK_EVENT_ID = "event1";

    private String eventId;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_map);

        String fromIntent = getIntent() != null ? getIntent().getStringExtra(EXTRA_EVENT_ID) : null;
        eventId = (fromIntent != null && !fromIntent.trim().isEmpty())
                ? fromIntent.trim()
                : FALLBACK_EVENT_ID;

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Called when the Google Map is ready to use.
     * Loads entrant locations from Firestore and places markers on the map.
     *
     * @param googleMap The GoogleMap instance that is ready.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        loadEntrantLocations();
    }

    /**
     * Fetches entrant documents from the Firestore waitingList subcollection.
     * For each entrant with valid latitude and longitude fields, adds a marker
     * to the map at their location.
     */
    private void loadEntrantLocations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<LatLng> points = new ArrayList<>();
        final int[] pending = {2};

        Runnable finishBatch = () -> {
            if (--pending[0] > 0) {
                return;
            }
            runOnUiThread(() -> fitCamera(points));
        };

        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", RegistrationStatus.WAITLISTED.getValue())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            addMarkerFromRegistration(doc, points);
                        }
                    } else if (task.getException() != null) {
                        Log.e(TAG, "Error loading registration locations", task.getException());
                    }
                    finishBatch.run();
                });

        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            addMarkerFromLegacyWaitingList(doc, points);
                        }
                    } else if (task.getException() != null) {
                        Log.e(TAG, "Error loading legacy waitingList", task.getException());
                    }
                    finishBatch.run();
                });
    }

    private void addMarkerFromRegistration(DocumentSnapshot doc, List<LatLng> points) {
        Double latitude = doc.getDouble("latitude");
        Double longitude = doc.getDouble("longitude");
        if (latitude == null || longitude == null) {
            return;
        }
        String title = doc.getString("name");
        if (title == null || title.trim().isEmpty()) {
            title = doc.getString("userId");
        }
        if (title == null || title.trim().isEmpty()) {
            title = getString(R.string.notif_event_fallback_name);
        }
        LatLng location = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(location).title(title.trim()));
        points.add(location);
    }

    private void addMarkerFromLegacyWaitingList(QueryDocumentSnapshot doc, List<LatLng> points) {
        String name = doc.getString("name");
        Double latitude = doc.getDouble("latitude");
        Double longitude = doc.getDouble("longitude");
        if (latitude == null || longitude == null || name == null || name.trim().isEmpty()) {
            return;
        }
        LatLng location = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(location).title(name.trim()));
        points.add(location);
    }

    private void fitCamera(List<LatLng> points) {
        if (mMap == null || points.isEmpty()) {
            return;
        }
        if (points.size() == 1) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 10));
            return;
        }
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (LatLng p : points) {
            bounds.include(p);
        }
        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
        } catch (IllegalStateException ex) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 10));
        }
    }
}
