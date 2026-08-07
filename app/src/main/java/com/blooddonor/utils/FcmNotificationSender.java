package com.blooddonor.utils;

import android.content.Context;
import android.util.Log;
import com.google.auth.oauth2.GoogleCredentials;
import okhttp3.*;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.Collections;

public class FcmNotificationSender {

    private static final String TAG = "FcmSender";
    private static final String FCM_URL = "https://fcm.googleapis.com/v1/projects/blooddonor-e680e/messages:send";

    public static void sendNotification(Context context, String token, String title, String body, String type, String relatedId) {
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token is null or empty, skipping FCM");
            return;
        }

        new Thread(() -> {
            try {
                String accessToken = getAccessToken(context);
                if (accessToken == null) {
                    Log.e(TAG, "Failed to get access token");
                    return;
                }

                OkHttpClient client = new OkHttpClient();

                // Create JSON payload for FCM v1
                JSONObject message = new JSONObject();
                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", body);
                
                // Add sound for iOS and some Android devices
                JSONObject androidConfig = new JSONObject();
                JSONObject androidNotification = new JSONObject();
                androidNotification.put("sound", "default");
                androidConfig.put("notification", androidNotification);

                JSONObject data = new JSONObject();
                data.put("type", type);
                data.put("relatedId", relatedId);
                data.put("title", title); 
                data.put("body", body);

                JSONObject messageContent = new JSONObject();
                messageContent.put("token", token);
                messageContent.put("notification", notification);
                messageContent.put("data", data);
                messageContent.put("android", androidConfig);

                message.put("message", messageContent);

                RequestBody requestBody = RequestBody.create(
                        message.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(FCM_URL)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String result = response.body() != null ? response.body().string() : "empty body";
                    Log.d(TAG, "FCM Response: " + result);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending FCM: " + e.getMessage());
            }
        }).start();
    }

    private static String getAccessToken(Context context) {
        try {
            InputStream is = context.getAssets().open("service-account.json");
            GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            Log.e(TAG, "AccessToken Error: " + e.getMessage());
            return null;
        }
    }
}
