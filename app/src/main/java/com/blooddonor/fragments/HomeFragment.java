package com.blooddonor.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Collections;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.blooddonor.R;
import com.blooddonor.activities.MainActivity;
import com.blooddonor.activities.MapActivity;
import com.blooddonor.activities.SearchDonorsActivity;
import com.blooddonor.adapters.BloodRequestAdapter;
import com.blooddonor.models.BloodRequest;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvWelcome, tvStats;
    private RecyclerView recyclerRecentRequests;
    private MaterialCardView cardFindDonors, cardViewMap, cardMyRequests;
    private BloodRequestAdapter adapter;
    private List<BloodRequest> requestList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        requestList = new ArrayList<>();

        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadUserName();
        loadRecentRequests();
        loadStats();
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvStats = view.findViewById(R.id.tv_stats);
        recyclerRecentRequests = view.findViewById(R.id.recycler_recent_requests);
        cardFindDonors = view.findViewById(R.id.card_find_donors);
        cardViewMap = view.findViewById(R.id.card_view_map);
        cardMyRequests = view.findViewById(R.id.card_my_requests);
    }

    private void setupRecyclerView() {
        adapter = new BloodRequestAdapter(requireContext(), requestList);
        recyclerRecentRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRecentRequests.setAdapter(adapter);
        recyclerRecentRequests.setNestedScrollingEnabled(true);
    }

    private void setupClickListeners() {
        cardFindDonors.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        SearchDonorsActivity.class)));

        cardViewMap.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        MapActivity.class)));

        // Fix: navigate to Requests tab in MainActivity
        cardMyRequests.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.navigateToRequests(true);
            }
        });
    }

    private void loadUserName() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        tvWelcome.setText("Hello, " + name + " 👋");
                    } else {
                        tvWelcome.setText("Hello, User 👋");
                    }
                });
    }

    private void loadRecentRequests() {
        // No orderBy — sort in Java to avoid index requirement
        db.collection("blood_requests")
                .whereEqualTo("status", "OPEN")
                .limit(10)
                .get()
                .addOnSuccessListener(snapshots -> {
                    requestList.clear();

                    for (var doc : snapshots.getDocuments()) {
                        BloodRequest req = doc.toObject(BloodRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId());
                            requestList.add(req);
                        }
                    }

                    // Sort newest first in Java
                    Collections.sort(requestList, (a, b) ->
                            Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // Silent fail on home screen — not critical
                });
    }

    private void loadStats() {
        db.collection("users").whereEqualTo("donor", true)
                .whereEqualTo("available", true).get()
                .addOnSuccessListener(snap ->
                        tvStats.setText(snap.size() + " donors available nearby"));
    }
}
