package com.example.willow_lotto_app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class AdminNotificationLogTest {

    private FirebaseFirestore db;
    private final String testUserId = "test_user_notif_log";
    private final List<String> insertedDocIds = new ArrayList<>();

    @Before
    public void setup() throws InterruptedException {
        db = FirebaseFirestore.getInstance();
        seedTestNotifications();
    }

    @After
    public void teardown() throws InterruptedException {
        // clean up all test documents written during the test
        CountDownLatch latch = new CountDownLatch(insertedDocIds.size());
        for (String docId : insertedDocIds) {
            db.collection("users")
                    .document(testUserId)
                    .collection("notifications")
                    .document(docId)
                    .delete()
                    .addOnCompleteListener(t -> latch.countDown());
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    /**
     * Seeds 3 fake notifications into Firestore under a test user
     * to simulate real organizer-sent notifications
     */
    private void seedTestNotifications() throws InterruptedException {
        List<Map<String, Object>> fakeNotifs = new ArrayList<>();

        // test notification 1 - lottery invite
        Map<String, Object> notif1 = new HashMap<>();
        notif1.put("title", "You have been selected!");
        notif1.put("message", "You were selected for Event A.");
        notif1.put("type", "LOTTERY_INVITED");
        notif1.put("eventId", "event_001");
        notif1.put("inviterId", "organizer_001");
        notif1.put("read", false);
        notif1.put("createdAt", Timestamp.now());
        fakeNotifs.add(notif1);

        // test notification 2 - waitlist joined
        Map<String, Object> notif2 = new HashMap<>();
        notif2.put("title", "Waitlist Confirmed");
        notif2.put("message", "You have joined the waitlist for Event B.");
        notif2.put("type", "WAITLIST_JOINED");
        notif2.put("eventId", "event_002");
        notif2.put("inviterId", "");
        notif2.put("read", false);
        notif2.put("createdAt", Timestamp.now());
        fakeNotifs.add(notif2);

        // test notification 3 - lottery cancelled
        Map<String, Object> notif3 = new HashMap<>();
        notif3.put("title", "Event Cancelled");
        notif3.put("message", "Unfortunately Event C has been cancelled.");
        notif3.put("type", "LOTTERY_CANCELLED");
        notif3.put("eventId", "event_003");
        notif3.put("inviterId", "");
        notif3.put("read", true);
        notif3.put("createdAt", Timestamp.now());
        fakeNotifs.add(notif3);

        // write each notification and track the doc ID for cleanup in teardown
        CountDownLatch latch = new CountDownLatch(fakeNotifs.size());
        for (Map<String, Object> notif : fakeNotifs) {
            db.collection("users")
                    .document(testUserId)
                    .collection("notifications")
                    .add(notif)
                    .addOnSuccessListener(ref -> {
                        insertedDocIds.add(ref.getId());
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AdminNotificationLogTest", "Failed to seed notification", e);
                        latch.countDown();
                    });
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    /**
     * Tests that the collectionGroup query used by showNotificationLogDialog
     * can find notifications and that they contain the expected fields
     */
    @Test
    public void notificationLogQueryReturnsResults() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final List<Map<String, Object>> results = new ArrayList<>();

        // run the same collectionGroup query that showNotificationLogDialog uses
        db.collectionGroup("notifications")
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                        // only collect the test documents we seeded
                        if (insertedDocIds.contains(doc.getId())) {
                            results.add(doc.getData());
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminNotificationLogTest", "Query failed", e);
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);

        // verify the query returned all 3 seeded notifications
        assertTrue("Expected 3 notifications, got " + results.size(), results.size() == 3);

        // verify each notification has the required fields that the dialog reads
        for (Map<String, Object> notif : results) {
            assertNotNull("title should not be null", notif.get("title"));
            assertNotNull("message should not be null", notif.get("message"));
            assertNotNull("type should not be null", notif.get("type"));
        }
    }

    /**
     * Tests that the query returns an empty result when no notifications exist
     * UNIMPLEMENTED - would require isolating the collection to only test data
     */
    @Test
    public void notificationLogQueryHandlesEmpty() {
        // left unimplemented matching the pattern of NotficationSentUnselected
    }
}