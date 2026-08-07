package com.blooddonor.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.blooddonor.BloodDonorApp;
import com.blooddonor.R;
import com.blooddonor.activities.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static ListenerRegistration listenerReg;

    public interface OnNewRequestCallback {
        void onNewRequest(String title, String message);
    }

    // ── Start listening for matching blood requests ────────────
    public static void startListeningForRequests(
            Context context,
            OnNewRequestCallback callback) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No user logged in — skipping listener");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String myUid = user.getUid();

        // Get current user's profile to know their blood group
        db.collection("users").document(myUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String  bloodGroup = doc.getString("bloodGroup");
                    Boolean isDonor    = doc.getBoolean("donor");
                    Boolean available  = doc.getBoolean("available");

                    if (bloodGroup == null) {
                        Log.d(TAG, "No blood group set");
                        return;
                    }
                    if (isDonor == null || !isDonor) {
                        Log.d(TAG, "User is not a donor");
                        return;
                    }
                    if (available == null || !available) {
                        Log.d(TAG, "User is not available");
                        return;
                    }

                    Log.d(TAG, "Starting listener for blood group: "
                            + bloodGroup);

                    // Only listen for requests created AFTER now
                    long startTime = System.currentTimeMillis();

                    listenerReg = db.collection("blood_requests")
                            .whereEqualTo("status",     "OPEN")
                            .whereEqualTo("bloodGroup", bloodGroup)
                            .whereGreaterThan("createdAt", startTime)
                            .addSnapshotListener((snapshots, error) -> {

                                if (error != null) {
                                    Log.e(TAG, "Listener error: "
                                            + error.getMessage());
                                    return;
                                }

                                if (snapshots == null
                                        || snapshots.isEmpty()) return;

                                for (var change :
                                        snapshots.getDocumentChanges()) {

                                    // Only process newly added documents
                                    if (!change.getType().name()
                                            .equals("ADDED")) continue;

                                    var reqDoc = change.getDocument();
                                    String requesterId =
                                            reqDoc.getString("requesterId");

                                    // Skip your own requests
                                    if (myUid.equals(requesterId))
                                        continue;

                                    String requesterName =
                                            reqDoc.getString("requesterName");
                                    String hospital =
                                            reqDoc.getString("hospitalName");
                                    String city =
                                            reqDoc.getString("city");
                                    String urgency =
                                            reqDoc.getString("urgency");
                                    String requestId = reqDoc.getId();

                                    String title = getEmoji(urgency)
                                            + " " + urgency
                                            + " Blood Request!";
                                    String message = requesterName
                                            + " needs " + bloodGroup
                                            + " blood at " + hospital
                                            + ", " + city
                                            + ". Tap to view!";

                                    Log.d(TAG, "New request detected: "
                                            + requestId);

                                    // Show system notification popup
                                    showLocalNotification(
                                            context, title, message);

                                    // Save to in-app history
                                    saveToHistory(
                                            myUid, db,
                                            title, message,
                                            "BLOOD_REQUEST", requestId);

                                    // Update badge in UI
                                    if (callback != null) {
                                        callback.onNewRequest(
                                                title, message);
                                    }
                                }
                            });
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error loading user: "
                                + e.getMessage()));
    }

    // ── Show actual system notification popup ─────────────────
    public static void showLocalNotification(
            Context context,
            String title,
            String message) {

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context, BloodDonorApp.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_blooddrop)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(new NotificationCompat
                                .BigTextStyle().bigText(message))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            int notifId = (int) System.currentTimeMillis();
            manager.notify(notifId, builder.build());
            Log.d(TAG, "Local notification shown: " + title);
        }
    }

    // ── Stop listener ─────────────────────────────────────────
    public static void stopListening() {
        if (listenerReg != null) {
            listenerReg.remove();
            listenerReg = null;
            Log.d(TAG, "Listener stopped");
        }
    }

    // ── Save to Firestore notification history ─────────────────
    private static void saveToHistory(
            String uid,
            FirebaseFirestore db,
            String title,
            String message,
            String type,
            String relatedId) {

        Map<String, Object> notif = new HashMap<>();
        notif.put("title",     title);
        notif.put("message",   message);
        notif.put("type",      type);
        notif.put("relatedId", relatedId);
        notif.put("read",      false);
        notif.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .add(notif)
                .addOnSuccessListener(ref ->
                        Log.d(TAG, "Notification history saved"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "History save failed: "
                                + e.getMessage()));
    }

    private static String getEmoji(String urgency) {
        if (urgency == null) return "🩸";
        switch (urgency) {
            case "CRITICAL": return "🚨";
            case "URGENT":   return "⚠️";
            default:         return "🩸";
        }
    }
}