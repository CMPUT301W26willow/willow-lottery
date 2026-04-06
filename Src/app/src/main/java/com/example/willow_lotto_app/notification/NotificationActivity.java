package com.example.willow_lotto_app.notification;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.home.MainActivity;
import com.example.willow_lotto_app.profile.ProfileActivity;
import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.events.EventDetailActivity;
import com.example.willow_lotto_app.organizer.EventOrganizerAccess;
import com.example.willow_lotto_app.events.EventsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationActivity displays the current signed-in user's in-app notifications.
 *<p>
 * Role in application:
 * - Acts as the controller/view layer for the notifications screen.
 * - Reads notification documents from Firestore under:
 *   users/{uid}/notifications
 * - Displays them in a RecyclerView using NotificationAdapter.
 *<p>
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
        adapter.setOnNotificationClickListener(this::onNotificationClicked);

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
                        item.setInviterId(getSafeString(doc, "inviterId"));

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

    // CHANGED: co-organizer invites still open the accept/decline dialog,
    // while regular event notifications now open the related event detail page.
    private void onNotificationClicked(UserNotificationItem item) {
        if (item == null) {
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        // CHANGED: special dialog flow for co-organizer invites.
        if (NotificationTypes.CO_ORGANIZER_INVITE.equals(item.getType())) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.co_invite_dialog_title)
                    .setMessage(R.string.co_invite_dialog_message)
                    .setPositiveButton(R.string.co_invite_accept, (d, w) ->
                            respondToCoOrganizerInvite(item, user.getUid(), true))
                    .setNegativeButton(R.string.co_invite_decline, (d, w) ->
                            respondToCoOrganizerInvite(item, user.getUid(), false))
                    .show();
            return;
        }

        // CHANGED: open the event tied to the notification for normal event-related notifications.
        if (!TextUtils.isEmpty(item.getEventId())) {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, item.getEventId());
            startActivity(intent);
            return;
        }

        Toast.makeText(this, "No event linked to this notification.", Toast.LENGTH_SHORT).show();
    }

    private void respondToCoOrganizerInvite(UserNotificationItem item, String uid, boolean accept) {
        String eventId = item.getEventId();
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, R.string.co_invite_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, R.string.co_invite_no_longer_valid, Toast.LENGTH_SHORT).show();
                        deleteNotificationDoc(uid, item.getId());
                        loadNotifications();
                        return;
                    }
                    List<String> pending = EventOrganizerAccess.readPendingCoOrganizerIds(doc);
                    if (!pending.contains(uid)) {
                        Toast.makeText(this, R.string.co_invite_no_longer_valid, Toast.LENGTH_SHORT).show();
                        deleteNotificationDoc(uid, item.getId());
                        loadNotifications();
                        return;
                    }
                    if (accept) {
                        db.collection("events")
                                .document(eventId)
                                .update(
                                        "pendingCoOrganizerIds", FieldValue.arrayRemove(uid),
                                        "coOrganizerIds", FieldValue.arrayUnion(uid))
                                .addOnSuccessListener(aVoid -> finishCoInviteResponse(uid, item.getId(),
                                        R.string.co_invite_accepted))
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, R.string.co_invite_failed, Toast.LENGTH_SHORT).show());
                    } else {
                        db.collection("events")
                                .document(eventId)
                                .update("pendingCoOrganizerIds", FieldValue.arrayRemove(uid))
                                .addOnSuccessListener(aVoid -> finishCoInviteResponse(uid, item.getId(),
                                        R.string.co_invite_declined))
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, R.string.co_invite_failed, Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.co_invite_failed, Toast.LENGTH_SHORT).show());
    }

    private void finishCoInviteResponse(String uid, String notificationDocId, int messageRes) {
        deleteNotificationDoc(uid, notificationDocId);
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
        loadNotifications();
    }

    private void deleteNotificationDoc(String uid, String notificationDocId) {
        if (TextUtils.isEmpty(notificationDocId)) {
            return;
        }
        db.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationDocId)
                .delete();
    }
}