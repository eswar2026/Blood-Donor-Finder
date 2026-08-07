package com.blooddonor.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.blooddonor.R;
import com.blooddonor.models.User;
import com.blooddonor.utils.LocationHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 2001;

    private MapView mapView;
    private FirebaseFirestore db;
    private MyLocationNewOverlay locationOverlay;
    private double myLat = 20.5937; // Default: center of India
    private double myLng = 78.9629;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MUST be before setContentView
        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(
                        getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        db      = FirebaseFirestore.getInstance();
        mapView = findViewById(R.id.map);

        setupMap();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Donor Map");
        }
        // Check location permission
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        }

        // Load all donor pins
        loadDonorsOnMap();

        // FAB — go to my location
        findViewById(R.id.fab_my_location).setOnClickListener(v -> {
            if (locationOverlay != null
                    && locationOverlay.getMyLocation() != null) {
                mapView.getController().animateTo(
                        locationOverlay.getMyLocation());
                mapView.getController().setZoom(14.0);
            } else {
                Toast.makeText(this,
                        "Getting your location...",
                        Toast.LENGTH_SHORT).show();
                zoomToMyLocation();
            }
        });
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        // Start at India center
        mapView.getController().setZoom(5.0);
        mapView.getController().setCenter(
                new GeoPoint(myLat, myLng));
    }

    private void enableMyLocation() {
        // Blue dot overlay showing user's real-time location
        locationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(this), mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        // Once location is found, zoom to it
        locationOverlay.runOnFirstFix(() ->
                runOnUiThread(() -> {
                    if (locationOverlay.getMyLocation() != null) {
                        myLat = locationOverlay.getMyLocation().getLatitude();
                        myLng = locationOverlay.getMyLocation().getLongitude();
                        mapView.getController().animateTo(
                                locationOverlay.getMyLocation());
                        mapView.getController().setZoom(13.0);
                        // Stop following after first fix
                        locationOverlay.disableFollowLocation();
                    }
                }));

        mapView.getOverlays().add(locationOverlay);
        mapView.invalidate();
    }

    private void zoomToMyLocation() {
        LocationHelper.getCurrentLocation(this,
                new LocationHelper.LocationCallback() {
                    @Override
                    public void onLocationReceived(double lat, double lng) {
                        myLat = lat;
                        myLng = lng;
                        runOnUiThread(() -> {
                            mapView.getController().animateTo(
                                    new GeoPoint(lat, lng));
                            mapView.getController().setZoom(13.0);
                        });
                    }
                    @Override
                    public void onLocationFailed(String error) {
                        runOnUiThread(() ->
                                Toast.makeText(MapActivity.this,
                                        "Location not available: " + error,
                                        Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void loadDonorsOnMap() {
        db.collection("users")
                .whereEqualTo("available", true)
                .whereEqualTo("donor", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int donorCount = 0;
                    for (var doc : snapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            if (user.getLatitude() != 0.0
                                    || user.getLongitude() != 0.0) {
                                // Has real coordinates — add pin
                                addDonorMarker(user);
                                donorCount++;
                            }
                        }
                    }
                    if (donorCount == 0) {
                        Toast.makeText(this,
                                "No donors with location found yet.\n" +
                                        "Ask donors to open the app to share location.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                donorCount + " donor(s) found on map",
                                Toast.LENGTH_SHORT).show();
                    }
                    mapView.invalidate();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load donors: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void addDonorMarker(User user) {
        Marker marker = new Marker(mapView);
        marker.setPosition(
                new GeoPoint(user.getLatitude(), user.getLongitude()));
        marker.setTitle(user.getName()
                + "  |  " + user.getBloodGroup());
        marker.setSnippet(
                "📍 " + user.getCity() + ", " + user.getState()
                        + "\n📞 " + user.getPhone()
                        + "\n🩸 Available: " + (user.isAvailable() ? "Yes" : "No"));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        marker.setOnMarkerClickListener((m, mapV) -> {
            m.showInfoWindow();
            return true;
        });

        mapView.getOverlays().add(marker);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                Toast.makeText(this,
                        "Location enabled",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Location permission denied — blue dot unavailable",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (locationOverlay != null) locationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
    }
}