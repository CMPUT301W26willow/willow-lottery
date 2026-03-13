package com.example.willow_lotto_app;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Detailed view for a single event.
 *
 * Responsibilities:
 * - Displays event metadata, poster, and registration period (02.01.04).
 * - Implements 01.01.01 / 01.01.02 "Join/Leave Events" by letting the user
 *   join or leave the waiting list for this event.
 * - Supports deep links from QR codes (willow-lottery://event/{id}).
 */
public class EventDetailActivity extends AppCompatActivity {

    private static final String REGISTRATIONS_COLLECTION = "registrations";
    public static final String EXTRA_EVENT_ID = "event_id";

    private FirebaseFirestore db;
    private String eventId;
    private String currentUserId;
    private Event event;
    private boolean joined;
    private int waitingListCount;

    private TextView nameView;
    private TextView dateView;
    private TextView descriptionView;
    private TextView organizerView;
    private TextView registrationDatesView;
    private TextView registrationOpensView;
    private TextView waitingListView;
    private TextView limitView;
    private TextView lotteryBulletsView;
    private ImageView posterView;
    private View posterPlaceholder;
    private Button joinLeaveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Support both in-app navigation (EXTRA_EVENT_ID) and deep links via QR code
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            // Try to parse from custom URI: willow-lottery://event/{eventId}
            Uri data = getIntent().getData();
            if (data != null
                    && "willow-lottery".equals(data.getScheme())
                    && "event".equals(data.getHost())
                    && data.getPathSegments() != null
                    && !data.getPathSegments().isEmpty()) {
                eventId = data.getPathSegments().get(0);
            }
        }
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        MaterialToolbar toolbar = findViewById(R.id.event_detail_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        nameView = findViewById(R.id.event_detail_name);
        dateView = findViewById(R.id.event_detail_date);
        descriptionView = findViewById(R.id.event_detail_description);
        organizerView = findViewById(R.id.event_detail_organizer);        
        registrationDatesView = findViewById(R.id.event_detail_registration_dates);
        registrationOpensView = findViewById(R.id.event_detail_registration_opens);
        waitingListView = findViewById(R.id.event_detail_waiting_list);
        limitView = findViewById(R.id.event_detail_limit);
        lotteryBulletsView = findViewById(R.id.event_detail_lottery_bullets);
        posterView = findViewById(R.id.event_detail_poster);
        posterPlaceholder = findViewById(R.id.event_detail_poster_placeholder);
        joinLeaveBtn = findViewById(R.id.event_detail_join_leave_btn);

        loadEvent();
    }

    // Load event doc then waiting-list count and join state.
    private void loadEvent() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(this::applyEventDoc)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void applyEventDoc(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        event = new Event();
        event.setId(doc.getId());
        event.setName(getString(doc, "name"));
        event.setDescription(getString(doc, "description"));
        event.setDate(getString(doc, "date"));
        event.setOrganizerId(getString(doc, "organizerId"));
        event.setRegistrationStart(getString(doc, "registrationStart"));
        event.setRegistrationEnd(getString(doc, "registrationEnd"));
        event.setPosterUri(getString(doc, "posterUri"));
        event.setLimit(getInt(doc, "limit"));
        event.setDrawSize(getInt(doc, "drawSize"));

        nameView.setText(event.getName() != null ? event.getName() : "");
        descriptionView.setText(event.getDescription() != null ? event.getDescription() : "");

        String organizerLabel =
                event.getOrganizerId() != null && !event.getOrganizerId().isEmpty()
                        ? event.getOrganizerId()
                        : "Organizer";
        organizerView.setText(
                getString(R.string.event_detail_organized_by, organizerLabel));

        dateView.setText(getString(R.string.event_detail_event_date,
                event.getDate() != null ? event.getDate() : ""));

        String start = event.getRegistrationStart();
        String end = event.getRegistrationEnd();
        if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
            registrationDatesView.setText(getString(R.string.event_detail_registration_format, start, end));
            registrationOpensView.setVisibility(View.VISIBLE);
            registrationOpensView.setText(getString(R.string.event_detail_registration_opens, start));
        } else {
            registrationDatesView.setText(R.string.event_detail_no_registration_dates);
            registrationOpensView.setVisibility(View.GONE);
        }

        String posterUrl = event.getPosterUri();
        if (posterUrl != null && !posterUrl.trim().isEmpty()) {
            posterPlaceholder.setVisibility(View.GONE);
            posterView.setVisibility(View.VISIBLE);
            Uri uri = Uri.parse(posterUrl.trim());
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.poster_placeholder)
                    .error(R.drawable.poster_placeholder);
            Glide.with(this)
                    .load(uri)
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(posterView);
        } else {
            posterView.setVisibility(View.GONE);
            posterPlaceholder.setVisibility(View.VISIBLE);
        }

        loadWaitingListCount();
    }

    // Query registrations for this event to show count and limit.
    private void loadWaitingListCount() {
        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    waitingListCount = snap != null ? snap.size() : 0;
                    waitingListView.setText(getString(R.string.event_detail_waiting_list_count, waitingListCount));
                    Integer limit = event.getLimit();
                    if (limit != null && limit > 0) {
                        limitView.setVisibility(View.VISIBLE);
                        limitView.setText(getString(R.string.event_detail_limit_spots, limit));
                    } else {
                        limitView.setVisibility(View.GONE);
                    }
                    buildLotteryCriteria();
                    checkJoinedAndUpdateButton();
                })
                .addOnFailureListener(e -> {
                    waitingListView.setText(getString(R.string.event_detail_waiting_list_count, 0));
                    limitView.setVisibility(View.GONE);
                    buildLotteryCriteria();
                    checkJoinedAndUpdateButton();
                });
    }

    private void buildLotteryCriteria() {
        Integer drawSize = event.getDrawSize();
        StringBuilder sb = new StringBuilder();
        if (drawSize != null && drawSize > 0) {
            sb.append("• ").append(getString(R.string.event_detail_draw_size, drawSize)).append("\n");
        }
        sb.append("• ").append(getString(R.string.event_detail_random_selection)).append("\n");
        sb.append("• ").append(getString(R.string.event_detail_one_entry));
        lotteryBulletsView.setText(sb.toString());
    }

    private void checkJoinedAndUpdateButton() {
        if (currentUserId == null) {
            joinLeaveBtn.setEnabled(false);
            joinLeaveBtn.setText(R.string.event_join_waiting_list);
            return;
        }
        db.collection(REGISTRATIONS_COLLECTION)
                .document(eventId + "_" + currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    joined = doc != null && doc.exists();
                    updateJoinLeaveButton();
                })
                .addOnFailureListener(e -> {
                    joined = false;
                    updateJoinLeaveButton();
                });
    }

    private void updateJoinLeaveButton() {
        joinLeaveBtn.setEnabled(true);
        joinLeaveBtn.setText(joined ? R.string.event_joined_waiting_list : R.string.event_join_waiting_list);
        joinLeaveBtn.setOnClickListener(v -> {
            if (joined) {
                leaveEvent();
            } else {
                joinEvent();
            }
        });
    }

    private void joinEvent() {
        if (currentUserId == null) return;
        String docId = eventId + "_" + currentUserId;
        Map<String, Object> reg = new HashMap<>();
        reg.put("eventId", eventId);
        reg.put("userId", currentUserId);
        reg.put("status", RegistrationStatus.WAITLISTED.getValue());
        db.collection(REGISTRATIONS_COLLECTION).document(docId).set(reg)
                .addOnSuccessListener(aVoid -> {
                    joined = true;
                    waitingListCount++;
                    waitingListView.setText(getString(R.string.event_detail_waiting_list_count, waitingListCount));
                    updateJoinLeaveButton();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Could not join event", Toast.LENGTH_SHORT).show());
    }

    private void leaveEvent() {
        if (currentUserId == null) return;
        String docId = eventId + "_" + currentUserId;
        db.collection(REGISTRATIONS_COLLECTION).document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    joined = false;
                    if (waitingListCount > 0) waitingListCount--;
                    waitingListView.setText(getString(R.string.event_detail_waiting_list_count, waitingListCount));
                    updateJoinLeaveButton();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Could not leave event", Toast.LENGTH_SHORT).show());
    }

    /** Returns doc field as string, or "". */
    private static String getString(DocumentSnapshot doc, String field) {
        Object o = doc.get(field);
        return o != null ? o.toString() : "";
    }

    /** Returns doc field as Integer, or null. */
    private static Integer getInt(DocumentSnapshot doc, String field) {
        Object o = doc.get(field);
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
