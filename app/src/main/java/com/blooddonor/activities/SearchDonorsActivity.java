package com.blooddonor.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.blooddonor.R;
import com.blooddonor.adapters.DonorAdapter;
import com.blooddonor.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class SearchDonorsActivity extends AppCompatActivity {

    private Spinner spinnerBloodGroup;
    private EditText etCity;
    private Button btnSearch;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoResults;

    private FirebaseFirestore db;
    private DonorAdapter adapter;
    private List<User> donorList;

    private final String[] BLOOD_GROUPS = {"All", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_donors);

        db = FirebaseFirestore.getInstance();
        donorList = new ArrayList<>();
        initViews();
        setupRecyclerView();
        setupSpinner();
// Add back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Find Donors");
        }
        btnSearch.setOnClickListener(v -> searchDonors());
        // Load all donors initially
        searchDonors();
    }

    private void initViews() {
        spinnerBloodGroup = findViewById(R.id.spinner_blood_group);
        etCity = findViewById(R.id.et_city);
        btnSearch = findViewById(R.id.btn_search);
        recyclerView = findViewById(R.id.recycler_donors);
        progressBar = findViewById(R.id.progress_bar);
        tvNoResults = findViewById(R.id.tv_no_results);
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupRecyclerView() {
        adapter = new DonorAdapter(this, donorList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adp = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, BLOOD_GROUPS);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adp);
    }

    private void searchDonors() {
        String selectedBloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        String city = etCity.getText().toString().trim();

        setLoading(true);
        Query query = db.collection("users")
                .whereEqualTo("available", true)
                .whereEqualTo("donor", true);

        if (!selectedBloodGroup.equals("All")) {
            query = query.whereEqualTo("bloodGroup", selectedBloodGroup);
        }
        if (!TextUtils.isEmpty(city)) {
            query = query.whereEqualTo("city", city);
        }

        query.get().addOnSuccessListener(snapshots -> {
            setLoading(false);
            donorList.clear();
            for (var doc : snapshots.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) donorList.add(user);
            }
            adapter.notifyDataSetChanged();
            tvNoResults.setVisibility(donorList.isEmpty() ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
    }
}
