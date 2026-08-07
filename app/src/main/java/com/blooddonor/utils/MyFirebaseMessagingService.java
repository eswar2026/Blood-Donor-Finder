package com.blooddonor.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.blooddonor.BloodDonorApp;
import com.blooddonor.R;
import com.blooddonor.activities.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService
        extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New token: " + token);

        BloodDonorApp.createNotificationChannel(this);

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update("fcmToken", token)
                    .addOnSuccessListener(v ->
                            Log.d(TAG, "Token saved"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Token save failed: "
                                    + e.getMessage()));
        } else {
            getSharedPreferences("fcm_prefs",
                    MODE_PRIVATE)
                    .edit()
                    .putString("pending_token", token)
                    .apply();
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        super.onMessageReceived(msg);
        Log.d(TAG, "Message from: " + msg.getFrom());

        String title = "Blood Donor Finder";
        String body  = "";

        if (msg.getNotification() != null) {
            if (msg.getNotification().getTitle() != null)
                title = msg.getNotification().getTitle();
            if (msg.getNotification().getBody() != null)
                body = msg.getNotification().getBody();
        }

        if (!msg.getData().isEmpty()) {
            if (msg.getData().containsKey("title"))
                title = msg.getData().get("title");
            if (msg.getData().containsKey("body"))
                body = msg.getData().get("body");
        }

        Log.d(TAG, "Title: " + title + " Body: " + body);

        String type = "GENERAL";
        String relatedId = "";

        if (!msg.getData().isEmpty()) {
            if (msg.getData().containsKey("type"))
                type = msg.getData().get("type");
            if (msg.getData().containsKey("relatedId"))
                relatedId = msg.getData().get("relatedId");
        }

        showPopupNotification(this, title, body, type, relatedId);
    }

    public static void showPopupNotification(
            Context context,
            String title,
            String body,
            String type,
            String relatedId) {

        BloodDonorApp.createNotificationChannel(context);

        Intent intent =
                new Intent(context, MainActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        intent.putExtra("type",      type);
        intent.putExtra("relatedId", relatedId);

        PendingIntent pi = PendingIntent.getActivity(
                context, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context, BloodDonorApp.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_blooddrop)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat
                                .BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(pi);

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            int id = (int) System.currentTimeMillis();
            manager.notify(id, builder.build());
        }
    }
}
