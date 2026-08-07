package com.blooddonor.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LocationHelper {

    public static final int LOCATION_PERMISSION_CODE = 1001;

    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
        void onLocationFailed(String error);
    }

    public static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_CODE);
    }

    public static void getCurrentLocation(Context context,
                                          LocationCallback callback) {
        if (!hasLocationPermission(context)) {
            callback.onLocationFailed("Location permission not granted");
            return;
        }

        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(context.getApplicationContext());

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.onLocationFailed("Permission denied");
            return;
        }

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocationReceived(
                                location.getLatitude(),
                                location.getLongitude());
                    } else {
                        // Try last known location as fallback
                        client.getLastLocation()
                                .addOnSuccessListener(lastLocation -> {
                                    if (lastLocation != null) {
                                        callback.onLocationReceived(
                                                lastLocation.getLatitude(),
                                                lastLocation.getLongitude());
                                    } else {
                                        callback.onLocationFailed(
                                                "Could not get location");
                                    }
                                })
                                .addOnFailureListener(e ->
                                        callback.onLocationFailed(e.getMessage()));
                    }
                })
                .addOnFailureListener(e ->
                        callback.onLocationFailed(e.getMessage()));
    }

    public static void updateUserLocation(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        getCurrentLocation(context, new LocationCallback() {
            @Override
            public void onLocationReceived(double lat, double lng) {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("latitude", lat, "longitude", lng);
            }
            @Override
            public void onLocationFailed(String error) {
                // Silent — not critical
            }
        });
    }

    public static double calculateDistance(double lat1, double lon1,
                                           double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000.0;
    }
}