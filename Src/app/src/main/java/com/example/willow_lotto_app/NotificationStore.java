package com.example.willow_lotto_app;

import com.google.firebase.firestore.FirebaseFirestore;


    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    private final FirebaseFirestore db;

        this.db = FirebaseFirestore.getInstance();
    }

    public void sendNotificationToUser(String userId, UserNotification notification, final SimpleCallback callback) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(notification.toMap())
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}