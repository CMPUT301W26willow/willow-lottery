package com.example.willow_lotto_app.events;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.willow_lotto_app.EntrantResponseManager;
import com.example.willow_lotto_app.OrganizerLotteryManager;
import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detailed view for a single event.
 *
 * Responsibilities:
 * - Displays event metadata, poster, and registration period (02.01.04).
 * - Implements 01.01.01 / 01.01.02 "Join/Leave Events" by letting the user
 *   join or leave the waiting list for this event.
 * - Implements 01.05.01 - 01.05.05
 * - Supports deep links from QR codes (willow-lottery://event/{id}).
 * @author Jasdeep Cheema and Dev Tiwari
 * @version 2.0
 * @since 30/03/2026
 */
public class EventDetailActivity extends AppCompatActivity {

    private static final String REGISTRATIONS_COLLECTION = "registrations";
    /** Subcollection under each {@code events/{eventId}} document. */
    private static final String COMMENTS_SUBCOLLECTION = "comments";
    private static final int COMMENTS_PAGE_LIMIT = 50;
    public static final String EXTRA_EVENT_ID = "event_id";

    private FirebaseFirestore db;

    // Added for the user story flow so this screen can work with the same
    // registration logic used in the rest of the project.
    private RegistrationStore registrationStore;
    private EntrantResponseManager responseManager;
    private OrganizerLotteryManager lotteryManager;

    private String eventId;
    private String currentUserId;

    // Added so the screen can track the exact registration and status
    // instead of only checking whether a document exists.
    private String registrationId;
    private String currentStatus;

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

    // Added for US 01.05.02 and US 01.05.03.
    // These buttons are only shown when the user has INVITED status.
    private Button acceptButton;
    private Button declineButton;

    private RecyclerView commentsRecyclerView;
    private TextView commentsEmptyView;
    private EditText commentInput;
    private Button postCommentButton;
    private EventCommentsAdapter commentsAdapter;
    private ListenerRegistration commentsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Support both in-app navigation (EXTRA_EVENT_ID) and deep links via QR code
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
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

        // Added to support the user-story logic directly inside EventDetailActivity.
        registrationStore = new RegistrationStore();
        responseManager = new EntrantResponseManager();
        lotteryManager = new OrganizerLotteryManager();

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

        // Added buttons for invitation response flow.
        acceptButton = findViewById(R.id.event_detail_accept_btn);
        declineButton = findViewById(R.id.event_detail_decline_btn);

        commentsRecyclerView = findViewById(R.id.event_detail_comments_list);
        commentsEmptyView = findViewById(R.id.event_detail_comments_empty);
        commentInput = findViewById(R.id.event_detail_comment_input);
        postCommentButton = findViewById(R.id.event_detail_comment_post_btn);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new EventCommentsAdapter();
        commentsRecyclerView.setAdapter(commentsAdapter);
        postCommentButton.setOnClickListener(v -> postComment());

        // Added listeners for the invited-user flow.
        acceptButton.setOnClickListener(v -> acceptInvitation());
        declineButton.setOnClickListener(v -> declineInvitation());

        updateCommentComposerState();
        loadEvent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachCommentsListener();
    }

    @Override
    protected void onStop() {
        detachCommentsListener();
        super.onStop();
    }

    private void attachCommentsListener() {
        if (eventId == null || eventId.isEmpty()) {
            return;
        }
        detachCommentsListener();
        commentsListener = db.collection("events")
                .document(eventId)
                .collection(COMMENTS_SUBCOLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(COMMENTS_PAGE_LIMIT)
                .addSnapshotListener((snap, error) -> {
                    if (error != null || snap == null) {
                        return;
                    }
                    List<EventComment> list = new ArrayList<>(snap.size());
                    for (QueryDocumentSnapshot doc : snap) {
                        list.add(EventComment.fromSnapshot(doc));
                    }
                    commentsAdapter.setComments(list);
                    boolean empty = list.isEmpty();
                    commentsEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                });
    }

    private void detachCommentsListener() {
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
    }

    private void updateCommentComposerState() {
        boolean signedIn = currentUserId != null;
        commentInput.setEnabled(signedIn);
        postCommentButton.setEnabled(signedIn);
        if (!signedIn) {
            commentInput.setHint(getString(R.string.event_detail_comment_sign_in));
        } else {
            commentInput.setHint(getString(R.string.event_detail_comment_hint));
        }
    }

    private void postComment() {
        if (currentUserId == null) {
            Toast.makeText(this, R.string.event_detail_comment_sign_in, Toast.LENGTH_SHORT).show();
            return;
        }
        String body = commentInput.getText() != null ? commentInput.getText().toString().trim() : "";
        if (body.isEmpty()) {
            return;
        }

        postCommentButton.setEnabled(false);
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(userDoc -> {
                    String authorName = userDoc != null ? userDoc.getString("name") : null;
                    if (authorName == null || authorName.trim().isEmpty()) {
                        authorName = "User";
                    }
                    Map<String, Object> comment = new HashMap<>();
                    comment.put("authorId", currentUserId);
                    comment.put("authorName", authorName.trim());
                    comment.put("body", body);
                    comment.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("events")
                            .document(eventId)
                            .collection(COMMENTS_SUBCOLLECTION)
                            .add(comment)
                            .addOnSuccessListener(ref -> {
                                commentInput.setText("");
                                Toast.makeText(this, R.string.event_detail_comment_posted, Toast.LENGTH_SHORT).show();
                                postCommentButton.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, R.string.event_detail_comment_failed, Toast.LENGTH_SHORT).show();
                                postCommentButton.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.event_detail_comment_failed, Toast.LENGTH_SHORT).show();
                    postCommentButton.setEnabled(true);
                });
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

        // Changed so the screen supports both the newer "waitlistLimit" field
        // and the older "limit" field already used elsewhere in the project.
        Integer waitlistLimit = getInt(doc, "waitlistLimit");
        if (waitlistLimit == null) {
            waitlistLimit = getInt(doc, "limit");
        }
        event.setLimit(waitlistLimit);

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

    // Changed for US 01.05.04.
    // The old version counted every registration for the event.
    // This version counts only WAITLISTED registrations, which is the actual
    // waiting-list number the story asks for.
    private void loadWaitingListCount() {
        db.collection(REGISTRATIONS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", RegistrationStatus.WAITLISTED.getValue())
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

    // Still builds the criteria text, but this also supports US 01.05.05
    // because the entrant can now see the lottery rules on the detail screen.
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

    // Changed so the screen reads not only whether the registration exists,
    // but also what the user's current status is.
    // That status is needed for invited / accepted / declined / cancelled states.
    private void checkJoinedAndUpdateButton() {
        if (currentUserId == null) {
            joined = false;
            registrationId = null;
            currentStatus = null;
            updateJoinLeaveButton();
            return;
        }

        db.collection(REGISTRATIONS_COLLECTION)
                .document(eventId + "_" + currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    joined = doc != null && doc.exists();

                    if (joined && doc != null) {
                        registrationId = doc.getId();
                        currentStatus = doc.getString("status");
                    } else {
                        registrationId = null;
                        currentStatus = null;
                    }

                    updateJoinLeaveButton();
                })
                .addOnFailureListener(e -> {
                    joined = false;
                    registrationId = null;
                    currentStatus = null;
                    updateJoinLeaveButton();
                });
    }

    // Changed so button behavior now depends on registration status,
    // not only on whether the registration document exists.
    private void updateJoinLeaveButton() {
        joinLeaveBtn.setVisibility(View.VISIBLE);
        joinLeaveBtn.setEnabled(true);
        acceptButton.setVisibility(View.GONE);
        declineButton.setVisibility(View.GONE);

        if (currentUserId == null) {
            joinLeaveBtn.setEnabled(false);
            joinLeaveBtn.setText(R.string.event_join_waiting_list);
            return;
        }

        if (!joined) {
            joinLeaveBtn.setText(R.string.event_join_waiting_list);
            joinLeaveBtn.setOnClickListener(v -> joinEvent());
            return;
        }

        // Added for US 01.05.02 and US 01.05.03.
        // Invited users should not see the normal join/leave button.
        // They should instead get Accept and Decline.
        if (RegistrationStatus.INVITED.getValue().equals(currentStatus)) {
            joinLeaveBtn.setVisibility(View.GONE);
            acceptButton.setVisibility(View.VISIBLE);
            declineButton.setVisibility(View.VISIBLE);
            return;
        }

        // Added so accepted users no longer look like normal waitlisted users.
        if (RegistrationStatus.ACCEPTED.getValue().equals(currentStatus)) {
            joinLeaveBtn.setText("Invitation Accepted");
            joinLeaveBtn.setEnabled(false);
            return;
        }

        // Added so declined users no longer look like normal waitlisted users.
        if (RegistrationStatus.DECLINED.getValue().equals(currentStatus)) {
            joinLeaveBtn.setText("Invitation Declined");
            joinLeaveBtn.setEnabled(false);
            return;
        }

        // Added for cancelled state handling.
        if (RegistrationStatus.CANCELLED.getValue().equals(currentStatus)) {
            joinLeaveBtn.setText("Registration Cancelled");
            joinLeaveBtn.setEnabled(false);
            return;
        }

        // Existing joined / waitlisted state still ends here.
        joinLeaveBtn.setText(R.string.event_joined_waiting_list);
        joinLeaveBtn.setOnClickListener(v -> leaveEvent());
    }

    // Same basic join logic as before, but now explicitly writes WAITLISTED status
    // so the rest of the project can use consistent registration-state logic.
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
                    registrationId = docId;
                    currentStatus = RegistrationStatus.WAITLISTED.getValue();
                    waitingListCount++;
                    waitingListView.setText(getString(R.string.event_detail_waiting_list_count, waitingListCount));
                    updateJoinLeaveButton();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Could not join event", Toast.LENGTH_SHORT).show());
    }

    // Same basic leave logic as before, with local UI state reset added
    // so the new status-based button logic updates immediately.
    private void leaveEvent() {
        if (currentUserId == null) return;
        String docId = eventId + "_" + currentUserId;
        db.collection(REGISTRATIONS_COLLECTION).document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    joined = false;
                    registrationId = null;
                    currentStatus = null;
                    if (waitingListCount > 0) waitingListCount--;
                    waitingListView.setText(getString(R.string.event_detail_waiting_list_count, waitingListCount));
                    updateJoinLeaveButton();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Could not leave event", Toast.LENGTH_SHORT).show());
    }

    // Added for US 01.05.02.
    // Invited users can now accept the invitation directly from this screen.
    private void acceptInvitation() {
        if (registrationId == null) {
            Toast.makeText(this, "Registration not found", Toast.LENGTH_SHORT).show();
            return;
        }

        responseManager.acceptInvitation(
                registrationId,
                eventId,
                currentUserId,
                new EntrantResponseManager.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        currentStatus = RegistrationStatus.ACCEPTED.getValue();
                        updateJoinLeaveButton();
                        Toast.makeText(EventDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(EventDetailActivity.this, "Could not accept invitation", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // Added for US 01.05.03.
    // Also completes US 01.05.01 because declining triggers replacement selection.
    private void declineInvitation() {
        if (registrationId == null) {
            Toast.makeText(this, "Registration not found", Toast.LENGTH_SHORT).show();
            return;
        }

        lotteryManager.replaceDeclinedOrCancelledEntrant(
                eventId,
                registrationId,
                RegistrationStatus.DECLINED,
                new OrganizerLotteryManager.LotteryCallback() {
                    @Override
                    public void onSuccess(String message, List<Registration> affectedRegistrations) {
                        currentStatus = RegistrationStatus.DECLINED.getValue();
                        updateJoinLeaveButton();
                        loadWaitingListCount();
                        Toast.makeText(EventDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(EventDetailActivity.this, "Could not decline invitation", Toast.LENGTH_SHORT).show();
                    }
                }
        );
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