package com.blooddonor.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import com.blooddonor.utils.InAppNotificationManager;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.blooddonor.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RequestBloodActivity extends AppCompatActivity {

    private Spinner  spinnerBloodGroup, spinnerUrgency, spinnerUnits;
    private EditText etHospitalName, etCity, etState,
            etAddress, etPatientName, etNotes, etPhone;
    private Button   btnSubmitRequest;
    private ProgressBar progressBar;

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;

    // Edit mode
    private boolean isEditMode    = false;
    private String  editRequestId = null;

    private final String[] BLOOD_GROUPS = {
            "A+","A-","B+","B-","AB+","AB-","O+","O-"
    };
    private final String[] URGENCY = {
            "CRITICAL","URGENT","NORMAL"
    };
    private final String[] UNITS = {
            "1","2","3","4","5","6","7","8","9","10"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_blood);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        initViews();
        setupSpinners();

        // Check if opened in edit mode
        isEditMode    = getIntent()
                .getBooleanExtra("edit_mode", false);
        editRequestId = getIntent()
                .getStringExtra("request_id");

        if (isEditMode) {
            if (getSupportActionBar() != null) {
                getSupportActionBar()
                        .setDisplayHomeAsUpEnabled(true);
                getSupportActionBar()
                        .setTitle("Edit Request");
            }
            btnSubmitRequest.setText("UPDATE REQUEST");
            prefillForEdit();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar()
                        .setDisplayHomeAsUpEnabled(true);
                getSupportActionBar()
                        .setTitle("Request Blood");
            }
            loadCurrentUserDefaults();
        }

        btnSubmitRequest.setOnClickListener(v -> {
            if (isEditMode) updateRequest();
            else            submitNewRequest();
        });
    }

    // ── Init ──────────────────────────────────────────────────

    private void initViews() {
        spinnerBloodGroup = findViewById(R.id.spinner_blood_group);
        spinnerUrgency    = findViewById(R.id.spinner_urgency);
        spinnerUnits      = findViewById(R.id.spinner_units);
        etHospitalName    = findViewById(R.id.et_hospital_name);
        etCity            = findViewById(R.id.et_city);
        etState           = findViewById(R.id.et_state);
        etAddress         = findViewById(R.id.et_address);
        etPatientName     = findViewById(R.id.et_patient_name);
        etNotes           = findViewById(R.id.et_notes);
        etPhone           = findViewById(R.id.et_phone);
        btnSubmitRequest  = findViewById(R.id.btn_submit_request);
        progressBar       = findViewById(R.id.progress_bar);
    }

    private void setupSpinners() {
        setupSpinner(spinnerBloodGroup, BLOOD_GROUPS);
        setupSpinner(spinnerUrgency,    URGENCY);
        setupSpinner(spinnerUnits,      UNITS);
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                items);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // ── Prefill for edit mode ─────────────────────────────────

    private void prefillForEdit() {
        setSpinnerValue(spinnerBloodGroup, BLOOD_GROUPS,
                getIntent().getStringExtra("blood_group"));
        setSpinnerValue(spinnerUrgency, URGENCY,
                getIntent().getStringExtra("urgency"));
        setSpinnerValue(spinnerUnits, UNITS,
                String.valueOf(
                        getIntent().getIntExtra("units", 1)));

        etHospitalName.setText(
                getIntent().getStringExtra("hospital"));
        etCity.setText(
                getIntent().getStringExtra("city"));
        etState.setText(
                getIntent().getStringExtra("state"));
        etPatientName.setText(
                getIntent().getStringExtra("patient"));
        etNotes.setText(
                getIntent().getStringExtra("notes"));
        etPhone.setText(
                getIntent().getStringExtra("phone"));
    }

    private void setSpinnerValue(
            Spinner spinner,
            String[] items,
            String value) {
        if (value == null) return;
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    // ── Load current user's city/state/phone as defaults ──────

    private void loadCurrentUserDefaults() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String city  = doc.getString("city");
                        String state = doc.getString("state");
                        String phone = doc.getString("phone");

                        if (city  != null) etCity.setText(city);
                        if (state != null) etState.setText(state);
                        if (phone != null) etPhone.setText(phone);
                    }
                });
    }

    // ── Validation ────────────────────────────────────────────

    private boolean validate() {
        String hospital = etHospitalName.getText()
                .toString().trim();
        String city  = etCity.getText().toString().trim();
        String state = etState.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(hospital)) {
            etHospitalName.setError("Hospital name required");
            etHospitalName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(city)) {
            etCity.setError("City required");
            etCity.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(state)) {
            etState.setError("State required");
            etState.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Contact phone required");
            etPhone.requestFocus();
            return false;
        }
        return true;
    }

    // ── Submit New Request ────────────────────────────────────

    private void submitNewRequest() {
        if (!validate()) return;
        if (mAuth.getCurrentUser() == null) return;

        setLoading(true);

        String uid      = mAuth.getCurrentUser().getUid();
        String hospital = etHospitalName.getText()
                .toString().trim();
        String city     = etCity.getText().toString().trim();
        String state    = etState.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String bg       = spinnerBloodGroup
                .getSelectedItem().toString();
        String urgency  = spinnerUrgency
                .getSelectedItem().toString();
        int units       = Integer.parseInt(
                spinnerUnits.getSelectedItem().toString());

        // Get requester name from Firestore first
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = "Unknown";
                    if (doc.exists()
                            && doc.getString("name") != null) {
                        name = doc.getString("name");
                    }

                    final String finalName = name;

                    Map<String, Object> data = new HashMap<>();
                    data.put("requesterId",    uid);
                    data.put("requesterName",  finalName);
                    data.put("requesterPhone", phone);
                    data.put("bloodGroup",     bg);
                    data.put("hospitalName",   hospital);
                    data.put("city",           city);
                    data.put("state",          state);
                    data.put("address",
                            etAddress.getText().toString().trim());
                    data.put("patientName",
                            etPatientName.getText().toString().trim());
                    data.put("additionalNotes",
                            etNotes.getText().toString().trim());
                    data.put("unitsNeeded",    units);
                    data.put("urgency",        urgency);
                    data.put("status",         "OPEN");
                    data.put("createdAt",
                            System.currentTimeMillis());
                    data.put("expiresAt",
                            System.currentTimeMillis()
                                    + (7L * 24 * 60 * 60 * 1000));

                    db.collection("blood_requests")
                            .add(data)
                            .addOnSuccessListener(ref -> {
                                ref.update("requestId", ref.getId());
                                setLoading(false);

                                // Save notifications to matching donors
                                saveNotificationsForMatchingDonors(
                                        uid, bg, hospital,
                                        city, finalName, ref.getId());

                                Toast.makeText(this,
                                        "Request posted! "
                                                + "Donors are being notified.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this,
                                        "Error posting request: "
                                                + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Error loading profile: "
                                    + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ── Save notifications to matching donors ─────────────────

    private void saveNotificationsForMatchingDonors(
            String myUid,
            String bloodGroup,
            String hospital,
            String city,
            String requesterName,
            String requestId) {

        String urgency = spinnerUrgency
                .getSelectedItem().toString();

        String emoji;
        switch (urgency) {
            case "CRITICAL": emoji = "🚨"; break;
            case "URGENT":   emoji = "⚠️"; break;
            default:         emoji = "🩸"; break;
        }

        String title = emoji + " "
                + urgency + " Blood Request!";
        String body  = requesterName
                + " needs " + bloodGroup
                + " blood at " + hospital
                + ", " + city
                + ". Tap to view and help!";

        db.collection("users")
                .whereEqualTo("donor",      true)
                .whereEqualTo("available",  true)
                .whereEqualTo("bloodGroup", bloodGroup)
                .get()
                .addOnSuccessListener(snapshots -> {
                    android.util.Log.d("RequestActivity",
                            "Found " + snapshots.size()
                                    + " matching donors");

                    for (var doc :
                            snapshots.getDocuments()) {
                        String donorUid = doc.getId();

                        // Skip self
                        if (donorUid.equals(myUid))
                            continue;

                        android.util.Log.d(
                                "RequestActivity",
                                "Saving notif for: "
                                        + doc.getString("name"));

                        // Save to donor's Firestore
                        // notification history
                        InAppNotificationManager
                                .saveNotification(
                                        db,
                                        donorUid,
                                        title,
                                        body,
                                        "BLOOD_REQUEST",
                                        requestId);

                        // Send real push notification
                        String token = doc.getString("fcmToken");
                        if (token != null && !token.isEmpty()) {
                            com.blooddonor.utils.FcmNotificationSender
                                    .sendNotification(
                                            getApplicationContext(),
                                            token,
                                            title,
                                            body,
                                            "BLOOD_REQUEST",
                                            requestId);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        android.util.Log.e(
                                "RequestActivity",
                                "Error: " + e.getMessage()));
    }
    // ── Update Existing Request ───────────────────────────────

    private void updateRequest() {
        if (!validate()) return;
        if (editRequestId == null) return;

        setLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("bloodGroup",
                spinnerBloodGroup.getSelectedItem().toString());
        updates.put("urgency",
                spinnerUrgency.getSelectedItem().toString());
        updates.put("unitsNeeded",
                Integer.parseInt(
                        spinnerUnits.getSelectedItem().toString()));
        updates.put("hospitalName",
                etHospitalName.getText().toString().trim());
        updates.put("city",
                etCity.getText().toString().trim());
        updates.put("state",
                etState.getText().toString().trim());
        updates.put("address",
                etAddress.getText().toString().trim());
        updates.put("patientName",
                etPatientName.getText().toString().trim());
        updates.put("additionalNotes",
                etNotes.getText().toString().trim());
        updates.put("requesterPhone",
                etPhone.getText().toString().trim());
        updates.put("updatedAt",
                System.currentTimeMillis());

        db.collection("blood_requests")
                .document(editRequestId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Request updated successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ── Loading state ─────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE);
        btnSubmitRequest.setEnabled(!loading);
        btnSubmitRequest.setText(loading
                ? "Please wait..."
                : (isEditMode ? "UPDATE REQUEST" : "SUBMIT REQUEST"));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}