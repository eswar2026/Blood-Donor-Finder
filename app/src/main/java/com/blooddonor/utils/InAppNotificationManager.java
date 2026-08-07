package com.blooddonor.utils;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InAppNotificationManager {

    private static final String TAG =
            "InAppNotifManager";
    private static ListenerRegistration listener;

    public interface NotificationCallback {
        void onNewNotification(
                String title, String message);
    }

    // ─────────────────────────────────────────────
    // Start listening to THIS USER'S notifications
    // sub-collection — 100% reliable approach
    // ─────────────────────────────────────────────
    public static void startListening(
            Context context,
            NotificationCallback callback) {

        // Stop any existing listener first
        stopListening();

        FirebaseUser user =
                FirebaseAuth.getInstance()
                        .getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No user logged in");
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore db =
                FirebaseFirestore.getInstance();

        Log.d(TAG, "Starting notification " +
                "listener for uid: " + uid);

        // Listen to the notifications
        // sub-collection of the current user
        // This is written to by whoever posts
        // a blood request matching this user's
        // blood group
        long lastCheckedTime = System.currentTimeMillis();

        listener = db.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt",
                        Query.Direction.DESCENDING)
                .whereGreaterThan("createdAt",
                        lastCheckedTime)
                .addSnapshotListener((snapshots, e) -> {

                    if (e != null) {
                        Log.e(TAG, "Listener error: "
                                + e.getMessage());
                        return;
                    }

                    if (snapshots == null
                            || snapshots.isEmpty()) {
                        return;
                    }

                    Log.d(TAG, "Snapshot received: "
                            + snapshots
                            .getDocumentChanges()
                            .size()
                            + " changes");

                    for (var change :
                            snapshots
                                    .getDocumentChanges()) {

                        // Only new documents
                        if (!Objects.equals(change.getType().name(), "ADDED")) {
                            continue;
                        }

                        var doc = change.getDocument();
                        String finalTitle =
                                Objects.requireNonNullElse(doc.getString("title"), "Blood Donor Finder");
                        String finalMessage =
                                Objects.requireNonNullElse(doc.getString("message"), "");

                        Log.d(TAG,
                                "New notification: "
                                        + finalTitle);

                        // Update badge in UI
                        if (callback != null) {
                            callback.onNewNotification(
                                    finalTitle, finalMessage);
                        }
                    }
                });

        Log.d(TAG, "Listener attached successfully");
    }

    // ── Load unread count from Firestore ─────────────────────
    public static void loadUnreadCount(
            String uid,
            UnreadCountCallback callback) {

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (callback != null) {
                        callback.onCount(snapshots.size());
                    }
                });
    }

    public interface UnreadCountCallback {
        void onCount(int count);
    }

    // ── Stop listener ─────────────────────────────
    public static void stopListening() {
        if (listener != null) {
            listener.remove();
            listener = null;
            Log.d(TAG, "Listener stopped");
        }
    }

    // ── Save notification to Firestore ─────────────
    public static void saveNotification(
            FirebaseFirestore db,
            String uid,
            String title,
            String message,
            String type,
            String relatedId) {

        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "UID is empty — skip save");
            return;
        }

        Map<String, Object> notif = new HashMap<>();
        notif.put("title",     title);
        notif.put("message",   message);
        notif.put("type",      type);
        notif.put("relatedId", relatedId);
        notif.put("read",      false);
        notif.put("createdAt",
                System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .add(notif)
                .addOnSuccessListener(r ->
                        Log.d(TAG,
                                "✅ Notif saved for: " + uid))
                .addOnFailureListener(e ->
                        Log.e(TAG,
                                "❌ Save failed for "
                                        + uid + ": "
                                        + e.getMessage()));
    }
}
