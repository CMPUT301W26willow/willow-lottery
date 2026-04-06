package com.example.willow_lotto_app.events;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.MainActivity;
import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.notification.NotificationActivity;
import com.example.willow_lotto_app.ProfileActivity;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;
import com.example.willow_lotto_app.registration.RegistrationStore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Events tab: registration history for the signed-in entrant (US 01.02.03).
 * Browse and join flows live on {@link MainActivity}.
 */
public class EventsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TextView emptyView;
    private RegistrationHistoryAdapter adapter;
    private RegistrationStore registrationStore;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        recycler = findViewById(R.id.events_recycler);
        emptyView = findViewById(R.id.events_empty);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_events);

        adapter = new RegistrationHistoryAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        adapter.setListener(item -> {
            if (item.getEventId() == null || item.getEventId().isEmpty()) {
                return;
            }
            Intent intent = new Intent(EventsActivity.this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, item.getEventId());
            startActivity(intent);
        });

        registrationStore = new RegistrationStore();
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            if (item.getItemId() == R.id.nav_events) {
                return true;
            }
            if (item.getItemId() == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                finish();
                return true;
            }
            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        if (currentUserId == null) {
            adapter.setItems(Collections.emptyList());
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(R.string.reg_history_empty_guest);
            recycler.setVisibility(View.GONE);
            return;
        }

        registrationStore.getRegistrationsForUser(currentUserId, new RegistrationStore.RegistrationListCallback() {
            @Override
            public void onSuccess(List<Registration> registrations) {
                if (registrations == null || registrations.isEmpty()) {
                    runOnUiThread(() -> showEmpty(getString(R.string.reg_history_empty)));
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
                    return Long.compare(tb.getSeconds(), ta.getSeconds());
                });

                int n = registrations.size();
                RegistrationHistoryItem[] slots = new RegistrationHistoryItem[n];
                AtomicInteger done = new AtomicInteger(0);

                for (int i = 0; i < n; i++) {
                    final int index = i;
                    Registration reg = registrations.get(i);
                    String eventId = reg.getEventId();
                    if (eventId == null || eventId.isEmpty()) {
                        slots[index] = buildItem(reg, null);
                        if (done.incrementAndGet() == n) {
                            runOnUiThread(() -> finishHistoryLoad(slots));
                        }
                        continue;
                    }
                    db.collection("events").document(eventId).get()
                            .addOnCompleteListener(task -> {
                                DocumentSnapshot ev = task.isSuccessful() ? task.getResult() : null;
                                slots[index] = buildItem(reg, ev);
                                if (done.incrementAndGet() == n) {
                                    runOnUiThread(() -> finishHistoryLoad(slots));
                                }
                            });
                }
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(EventsActivity.this, R.string.reg_history_load_failed, Toast.LENGTH_SHORT).show();
                    showEmpty(getString(R.string.reg_history_empty));
                });
            }
        });
    }

    private void finishHistoryLoad(RegistrationHistoryItem[] slots) {
        List<RegistrationHistoryItem> list = new ArrayList<>(slots.length);
        Collections.addAll(list, slots);
        adapter.setItems(list);
        if (list.isEmpty()) {
            showEmpty(getString(R.string.reg_history_empty));
        } else {
            emptyView.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(String message) {
        adapter.setItems(Collections.emptyList());
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.GONE);
    }

    private RegistrationHistoryItem buildItem(Registration reg, DocumentSnapshot eventDoc) {
        String eventId = reg.getEventId() != null ? reg.getEventId() : "";
        String title;
        if (eventDoc != null && eventDoc.exists()) {
            String name = eventDoc.getString("name");
            title = (name != null && !name.isEmpty()) ? name : eventId;
        } else {
            title = eventId.isEmpty() ? getString(R.string.reg_history_unknown_event) : eventId;
        }

        String eventDateRaw = eventDoc != null && eventDoc.exists() ? eventDoc.getString("date") : null;
        String eventDatePretty = (eventDateRaw != null && !eventDateRaw.trim().isEmpty())
                ? eventDateRaw.trim()
                : getString(R.string.reg_history_unknown_date);
        String eventDateLine = getString(R.string.reg_history_event_date_fmt, eventDatePretty);

        String registeredPretty = formatTimestamp(reg.getCreatedAt());
        String registeredLine = getString(R.string.reg_history_registered_fmt, registeredPretty);

        RegistrationStatus status = reg.getStatusEnum();
        int badgeBg;
        int badgeColor;
        String badgeLabel;
        RegistrationHistoryItem.Outcome outcome;
        int outcomeRes;

        switch (status) {
            case INVITED:
                badgeBg = R.drawable.bg_reg_history_badge_enrolled;
                badgeColor = R.color.reg_history_badge_enrolled_text;
                badgeLabel = getString(R.string.reg_history_badge_invited);
                outcome = RegistrationHistoryItem.Outcome.POSITIVE;
                outcomeRes = R.string.reg_history_outcome_selected;
                break;
            case ACCEPTED:
                badgeBg = R.drawable.bg_reg_history_badge_enrolled;
                badgeColor = R.color.reg_history_badge_enrolled_text;
                badgeLabel = getString(R.string.reg_history_badge_enrolled);
                outcome = RegistrationHistoryItem.Outcome.POSITIVE;
                outcomeRes = R.string.reg_history_outcome_selected;
                break;
            case DECLINED:
            case CANCELLED:
                badgeBg = R.drawable.bg_reg_history_badge_not_selected;
                badgeColor = R.color.reg_history_badge_not_selected_text;
                badgeLabel = getString(R.string.reg_history_badge_not_selected);
                outcome = RegistrationHistoryItem.Outcome.NEGATIVE;
                outcomeRes = R.string.reg_history_outcome_not_selected;
                break;
            case WAITLISTED:
            case PRIVATE_INVITED:
            default:
                badgeBg = R.drawable.bg_reg_history_badge_waitlist;
                badgeColor = R.color.reg_history_badge_waitlist_text;
                badgeLabel = getString(R.string.reg_history_badge_waitlist);
                outcome = RegistrationHistoryItem.Outcome.NONE;
                outcomeRes = R.string.reg_history_outcome_not_selected;
                break;
        }

        return new RegistrationHistoryItem(
                reg.getId(),
                eventId,
                title,
                eventDateLine,
                registeredLine,
                badgeBg,
                badgeColor,
                badgeLabel,
                outcome,
                outcomeRes);
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) {
            return getString(R.string.reg_history_unknown_date);
        }
        Date d = ts.toDate();
        return new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(d);
    }
}
