/**
 * EntrantMapActivity.java
 *
 * Author: Mehr Dhanda
 *
 * Displays a Google Map showing the locations where entrants joined
 * the waiting list for a specific event. Each entrant is represented
 * by a map marker at their recorded location.
 *
 * Role: Controller in the MVC pattern.
 *
 * Outstanding issues:
 * - Event ID is currently hardcoded as "event1". Should be passed dynamically via Intent.
 * - No clustering for overlapping markers.
 */
package com.example.willow_lotto_app;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Activity that displays a map with markers showing where entrants
 * joined the event waiting list from.
 */
public class EntrantMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String EVENT_ID = "event1";
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_map);

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

        db.collection("events")
                .document(EVENT_ID)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        Double latitude = doc.getDouble("latitude");
                        Double longitude = doc.getDouble("longitude");

                        if (latitude != null && longitude != null && name != null) {
                            LatLng location = new LatLng(latitude, longitude);
                            mMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .title(name));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error loading entrant locations", e);
                });
    }
}