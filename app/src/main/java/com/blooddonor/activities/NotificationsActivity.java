package com.blooddonor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.blooddonor.R;
import com.blooddonor.adapters.NotificationAdapter;
import com.blooddonor.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView    recyclerView;
    private ProgressBar     progressBar;
    private TextView        tvEmpty;

    private FirebaseFirestore   db;
    private NotificationAdapter adapter;
    private List<Notification>  notificationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        if (getSupportActionBar() != null) {
            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(true);
            getSupportActionBar()
                    .setTitle("Notifications");
        }

        db               = FirebaseFirestore.getInstance();
        notificationList = new ArrayList<>();

        recyclerView = findViewById(
                R.id.recycler_notifications);
        progressBar  = findViewById(R.id.progress_bar);
        tvEmpty      = findViewById(R.id.tv_empty);

        adapter = new NotificationAdapter(
                this, notificationList);

        // Tap notification → redirect to request detail
        adapter.setOnNotificationClickListener(notification -> {
            String type = notification.getType();
            String relatedId = notification.getRelatedId();

            if (relatedId != null && !relatedId.isEmpty()) {
                if ("BLOOD_REQUEST".equals(type) || "DONOR_FOUND".equals(type)) {
                    Intent intent = new Intent(this, RequestDetailActivity.class);
                    intent.putExtra(RequestDetailActivity.EXTRA_REQUEST_ID, relatedId);
                    startActivity(intent);
                }
            }
        });

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt",
                        Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);
                    notificationList.clear();

                    if (snapshots.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(
                                "No notifications yet.\n\n"
                                        + "You will receive alerts when\n"
                                        + "someone posts a blood request\n"
                                        + "matching your blood group.");
                        return;
                    }

                    for (var doc :
                            snapshots.getDocuments()) {

                        Notification n = new Notification();
                        n.setNotificationId(doc.getId());

                        // Read title
                        String title =
                                doc.getString("title");
                        n.setTitle(title != null
                                ? title : "Notification");

                        // Read message
                        String message =
                                doc.getString("message");
                        n.setMessage(message != null
                                ? message : "");

                        // Read type
                        String type =
                                doc.getString("type");
                        n.setType(type != null
                                ? type : "GENERAL");

                        // Read relatedId
                        String related =
                                doc.getString("relatedId");
                        n.setRelatedId(related != null
                                ? related : "");

                        // Handle both "read" and "isRead"
                        Boolean read =
                                doc.getBoolean("read");
                        if (read == null)
                            read = doc.getBoolean("isRead");
                        n.setRead(read != null && read);

                        // Read timestamp
                        Long createdAt =
                                doc.getLong("createdAt");
                        n.setCreatedAt(createdAt != null
                                ? createdAt : 0L);

                        notificationList.add(n);
                    }

                    adapter.notifyDataSetChanged();
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);

                    // Mark all as read
                    markAllRead(uid);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(
                            "Failed to load.\n"
                                    + "Check your internet connection.");
                });
    }

    private void markAllRead(String uid) {
        for (Notification n : notificationList) {
            if (!n.isRead()) {
                db.collection("users")
                        .document(uid)
                        .collection("notifications")
                        .document(n.getNotificationId())
                        .update("read", true);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}