package com.blooddonor.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.blooddonor.R;
import com.blooddonor.fragments.HomeFragment;
import com.blooddonor.fragments.ProfileFragment;
import com.blooddonor.fragments.RequestsFragment;
import com.blooddonor.fragments.SearchFragment;
import com.blooddonor.utils.InAppNotificationManager;
import com.blooddonor.utils.LocationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabRequest;
    private ImageView ivNotificationBell;
    private TextView tvNotificationBadge;
    private ViewPager2 viewPager;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private int unreadCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            FirebaseFirestore.getInstance()
                    .setFirestoreSettings(
                            new FirebaseFirestoreSettings.Builder()
                                    .setPersistenceEnabled(true)
                                    .build());
        } catch (Exception ignored) {}

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupViewPager();
        setupBottomNav();
        setupNotificationBell();
        
        requestLocationAndSave();
        requestNotificationPermission();
        startNotificationListener();
        loadUnreadCount();
        handleRedirection(getIntent());
    }

    private void initViews() {
        bottomNav           = findViewById(R.id.bottom_navigation);
        fabRequest          = findViewById(R.id.fab_request);
        ivNotificationBell  = findViewById(R.id.iv_notification_bell);
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);
        viewPager           = findViewById(R.id.view_pager);
    }

    private void setupViewPager() {
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);
        
        // Sync ViewPager swipe with BottomNav selection
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: bottomNav.setSelectedItemId(R.id.nav_home); break;
                    case 1: bottomNav.setSelectedItemId(R.id.nav_search); break;
                    case 2: bottomNav.setSelectedItemId(R.id.nav_requests); break;
                    case 3: bottomNav.setSelectedItemId(R.id.nav_profile); break;
                }
            }
        });
        
        // Optimize ViewPager performance
        viewPager.setOffscreenPageLimit(3);
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                viewPager.setCurrentItem(0, false);
                return true;
            }
            if (id == R.id.nav_search) {
                viewPager.setCurrentItem(1, false);
                return true;
            }
            if (id == R.id.nav_requests) {
                viewPager.setCurrentItem(2, false);
                return true;
            }
            if (id == R.id.nav_profile) {
                viewPager.setCurrentItem(3, false);
                return true;
            }
            return false;
        });

        fabRequest.setOnClickListener(v ->
                startActivity(new Intent(this, RequestBloodActivity.class)));
    }

    private void setupNotificationBell() {
        ivNotificationBell.setOnClickListener(v -> {
            unreadCount = 0;
            tvNotificationBadge.setVisibility(View.GONE);
            startActivity(new Intent(this, NotificationsActivity.class));
        });
    }

    private void startNotificationListener() {
        InAppNotificationManager.startListening(
                this,
                (title, message) -> runOnUiThread(() -> {
                    unreadCount++;
                    updateBadge(unreadCount);
                    
                    // Play notification sound for in-app alert
                    try {
                        android.net.Uri notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
                        android.media.Ringtone r = android.media.RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
        );
    }

    public void loadUnreadCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        InAppNotificationManager.loadUnreadCount(
                user.getUid(),
                count -> runOnUiThread(() -> {
                    unreadCount = count;
                    updateBadge(unreadCount);
                }));
    }

    private void updateBadge(int count) {
        if (count > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    public void navigateToRequests(boolean showMyRequests) {
        // Select the Requests item in bottom nav
        bottomNav.setSelectedItemId(R.id.nav_requests);
        
        // If we want to show the "My Requests" tab specifically, notify the fragment
        if (showMyRequests) {
            Bundle result = new Bundle();
            result.putBoolean("showMyRequests", true);
            getSupportFragmentManager().setFragmentResult("request_tab_switch", result);
        }
    }

    private void requestLocationAndSave() {
        if (LocationHelper.hasLocationPermission(this)) {
            LocationHelper.updateUserLocation(this);
        } else {
            LocationHelper.requestLocationPermission(this);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void handleRedirection(Intent intent) {
        if (intent == null) return;
        String type = intent.getStringExtra("type");
        String relatedId = intent.getStringExtra("relatedId");
        if (relatedId != null && !relatedId.isEmpty()) {
            if ("BLOOD_REQUEST".equals(type) || "DONOR_FOUND".equals(type)) {
                Intent detailIntent = new Intent(this, RequestDetailActivity.class);
                detailIntent.putExtra(RequestDetailActivity.EXTRA_REQUEST_ID, relatedId);
                startActivity(detailIntent);
                intent.removeExtra("type");
                intent.removeExtra("relatedId");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUnreadCount();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleRedirection(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LocationHelper.updateUserLocation(this);
            }
        }
    }

    public void logout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    InAppNotificationManager.stopListening();
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finishAffinity();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        InAppNotificationManager.stopListening();
    }

    // Adapter class for ViewPager2
    private static class MainPagerAdapter extends FragmentStateAdapter {
        public MainPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1: return new SearchFragment();
                case 2: return new RequestsFragment();
                case 3: return new ProfileFragment();
                default: return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
