package com.blooddonor.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.blooddonor.R;
import com.blooddonor.adapters.DonorAdapter;
import com.blooddonor.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private Spinner spinnerBloodGroup;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoResults;
    private ToggleButton toggleAvailableOnly;

    private FirebaseFirestore db;
    private DonorAdapter adapter;
    private List<User> donorList;

    private final String[] BLOOD_GROUPS = {"All", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        donorList = new ArrayList<>();

        initViews(view);
        setupSpinner();
        setupRecyclerView();
        setupListeners();
        fetchDonors("All", "");
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        spinnerBloodGroup = view.findViewById(R.id.spinner_blood_group);
        recyclerView = view.findViewById(R.id.recycler_donors);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoResults = view.findViewById(R.id.tv_no_results);
        toggleAvailableOnly = view.findViewById(R.id.toggle_available_only);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, BLOOD_GROUPS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);
        spinnerBloodGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new DonorAdapter(requireContext(), donorList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        toggleAvailableOnly.setOnCheckedChangeListener((btn, checked) -> applyFilters());
    }

    private void applyFilters() {
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        String city = etSearch.getText().toString().trim();
        fetchDonors(bloodGroup, city);
    }

    private void fetchDonors(String bloodGroup, String city) {
        progressBar.setVisibility(View.VISIBLE);

        Query query = db.collection("users").whereEqualTo("donor", true);
        if (toggleAvailableOnly != null && toggleAvailableOnly.isChecked()) {
            query = query.whereEqualTo("available", true);
        }
        if (!bloodGroup.equals("All")) {
            query = query.whereEqualTo("bloodGroup", bloodGroup);
        }

        query.get().addOnSuccessListener(snapshots -> {
            progressBar.setVisibility(View.GONE);
            donorList.clear();
            for (var doc : snapshots.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    // City filter client-side for partial match
                    if (city.isEmpty() || user.getCity().toLowerCase()
                            .contains(city.toLowerCase())) {
                        donorList.add(user);
                    }
                }
            }
            adapter.notifyDataSetChanged();
            tvNoResults.setVisibility(donorList.isEmpty() ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show();
        });
    }
}
