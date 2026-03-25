package com.example.willow_lotto_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationActivity displays the current signed-in user's in-app notifications.
 *
 * Role in application:
 * - Acts as the controller/view layer for the notifications screen.
 * - Reads notification documents from Firestore under:
 *   users/{uid}/notifications
 * - Displays them in a RecyclerView using NotificationAdapter.
 *
 * Current limitations / outstanding issues:
 * - Notifications are currently read-only.
 * - Notifications are not yet marked as read when opened.
 * - Tapping a notification does not yet open the related event.
 * - Event names are not currently resolved here unless added separately.
 */

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView notificationsRecycler;
    private TextView notificationsEmpty;
    private NotificationAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationsRecycler = findViewById(R.id.notifications_recycler);
        notificationsEmpty = findViewById(R.id.notifications_empty);

        adapter = new NotificationAdapter();
        notificationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadNotifications();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_notifications);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }

            if (item.getItemId() == R.id.nav_events) {
                startActivity(new Intent(this, EventsActivity.class));
                finish();
                return true;
            }

            if (item.getItemId() == R.id.nav_notifications) {
                return true;
            }

            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    private void loadNotifications() {
        if (mAuth.getCurrentUser() == null) {
            notificationsEmpty.setVisibility(View.VISIBLE);
            notificationsEmpty.setText("You must be signed in to view notifications.");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserNotificationItem> items = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        UserNotificationItem item = new UserNotificationItem();
                        item.setId(doc.getId());
                        item.setEventId(getSafeString(doc, "eventId"));
                        item.setTitle(getSafeString(doc, "title"));
                        item.setMessage(getSafeString(doc, "message"));
                        item.setType(getSafeString(doc, "type"));

                        Boolean read = doc.getBoolean("read");
                        item.setRead(read != null && read);

                        item.setCreatedAt(doc.getTimestamp("createdAt"));
                        items.add(item);
                    }

                    adapter.setNotifications(items);
                    notificationsEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    notificationsEmpty.setVisibility(View.VISIBLE);
                    notificationsEmpty.setText("Could not load notifications.");
                    Toast.makeText(this, "Failed to load notifications.", Toast.LENGTH_SHORT).show();
                });
    }

    private String getSafeString(QueryDocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : value.toString();
    }
}