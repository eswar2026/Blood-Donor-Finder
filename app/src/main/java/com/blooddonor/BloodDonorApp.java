package com.blooddonor;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class BloodDonorApp extends Application {

    public static final String CHANNEL_ID   =
            "blood_donor_channel";
    public static final String CHANNEL_NAME =
            "Blood Donor Alerts";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(this);
    }

    public static void createNotificationChannel(
            Context context) {
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH);
            
            // Set default sound for the channel
            android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes);

            channel.setDescription(
                    "Blood donation urgent alerts");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(
                    android.app.Notification
                            .VISIBILITY_PUBLIC);

            NotificationManager manager =
                    (NotificationManager)
                            context.getSystemService(
                                    NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(
                        channel);
            }
        }
    }
}