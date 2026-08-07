package com.blooddonor.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.blooddonor.R;
import com.blooddonor.activities.RequestBloodActivity;
import com.blooddonor.adapters.BloodRequestAdapter;
import com.blooddonor.models.BloodRequest;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestsFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BloodRequestAdapter adapter;
    private List<BloodRequest> requestList;
    private boolean showingMyRequests = false;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        requestList = new ArrayList<>();

        initViews(view);
        setupRecyclerView();
        setupTabs();
        setupSwipeRefresh();

        // Listen for navigation requests to switch to "My Requests" tab
        getParentFragmentManager().setFragmentResultListener("request_tab_switch", getViewLifecycleOwner(), (requestKey, bundle) -> {
            boolean showMyRequests = bundle.getBoolean("showMyRequests", false);
            if (showMyRequests && tabLayout != null) {
                TabLayout.Tab myRequestsTab = tabLayout.getTabAt(1);
                if (myRequestsTab != null) {
                    myRequestsTab.select();
                }
            }
        });

        // Load immediately on open
        loadRequests();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload every time user comes back to this tab
        loadRequests();
    }

    private void initViews(View view) {
        tabLayout   = view.findViewById(R.id.tab_layout);
        recyclerView = view.findViewById(R.id.recycler_requests);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar  = view.findViewById(R.id.progress_bar);
        tvEmpty      = view.findViewById(R.id.tv_empty);
    }

    private void setupRecyclerView() {
        adapter = new BloodRequestAdapter(requireContext(), requestList);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Edit click
        adapter.setOnEditClickListener(request -> {
            Intent intent = new Intent(requireContext(),
                    RequestBloodActivity.class);
            intent.putExtra("edit_mode",    true);
            intent.putExtra("request_id",   request.getRequestId());
            intent.putExtra("blood_group",  request.getBloodGroup());
            intent.putExtra("hospital",     request.getHospitalName());
            intent.putExtra("city",         request.getCity());
            intent.putExtra("state",        request.getState());
            intent.putExtra("units",        request.getUnitsNeeded());
            intent.putExtra("urgency",      request.getUrgency());
            intent.putExtra("patient",      request.getPatientName());
            intent.putExtra("notes",        request.getAdditionalNotes());
            intent.putExtra("phone",        request.getRequesterPhone());
            startActivity(intent);
        });

        // Delete click
        adapter.setOnDeleteClickListener(request ->
                showDeleteConfirmation(request));
    }

    private void showDeleteConfirmation(BloodRequest request) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to delete this blood request?\n\nThis cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) ->
                        deleteRequest(request))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRequest(BloodRequest request) {
        FirebaseFirestore.getInstance()
                .collection("blood_requests")
                .document(request.getRequestId())
                .delete()
                .addOnSuccessListener(unused -> {
                    requestList.remove(request);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(),
                            "Request deleted successfully",
                            Toast.LENGTH_SHORT).show();
                    if (requestList.isEmpty()) {
                        showEmpty("No requests found.");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All Requests"));
        tabLayout.addTab(tabLayout.newTab().setText("My Requests"));

        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        showingMyRequests = tab.getPosition() == 1;
                        loadRequests();
                    }
                    @Override public void onTabUnselected(TabLayout.Tab tab) {}
                    @Override public void onTabReselected(TabLayout.Tab tab) {
                        // Reload when user taps the same tab again
                        loadRequests();
                    }
                });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
                getResources().getColor(R.color.blood_red, null));
        swipeRefresh.setOnRefreshListener(() -> {
            // Force reload from server, ignore cache
            loadRequestsFromServer();
        });
    }

    // ── Normal load — tries cache first, then server ─────────
    private void loadRequests() {
        if (isLoading) return;
        isLoading = true;

        showLoading(true);

        if (showingMyRequests) {
            loadMyRequests(Source.DEFAULT);
        } else {
            loadAllRequests(Source.DEFAULT);
        }
    }

    // ── Force reload from server — used by swipe refresh ─────
    private void loadRequestsFromServer() {
        if (showingMyRequests) {
            loadMyRequests(Source.SERVER);
        } else {
            loadAllRequests(Source.SERVER);
        }
    }

    private void loadAllRequests(Source source) {
        // ONLY show OPEN requests to everyone
        // FULFILLED requests disappear from this list automatically
        db.collection("blood_requests")
                .whereEqualTo("status", "OPEN")
                .limit(50)
                .get(source)
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded()) return;
                    isLoading = false;
                    swipeRefresh.setRefreshing(false);
                    showLoading(false);
                    requestList.clear();

                    for (var doc : snapshots.getDocuments()) {
                        BloodRequest req =
                                doc.toObject(BloodRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId());
                            requestList.add(req);
                        }
                    }

                    // Sort newest first
                    Collections.sort(requestList, (a, b) ->
                            Long.compare(
                                    b.getCreatedAt(),
                                    a.getCreatedAt()));

                    adapter.notifyDataSetChanged();

                    if (requestList.isEmpty()) {
                        showEmpty("No open blood requests found.\n" +
                                "Tap + to post one.");
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    isLoading = false;
                    swipeRefresh.setRefreshing(false);
                    showLoading(false);
                    retryFromCache();
                });
    }

    private void loadMyRequests(Source source) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            isLoading = false;
            showLoading(false);
            showEmpty("Please log in");
            return;
        }

        String uid = user.getUid();

        // NO status filter here — show OPEN, FULFILLED, CLOSED
        db.collection("blood_requests")
                .whereEqualTo("requesterId", uid)
                .limit(30)
                .get(source)
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded()) return;
                    isLoading = false;
                    swipeRefresh.setRefreshing(false);
                    showLoading(false);
                    requestList.clear();

                    for (var doc : snapshots.getDocuments()) {
                        BloodRequest req =
                                doc.toObject(BloodRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId());
                            requestList.add(req);
                        }
                    }

                    // Sort newest first
                    Collections.sort(requestList, (a, b) ->
                            Long.compare(
                                    b.getCreatedAt(),
                                    a.getCreatedAt()));

                    adapter.notifyDataSetChanged();

                    if (requestList.isEmpty()) {
                        showEmpty(
                                "You have not posted any requests yet.\n" +
                                        "Tap + to post one.");
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    isLoading = false;
                    swipeRefresh.setRefreshing(false);
                    showLoading(false);
                    showEmpty("Failed to load. Pull down to refresh.");
                });
    }

    // ── Retry from local Firestore cache if server fails ─────
    private void retryFromCache() {
        if (!isAdded()) return;

        Toast.makeText(requireContext(),
                "Checking local cache...",
                Toast.LENGTH_SHORT).show();

        if (showingMyRequests) {
            loadMyRequests(Source.CACHE);
        } else {
            loadAllRequests(Source.CACHE);
        }
    }

    private void showLoading(boolean loading) {
        if (!isAdded()) return;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void showEmpty(String message) {
        if (!isAdded()) return;
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }
}