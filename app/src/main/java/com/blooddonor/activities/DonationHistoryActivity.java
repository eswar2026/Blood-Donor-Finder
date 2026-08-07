package com.blooddonor.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.blooddonor.R;
import com.blooddonor.adapters.DonationHistoryAdapter;
import com.blooddonor.models.DonationHistory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DonationHistoryActivity extends AppCompatActivity {

    private TextView tvTotalCount, tvLastDate,
            tvNextEligible, tvEmpty;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    private FirebaseFirestore db;
    private DonationHistoryAdapter adapter;
    private List<DonationHistory> historyList;

    private final SimpleDateFormat displaySdf =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Donation History");
        }

        db          = FirebaseFirestore.getInstance();
        historyList = new ArrayList<>();

        initViews();
        setupRecyclerView();
        loadDonationHistory();
    }

    private void initViews() {
        tvTotalCount   = findViewById(R.id.tv_total_count);
        tvLastDate     = findViewById(R.id.tv_last_date);
        tvNextEligible = findViewById(R.id.tv_next_eligible);
        tvEmpty        = findViewById(R.id.tv_empty);
        progressBar    = findViewById(R.id.progress_bar);
        recyclerView   = findViewById(R.id.recycler_history);
    }

    private void setupRecyclerView() {
        adapter = new DonationHistoryAdapter(this, historyList);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadDonationHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .document(user.getUid())
                .collection("donation_history")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);
                    historyList.clear();

                    for (var doc : snapshots.getDocuments()) {
                        DonationHistory item =
                                doc.toObject(DonationHistory.class);
                        if (item != null) {
                            item.setDonationId(doc.getId());
                            historyList.add(item);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    // Update header stats
                    tvTotalCount.setText(String.valueOf(historyList.size()));

                    if (!historyList.isEmpty()) {
                        String lastDate = historyList.get(0).getDonationDate();
                        tvLastDate.setText("Last donated: " + lastDate);
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        showNextEligibleDate(lastDate);
                    } else {
                        tvLastDate.setText("Last donated: Never");
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Failed to load history.\nTap back and try again.");
                });
    }

    // Donors must wait 90 days between donations
    private void showNextEligibleDate(String lastDonationDate) {
        try {
            Date lastDate = displaySdf.parse(lastDonationDate);
            if (lastDate == null) return;

            Calendar cal = Calendar.getInstance();
            cal.setTime(lastDate);
            cal.add(Calendar.DAY_OF_YEAR, 90);
            Date nextEligible = cal.getTime();

            Date today = new Date();
            if (today.after(nextEligible)) {
                tvNextEligible.setText(
                        "✅ You are eligible to donate again!");
            } else {
                long diffMs  = nextEligible.getTime() - today.getTime();
                long diffDays = diffMs / (1000 * 60 * 60 * 24);
                tvNextEligible.setText(
                        "Next eligible: "
                                + displaySdf.format(nextEligible)
                                + " (" + diffDays + " days left)");
            }
        } catch (ParseException e) {
            // Silent — date format mismatch
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}